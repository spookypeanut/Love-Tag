package uk.co.spookypeanut.loveandtag;

/**
 * Copyright (c) 2014 Henry Bush
 * Distributed under the GNU GPL v3. For full terms see the file COPYING.
 */

import android.util.Log;

import java.util.ArrayList;

public class TagList extends ArrayList<Tag> {
    // This is just a way to pass another flag back with the list
    boolean mValid = true;

    public boolean add(Tag new_tag) {
        final String tag = "TagList.add";
        if (this.contains(new_tag)) {
            Log.d(tag, new_tag.mName + " is already there");
            return false;
        }
        super.add(new_tag);
        return true;
    }

    public void addAll(TagList new_tags) {
        final String tag = "TagList.addAll";
        Log.d(tag, new_tags.toString());
        for (Tag new_tag : new_tags) {
            add(new_tag);
        }
    }

    public ArrayList<String> getAsStrings() {
        ArrayList<String> out = new ArrayList<>();
        for (Tag tag : this) {
            out.add(tag.mName);
        }
        return out;
    }

    public TagList getActiveList() {
        TagList tl = new TagList();
        for (Tag tag : this) {
            if (tag.mActive) {
                tl.add(tag);
            }
        }
        return tl;
    }
}
