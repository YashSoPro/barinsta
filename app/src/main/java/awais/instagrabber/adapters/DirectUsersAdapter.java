package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.viewholder.directmessages.DirectUserViewHolder;
import awais.instagrabber.databinding.ItemFavSectionHeaderBinding;
import awais.instagrabber.databinding.LayoutDmUserItemBinding;
import awais.instagrabber.repositories.responses.User;

public final class DirectUsersAdapter extends ListAdapter<DirectUsersAdapter.DirectUserOrHeader, RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_USER = 1;

    private static final DiffUtil.ItemCallback<DirectUserOrHeader> DIFF_CALLBACK = new DiffUtil.ItemCallback<DirectUserOrHeader>() {
        @Override
        public boolean areItemsTheSame(@NonNull final DirectUserOrHeader oldItem, @NonNull final DirectUserOrHeader newItem) {
            if (oldItem.isHeader() && newItem.isHeader()) {
                return oldItem.headerTitle == newItem.headerTitle;
            }
            if (!oldItem.isHeader() && !newItem.isHeader() && oldItem.user != null && newItem.user != null) {
                return oldItem.user.getPk() == newItem.user.getPk();
            }
            return false;
        }

        @Override
        public boolean areContentsTheSame(@NonNull final DirectUserOrHeader oldItem, @NonNull final DirectUserOrHeader newItem) {
            if (oldItem.isHeader() && newItem.isHeader()) {
                return oldItem.headerTitle == newItem.headerTitle;
            }
            if (!oldItem.isHeader() && !newItem.isHeader() && oldItem.user != null && newItem.user != null) {
                return oldItem.user.getUsername().equals(newItem.user.getUsername()) &&
                       oldItem.user.getFullName().equals(newItem.user.getFullName());
            }
            return false;
        }
    };

    private final long inviterId;
    private final OnDirectUserClickListener onClickListener;
    private final OnDirectUserLongClickListener onLongClickListener;
    private List<Long> adminUserIds = new ArrayList<>();

    public DirectUsersAdapter(final long inviterId,
                              final OnDirectUserClickListener onClickListener,
                              final OnDirectUserLongClickListener onLongClickListener) {
        super(DIFF_CALLBACK);
        this.inviterId = inviterId;
        this.onClickListener = onClickListener;
        this.onLongClickListener = onLongClickListener;
        setHasStableIds(true);
    }

    public void submitUsers(final List<User> users, final List<User> leftUsers) {
        if (users == null && leftUsers == null) return;
        final List<DirectUserOrHeader> userOrHeaders = combineLists(users, leftUsers);
        submitList(userOrHeaders);
    }

    private List<DirectUserOrHeader> combineLists(final List<User> users, final List<User> leftUsers) {
        List<DirectUserOrHeader> userOrHeaders = new ArrayList<>();
        if (users != null && !users.isEmpty()) {
            userOrHeaders.add(new DirectUserOrHeader(R.string.members));
            for (User user : users) {
                userOrHeaders.add(new DirectUserOrHeader(user));
            }
        }
        if (leftUsers != null && !leftUsers.isEmpty()) {
            userOrHeaders.add(new DirectUserOrHeader(R.string.dms_left_users));
            for (User user : leftUsers) {
                userOrHeaders.add(new DirectUserOrHeader(user));
            }
        }
        return userOrHeaders;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_USER) {
            final LayoutDmUserItemBinding binding = LayoutDmUserItemBinding.inflate(layoutInflater, parent, false);
            return new DirectUserViewHolder(binding, onClickListener, onLongClickListener);
        } else {
            final ItemFavSectionHeaderBinding headerBinding = ItemFavSectionHeaderBinding.inflate(layoutInflater, parent, false);
            return new HeaderViewHolder(headerBinding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof HeaderViewHolder) {
            bindHeader((HeaderViewHolder) holder, position);
        } else if (holder instanceof DirectUserViewHolder) {
            bindUser((DirectUserViewHolder) holder, position);
        }
    }

    private void bindHeader(HeaderViewHolder holder, int position) {
        holder.bind(getItem(position).headerTitle);
    }

    private void bindUser(DirectUserViewHolder holder, int position) {
        final User user = getItem(position).user;
        holder.bind(position,
                    user,
                    user != null && adminUserIds.contains(user.getPk()),
                    user != null && user.getPk() == inviterId,
                    false,
                    false);
    }

    @Override
    public int getItemViewType(final int position) {
        return getItem(position).isHeader() ? VIEW_TYPE_HEADER : VIEW_TYPE_USER;
    }

    @Override
    public long getItemId(final int position) {
        return getItem(position).isHeader() ? getItem(position).headerTitle : getItem(position).user.getPk();
    }

    public void setAdminUserIds(final List<Long> adminUserIds) {
        this.adminUserIds = adminUserIds != null ? adminUserIds : new ArrayList<>();
        notifyDataSetChanged();
    }

    public static class DirectUserOrHeader {
        int headerTitle;
        User user;

        public DirectUserOrHeader(final int headerTitle) {
            this.headerTitle = headerTitle;
        }

        public DirectUserOrHeader(final User user) {
            this.user = user;
        }

        boolean isHeader() {
            return headerTitle > 0;
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final ItemFavSectionHeaderBinding binding;

        public HeaderViewHolder(@NonNull final ItemFavSectionHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(@StringRes final int headerTitle) {
            binding.textViewHeader.setText(headerTitle); // Assume you have a TextView for the header
        }
    }

    public interface OnDirectUserClickListener {
        void onClick(int position, User user, boolean selected);
    }

    public interface OnDirectUserLongClickListener {
        boolean onLongClick(int position, User user);
    }
}
