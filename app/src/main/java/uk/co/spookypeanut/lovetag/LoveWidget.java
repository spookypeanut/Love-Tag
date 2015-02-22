package uk.co.spookypeanut.lovetag;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RemoteViews;
import android.widget.Toast;

public class LoveWidget extends AppWidgetProvider {
    LastfmSession mLfs;

    @Override
    public void onReceive(Context context, Intent intent) {
        String tag = "Love&Tag.LoveWidget.onReceive";
        String action = intent.getAction();
        if (action == null) {
            Log.d(tag, "null action");
            context.startService(new Intent(context, UpdateService.class));
            return;
        }
        Log.d(tag, "non-null action: " + intent.getAction());
        super.onReceive(context, intent);
        String test = context.getString(R.string .love_widget_click_action);
        if (action.equals(test)) {

        }
    }

    public void loveCurrent() {

    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        String tag = "Love&Tag.LoveWidget.onUpdate";
        Log.d(tag, "Starting");
        // There may be multiple widgets active, so update all of them
        Intent i = new Intent(context, UpdateService.class);
        context.startService(i);
        /*
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }*/
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        String tag = "Love&Tag.LoveWidget.onEnabled";
        mLfs = new LastfmSession();
        if (!mLfs.isLoggedIn()) {
            Log.d(tag, "Session not logged in");
            int msg_id = R.string.widget_not_logged_in_message;
            Toast.makeText(context, msg_id, Toast.LENGTH_SHORT).show();
        } else {
            Log.d(tag, "Session already logged in");
        }
        IntentFilter iF = new IntentFilter();
        // TODO: use a global list for these
        iF.addAction("com.android.music.metachanged");
        iF.addAction("com.android.music.playstatechanged");
        iF.addAction("com.android.music.playbackcomplete");
        iF.addAction("com.android.music.queuechanged");
        context.registerReceiver(this, iF);
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        context.unregisterReceiver(this);
    }

    public static class UpdateService extends IntentService {
        private SharedPreferences mPrefs;
        private Track mNowPlaying;
        public UpdateService() {
            super("LoveWidget$UpdateService");
            String tag = "Love&Tag.LoveWidget.UpdateService";
            Log.d(tag, "Constructor");
        }
        @Override
        public void onCreate() {
            super.onCreate();
            String tag = "Love&Tag.LoveWidget.UpdateService.onCreate";
            Log.d(tag, "Starting");
            mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        }
        @Override
        public void onHandleIntent(Intent intent) {
            String tag = "Love&Tag.LoveWidget.UpdateService.onHandleIntent";
            Log.d(tag, "Handling intent");
            ComponentName me = new ComponentName(this, LoveWidget.class);
            AppWidgetManager mgr = AppWidgetManager.getInstance(this);
            if (!intent.hasExtra("artist")) {
                mgr.updateAppWidget(me, buildUpdate(this));
                return;
            }
            String artist = intent.getStringExtra("artist");
            String title = intent.getStringExtra("title");
            mgr.updateAppWidget(me, buildUpdate(this, artist, title));
        }

        private RemoteViews buildUpdate(Context context, String artist,
                                        String title) {
            String tag = "Love&Tag.LoveWidget.UpdateService.buildUpdate (CSS)";
            Log.d(tag, "Found track: " + artist + ", " + title);
            RemoteViews views = buildUpdate(context);
            Track mNowPlaying = new Track(artist, title, false);
            // Construct the RemoteViews object
            views.setTextViewText(R.id.loveWidgetLabel, title);
            return views;
        }

        private RemoteViews buildUpdate(Context context) {
            String tag = "Love&Tag.LoveWidget.UpdateService.buildUpdate";
            Log.d(tag, "Starting");
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.love_widget);

            Intent i = new Intent(this, LoveWidget.class);
            i.setAction(getString(R.string.love_widget_click_action));
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.loveWidgetButton, pi);

            return views;
        }
    }
}


