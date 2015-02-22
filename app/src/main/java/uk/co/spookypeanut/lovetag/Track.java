package uk.co.spookypeanut.lovetag;

import java.util.List;
import java.util.Map;

/**
 * Created by hbush on 07/02/15.
 */
public class Track {
    String mArtist;
    String mTitle;
    boolean mLoved;
    List<String> mTags;
    public Track(String artist, String title) {
        mArtist = artist;
        mTitle = title;
        mLoved = false;
    }

    public Track(String artist, String title, boolean loved) {
        mArtist = artist;
        mTitle = title;
        mLoved = loved;
    }
    public Track(Map<String, String> params) {
        mArtist = params.get("artist");
        mTitle = params.get("title");
        if ("1".equals(params.get("loved"))) {
            mLoved = true;
        } else {
            mLoved = false;
        }
    }
    boolean equals(Track other) {
        if (mArtist != other.mArtist) {
            return false;
        }
        if (mTitle != other.mTitle) {
            return false;
        }
        return true;
    }
    boolean isIn(List<Track> other_tracks) {
        for (Track other : other_tracks) {
            if (equals(other)) {
                return true;
            }
        }
        return false;
    }
}
