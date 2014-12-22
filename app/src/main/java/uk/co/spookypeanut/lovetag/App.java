package uk.co.spookypeanut.lovetag;

import android.app.Application;
import android.content.Context;

/**
 * Created by hbush on 21/12/14.
 */
public class App extends Application {

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }

    public static Context getContext(){
        return mContext;
    }
}
