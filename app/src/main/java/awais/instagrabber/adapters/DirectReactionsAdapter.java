package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import awais.instagrabber.adapters.viewholder.directmessages.DirectReactionViewHolder;
import awais.instagrabber.databinding.LayoutDmUserItemBinding;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItemEmojiReaction;

public final class DirectReactionsAdapter extends ListAdapter<DirectItemEmojiReaction, DirectReactionViewHolder> {

    private static final DiffUtil.ItemCallback<DirectItemEmojiReaction> DIFF_CALLBACK = new DiffUtil.ItemCallback<DirectItemEmojiReaction>() {
        @Override
        public boolean areItemsTheSame(@NonNull final DirectItemEmojiReaction oldItem, @NonNull final DirectItemEmojiReaction newItem) {
            return oldItem.getSenderId() == newItem.getSenderId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull final DirectItemEmojiReaction oldItem, @NonNull final DirectItemEmojiReaction newItem) {
            return oldItem.getEmoji().equals(newItem.getEmoji());
        }
    };

    private final long viewerId;
    private final Map<Long, User> userMap; // Use a Map for quicker access
    private final String itemId;
    private final OnReactionClickListener onReactionClickListener;

    public DirectReactionsAdapter(final long viewerId,
                                  final List<User> users,
                                  final String itemId,
                                  final OnReactionClickListener onReactionClickListener) {
        super(DIFF_CALLBACK);
        this.viewerId = viewerId;
        this.itemId = itemId;
        this.onReactionClickListener = onReactionClickListener;
        this.userMap = createUserMap(users); // Initialize userMap
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public DirectReactionViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        final LayoutDmUserItemBinding binding = LayoutDmUserItemBinding.inflate(layoutInflater, parent, false);
        return new DirectReactionViewHolder(binding, viewerId, itemId, onReactionClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull final DirectReactionViewHolder holder, final int position) {
        final DirectItemEmojiReaction reaction = getItem(position);
        holder.bind(reaction, getUser(reaction.getSenderId())); // Handle binding
    }

    @Override
    public long getItemId(final int position) {
        return getItem(position).getSenderId();
    }

    @Nullable
    private User getUser(final long pk) {
        return userMap.get(pk); // Lookup user from Map
    }

    /**
     * Creates a mapping of user IDs to User objects for efficient access.
     *
     * @param users List of User objects to be mapped
     * @return Map of user IDs to User objects
     */
    private Map<Long, User> createUserMap(final List<User> users) {
        Map<Long, User> userMap = new HashMap<>();
        for (User user : users) {
            userMap.put(user.getPk(), user);
        }
        return userMap;
    }

    public interface OnReactionClickListener {
        void onReactionClick(String itemId, DirectItemEmojiReaction reaction);
    }
}
