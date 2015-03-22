package uk.co.spookypeanut.loveandtag;

/**
 * Copyright (c) 2014 Henry Bush
 * Distributed under the GNU GPL v3. For full terms see the file COPYING.
 */

import java.util.List;
import java.util.Map;

public class Track {
    String mArtist;
    String mTitle;
    boolean mLoved = false;
    boolean mCorrected = false;

    public Track(String artist, String title) {
        mArtist = artist;
        mTitle = title;
    }
    public Track(String artist, String title, boolean loved) {
        this(artist, title);
        mLoved = loved;
    }
    public Track(Map<String, String> params) {
        mArtist = params.get("artist");
        mTitle = params.get("title");
        mLoved = "1".equals(params.get("loved"));
    }
    boolean equals(Track other) {
        return mArtist.equals(other.mArtist) && mTitle.equals(other.mTitle);
    }

    boolean isIn(List<Track> other_tracks) {
        for (Track other : other_tracks) {
            if (equals(other)) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        return ("\"" + mTitle + "\", by " + mArtist + " (" + mLoved + ")");
    }
}
