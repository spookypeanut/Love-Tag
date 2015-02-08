package uk.co.spookypeanut.lovetag;

import java.util.List;
import java.util.Map;

/**
 * Created by hbush on 07/02/15.
 */
public class RecentTrack {
    String mArtist;
    String mTitle;
    boolean mLoved;
    List<String> mTags;
    public RecentTrack(String artist, String title, boolean loved) {
        mArtist = artist;
        mTitle = title;
        mLoved = loved;
    }
    public RecentTrack(Map<String, String> params) {
        mArtist = params.get("artist");
        mTitle = params.get("title");
        if ("1".equals(params.get("loved"))) {
            mLoved = true;
        } else {
            mLoved = false;
        }
    }
}
