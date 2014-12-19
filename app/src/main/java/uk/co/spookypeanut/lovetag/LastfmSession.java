package uk.co.spookypeanut.lovetag;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by hbush on 19/12/14.
 */
public class LastfmSession {
    Context mContext;
    SharedPreferences mSettings;
    String mSessionKey;

    public LastfmSession(Context context) {
        mContext = context;
        mSettings = mContext.getSharedPreferences("LastfmSession", mContext.MODE_MULTI_PROCESS);
        if (mSettings.contains("session_key")) {
            mSessionKey = mSettings.getString("session_key", "");
        } else {
            // Start login activity
            // Retrieve authToken
            // Create url for auth.getMobileSession
            // Call it
            // Handle invalid login
        }
    }
}
