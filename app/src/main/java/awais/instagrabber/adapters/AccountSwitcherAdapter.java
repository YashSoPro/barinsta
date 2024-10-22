package awais.instagrabber.adapters;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.R;
import awais.instagrabber.databinding.PrefAccountSwitcherBinding;
import awais.instagrabber.db.entities.Account;
import awais.instagrabber.utils.Constants;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class AccountSwitcherAdapter extends ListAdapter<Account, AccountSwitcherAdapter.ViewHolder> {
    private static final String TAG = "AccountSwitcherAdapter";

    private static final DiffUtil.ItemCallback<Account> DIFF_CALLBACK = new DiffUtil.ItemCallback<Account>() {
        @Override
        public boolean areItemsTheSame(@NonNull final Account oldItem, @NonNull final Account newItem) {
            // Compare unique IDs to determine if items are the same
            return oldItem.getUid().equals(newItem.getUid());
        }

        @Override
        public boolean areContentsTheSame(@NonNull final Account oldItem, @NonNull final Account newItem) {
            // Compare contents to determine if they are the same
            return oldItem.equals(newItem);
        }
    };

    private final OnAccountClickListener clickListener;
    private final OnAccountLongClickListener longClickListener;

    public AccountSwitcherAdapter(final OnAccountClickListener clickListener,
                                  final OnAccountLongClickListener longClickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        // Inflate the layout for each item
        final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        final PrefAccountSwitcherBinding binding = PrefAccountSwitcherBinding.inflate(layoutInflater, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final Account model = getItem(position);
        if (model == null) return; // Safeguard against null model
        final String cookie = settingsHelper.getString(Constants.COOKIE);
        final boolean isCurrent = model.getCookie().equals(cookie);
        holder.bind(model, isCurrent, clickListener, longClickListener);
    }

    public interface OnAccountClickListener {
        void onAccountClick(final Account model, final boolean isCurrent);
    }

    public interface OnAccountLongClickListener {
        boolean onAccountLongClick(final Account model, final boolean isCurrent);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final PrefAccountSwitcherBinding binding;

        public ViewHolder(final PrefAccountSwitcherBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            // Setting default image resource for arrowDown
            binding.arrowDown.setImageResource(R.drawable.ic_check_24);
        }

        @SuppressLint("SetTextI18n")
        public void bind(final Account model,
                         final boolean isCurrent,
                         final OnAccountClickListener clickListener,
                         final OnAccountLongClickListener longClickListener) {
            // Set click listeners for the item
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onAccountClick(model, isCurrent);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    return longClickListener.onAccountLongClick(model, isCurrent);
                }
                return false;
            });

            // Bind account information
            binding.profilePic.setImageURI(model.getProfilePic());
            binding.username.setText("@" + model.getUsername());
            binding.fullName.setTypeface(null);
            
            final String fullName = model.getFullName();
            if (TextUtils.isEmpty(fullName)) {
                binding.fullName.setVisibility(View.GONE); // Hide if empty
            } else {
                binding.fullName.setVisibility(View.VISIBLE);
                binding.fullName.setText(fullName);
            }

            // Highlight current account
            if (isCurrent) {
                binding.fullName.setTypeface(binding.fullName.getTypeface(), Typeface.BOLD);
                binding.arrowDown.setVisibility(View.VISIBLE);
            } else {
                binding.arrowDown.setVisibility(View.GONE);
            }
        }
    }
}
