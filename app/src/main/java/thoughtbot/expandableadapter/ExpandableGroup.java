package thoughtbot.expandableadapter;

import java.util.Collections;
import java.util.List;

import awais.instagrabber.repositories.responses.User;

public class ExpandableGroup {
    private final String title;
    private final List<User> items;

    public ExpandableGroup(final String title, final List<User> items) {
        this.title = title;
        // Use Collections.emptyList() to avoid NullPointerException if items is null
        this.items = items != null ? items : Collections.emptyList();
    }

    public String getTitle() {
        return title;
    }

    public List<User> getItems() {
        return items;
    }

    public int getItemCount() {
        return items.size(); // No need for null check as items is never null
    }
}
