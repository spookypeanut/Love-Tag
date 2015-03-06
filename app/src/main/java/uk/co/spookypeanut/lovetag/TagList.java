package uk.co.spookypeanut.lovetag;

import android.util.Log;

import java.util.ArrayList;

public class TagList extends ArrayList<Tag> {
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

    public void addAll(TagList new_tags) {
        String tag = "Love&Tag.TagList.addAll";
        Log.d(tag, new_tags.toString());
        for (Tag new_tag : new_tags) {
            add(new_tag);
        }
    }
    public boolean add(Tag new_tag) {
        String tag = "Love&Tag.TagList.add";
        if (this.contains(new_tag)) {
            Log.d(tag, new_tag.mName + " is already there");
            return false;
        }
        super.add(new_tag);
        return true;
    }
}
