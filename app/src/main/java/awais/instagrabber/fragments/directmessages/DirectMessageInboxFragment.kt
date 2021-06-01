package awais.instagrabber.fragments.directmessages

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import awais.instagrabber.R
import awais.instagrabber.activities.MainActivity
import awais.instagrabber.adapters.DirectMessageInboxAdapter
import awais.instagrabber.customviews.helpers.RecyclerLazyLoaderAtEdge
import awais.instagrabber.databinding.FragmentDirectMessagesInboxBinding
import awais.instagrabber.models.Resource
import awais.instagrabber.repositories.responses.directmessages.DirectInbox
import awais.instagrabber.repositories.responses.directmessages.DirectThread
import awais.instagrabber.utils.extensions.TAG
import awais.instagrabber.viewmodels.DirectInboxViewModel
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.internal.ToolbarUtils
import com.google.android.material.snackbar.Snackbar

class DirectMessageInboxFragment : Fragment(), OnRefreshListener {
    private lateinit var fragmentActivity: MainActivity
    private lateinit var viewModel: DirectInboxViewModel
    private lateinit var root: CoordinatorLayout
    private lateinit var binding: FragmentDirectMessagesInboxBinding
    private lateinit var inboxAdapter: DirectMessageInboxAdapter
    private lateinit var lazyLoader: RecyclerLazyLoaderAtEdge

