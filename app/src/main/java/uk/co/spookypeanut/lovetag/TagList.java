package uk.co.spookypeanut.lovetag;

import java.util.ArrayList;
import java.util.List;

public class TagList extends ArrayList<Tag> {
    public TagList(TagList to_clone) {
        for (Tag tag : to_clone) {
            this.add(tag);
        }
    }

    public TagList() {}

    public TagList getActiveList() {
        TagList tl = new TagList();
        for (Tag tag : this) {
            if (tag.mActive) {
                tl.add(tag);
            }
        }
        return tl;
    }
    public TagList getInactiveList() {
        TagList tl = new TagList();
        for (Tag tag : this) {
            if (!tag.mActive) {
                tl.add(tag);
            }
        }
        return tl;
    }
    public void removeNames(List<String> names) {
        TagList copy = new TagList(this);
        for (Tag tag : copy) {
            if (names.contains(tag.mName)) {
                this.remove(tag);
            }
        }
    }
}
