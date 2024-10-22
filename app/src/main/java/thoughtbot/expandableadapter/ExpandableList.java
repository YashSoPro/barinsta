package thoughtbot.expandableadapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public final class ExpandableList {
    private final int groupsSize;
    public final ArrayList<ExpandableGroup> groups;
    public final boolean[] expandedGroupIndexes;

    public ExpandableList(@NonNull final ArrayList<ExpandableGroup> groups) {
        this.groups = groups;
        this.groupsSize = groups.size();
        this.expandedGroupIndexes = new boolean[groupsSize];
    }

    public ExpandableList(@NonNull final ArrayList<ExpandableGroup> groups,
                          @Nullable final boolean[] expandedGroupIndexes) {
        this.groups = groups;
        this.groupsSize = groups.size();
        // Use provided expandedGroupIndexes or initialize with default values
        this.expandedGroupIndexes = expandedGroupIndexes != null ? expandedGroupIndexes : new boolean[groupsSize];
    }

    public int getVisibleItemCount() {
        int count = 0;
        for (int i = 0; i < groupsSize; i++) {
            count += numberOfVisibleItemsInGroup(i); // Using += for better readability
        }
        return coun