    private var shouldRefresh = true
    // private var receiver: DMRefreshBroadcastReceiver? = null
    private var scrollToTop = false
    private var navigating = false
    private var threadsObserver: Observer<List<DirectThread?>>? = null
    private var pendingRequestsMenuItem: MenuItem? = null
    private var pendingRequestTotalBadgeDrawable: BadgeDrawable? = null
    private var isPendingRequestTotalBadgeAttached = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentActivity = requireActivity() as MainActivity
        viewModel = ViewModelProvider(fragmentActivity).get(DirectInboxViewModel::class.java)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        if (this::root.isInitialized) {
            shouldRefresh = false
            return root
        }
        binding = FragmentDirectMessagesInboxBinding.inflate(inflater, container, false)
        root = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!shouldRefresh) return
        init()
    }

    override fun onRefresh() {
        lazyLoader.resetState()
        scrollToTop = true
        viewModel.refresh()
    }

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    override fun onPause() {
        super.onPause()
        // unregisterReceiver()
        isPendingRequestTotalBadgeAttached = false
        pendingRequestsMenuItem?.let {
            @SuppressLint("RestrictedApi") val menuItemView = ToolbarUtils.getActionMenuItemView(fragmentActivity.toolbar, it.itemId)
            if (menuItemView != null) {
                BadgeUtils.detachBadgeDrawable(pendingRequestTotalBadgeDrawable, fragmentActivity.toolbar, it.itemId)
                pendingRequestTotalBadgeDrawable = null
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupObservers()
        // val context = context ?: return
        // receiver = DMRefreshBroadcastReceiver { Log.d(TAG, "onResume: broadcast received") }
        // context.registerReceiver(receiver, IntentFilter(DMRefreshBroadcastReceiver.ACTION_REFRESH_DM))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.dm_inbox_menu, menu)
        pendingRequestsMenuItem = menu.findItem(R.id.pending_requests)
        pendingRequestsMenuItem?.isVisible = isPendingRequestTotalBadgeAttached
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.pending_requests) {
            val directions = DirectMessageInboxFragmentDirections.actionInboxToPendingInbox()
            try {
                NavHostFragment.findNavController(this).navigate(directions)
            } catch (e: Exception) {
                Log.e(TAG, "onOptionsItemSelected: ", e)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // private fun unregisterReceiver() {
    //     if (receiver == null) return
    //     val context = context ?: return
    //     context.unregisterReceiver(receiver)
    //     receiver = null
    // }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        init()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeViewModelObservers()
        viewModel.onDestroy()
    }

    private fun setupObservers() {
        removeViewModelObservers()
        threadsObserver = Observer { list: List<DirectThread?> ->
            inboxAdapter.submitList(list) {
                if (!scrollToTop) return@submitList
                binding.inboxList.post { binding.inboxList.smoothScrollToPosition(0) }
                scrollToTop = false
            }
        }
        threadsObserver?.let { viewModel.threads.observe(fragmentActivity, it) }
        viewModel.inbox.observe(viewLifecycleOwner, { inboxResource: Resource<DirectInbox?>? ->
            if (inboxResource == null) return@observe
            when (inboxResource.status) {
                Resource.Status.SUCCESS -> binding.swipeRefreshLayout.isRefreshing = false
                Resource.Status.ERROR -> {
                    if (inboxResource.message != null) {
                        Snackbar.make(binding.root, inboxResource.message, Snackbar.LENGTH_LONG).show()
                    }
                    if (inboxResource.resId != 0) {
                        Snackbar.make(binding.root, inboxResource.resId, Snackbar.LENGTH_LONG).show()
                    }
                    binding.swipeRefreshLayout.isRefreshing = false
                }
                Resource.Status.LOADING -> binding.swipeRefreshLayout.isRefreshing = true
            }
        })
        viewModel.pendingRequestsTotal.observe(viewLifecycleOwner, { count: Int? -> attachPendingRequestsBadge(count) })
    }

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    private fun attachPendingRequestsBadge(count: Int?) {
        val pendingRequestsMenuItem1 = pendingRequestsMenuItem
        if (pendingRequestsMenuItem1 == null) {
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({ attachPendingRequestsBadge(count) }, 500)
            return
        }
        if (pendingRequestTotalBadgeDrawable == null) {
            val context = context ?: return
            pendingRequestTotalBadgeDrawable = BadgeDrawable.create(context)
        }
        if (count == null || count == 0) {
            @SuppressLint("RestrictedApi") val menuItemView = ToolbarUtils.getActionMenuItemView(
                fragmentActivity.toolbar,
                pendingRequestsMenuItem1.itemId
            )
            if (menuItemView != null) {
                BadgeUtils.detachBadgeDrawable(pendingRequestTotalBadgeDrawable, fragmentActivity.toolbar, pendingRequestsMenuItem1.itemId)
            }
            isPendingRequestTotalBadgeAttached = false
            pendingRequestTotalBadgeDrawable?.number = 0
            pendingRequestsMenuItem1.isVisible = false
            return
        }
        pendingRequestsMenuItem1.isVisible = true
        if (pendingRequestTotalBadgeDrawable?.number == count) return
        pendingRequestTotalBadgeDrawable?.number = count
        if (!isPendingRequestTotalBadgeAttached) {
            pendingRequestTotalBadgeDrawable?.let {
                BadgeUtils.attachBadgeDrawable(it, fragmentActivity.toolbar, pendingRequestsMenuItem1.itemId)
                isPendingRequestTotalBadgeAttached = true
            }
        }
    }

    private fun removeViewModelObservers() {
        threadsObserver?.let { viewModel.threads.removeObserver(it) }
        // no need to explicitly remove observers whose lifecycle owner is getViewLifecycleOwner
    }

    private fun init() {
        val context = context ?: return
        setupObservers()
        binding.swipeRefreshLayout.setOnRefreshListener(this)
        binding.inboxList.setHasFixedSize(true)
        binding.inboxList.setItemViewCacheSize(20)
        val layoutManager = LinearLayoutManager(context)
        binding.inboxList.layoutManager = layoutManager
        inboxAdapter = DirectMessageInboxAdapter { thread ->
            val threadId = thread.threadId
            val threadTitle = thread.threadTitle
            if (navigating || threadId.isNullOrBlank() || threadTitle.isNullOrBlank()) return@DirectMessageInboxAdapter
            navigating = true
            if (isAdded) {
                val directions = DirectMessageInboxFragmentDirections.actionInboxToThread(threadId, threadTitle)
                try {
                    NavHostFragment.findNavController(this).navigate(directions)
                } catch (e: Exception) {
                    Log.e(TAG, "init: ", e)
                }
            }
            navigating = false
        }
        inboxAdapter.setHasStableIds(true)
        binding.inboxList.adapter = inboxAdapter
        lazyLoader = RecyclerLazyLoaderAtEdge(layoutManager) { viewModel.fetchInbox() }
        lazyLoader.let { binding.inboxList.addOnScrollListener(it) }
    }
}