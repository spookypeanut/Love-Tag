package uk.co.spookypeanut.lovetag;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

public class TagWidget extends AppWidgetProvider {
    static final String tag_widget_click_action = "tag_widget_click";
    static final String tag_widget_new_track_action = "tag_widget_new_track";
    Track mNowPlaying;

    @Override
    public void onReceive(Context context, Intent intent) {
        String tag = "Love&Tag.TagWidget.onReceive";
        String action = intent.getAction();
        if (action == null) {
            Log.d(tag, "null action");
            context.startService(new Intent(context, UpdateService.class));
            return;
        }
        Log.d(tag, "non-null action: " + intent.getAction());
        super.onReceive(context, intent);
        String test = tag_widget_click_action;
        if (action.equals(test)) {
            Log.d(tag, "Widget button clicked");
            Intent i = new Intent(context, UpdateService.class);
            i.setAction(tag_widget_click_action);
            context.startService(i);
            return;
        }
        test = context.getString(R.string.metachanged);
        if (action.equals(test)) {
            String artist = intent.getStringExtra("artist");
            String title = intent.getStringExtra("track");
            mNowPlaying = new Track(artist, title, false);
            Log.d(tag, "Got new track: " + mNowPlaying.mTitle + " (" + action +
                    ")");
            Intent i = new Intent(context,  UpdateService.class);
            i.setAction(tag_widget_new_track_action);
            i.putExtra("artist", artist);
            i.putExtra("title", title);
            context.startService(i);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        String tag = "Love&Tag.TagWidget.onUpdate";
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
        String tag = "Love&Tag.TagWidget.onEnabled";
        LastfmSession lfs = new LastfmSession();
        if (!lfs.isLoggedIn()) {
            Log.d(tag, "Session not logged in");
            int msg_id = R.string.widget_not_logged_in_message;
            Toast.makeText(context, msg_id, Toast.LENGTH_SHORT).show();
        } else {
            Log.d(tag, "Session already logged in");
        }
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    public static class UpdateService extends IntentService {
        LastfmSession mLfs;
        private SharedPreferences mSettings;
        public UpdateService() {
            super("TagWidget$UpdateService");
            String tag = "Love&Tag.TagWidget.UpdateService";
            Log.d(tag, "Constructor");
        }

        @Override
        public void onCreate() {
            super.onCreate();
            String tag = "Love&Tag.TagWidget.UpdateService.onCreate";
            Log.d(tag, "Starting");
            mLfs = new LastfmSession();
            if (!mLfs.isLoggedIn()) {
                Log.d(tag, "Session not logged in");
                int msg_id = R.string.widget_not_logged_in_message;
                Toast.makeText(this, msg_id, Toast.LENGTH_SHORT).show();
            } else {
                Log.d(tag, "Session already logged in");
            }
            mSettings = PreferenceManager.getDefaultSharedPreferences(this);
            getTrack();
        }

        public void setTrack(Track track) {
            SharedPreferences.Editor editor = mSettings.edit();
            editor.putString("tw_artist", track.mArtist);
            editor.putString("tw_title", track.mTitle);
            editor.putBoolean("tw_loved", track.mLoved).apply();
        }

        public Track getTrack() {
            Track nowPlaying = null;
            String artist = mSettings.getString("tw_artist", "");
            String title = mSettings.getString("tw_title", "");
            boolean loved = mSettings.getBoolean("tw_loved", false);
            if (!artist.equals("")) {
                nowPlaying = new Track(artist, title, loved);
            }
            return nowPlaying;
        }

        @Override
        public void onHandleIntent(Intent intent) {
            String tag = "Love&Tag.TagWidget.UpdateService.onHandleIntent";
            String action = intent.getAction();
            Log.d(tag, "Handling intent: " + action);
            ComponentName me = new ComponentName(this, TagWidget.class);
            AppWidgetManager mgr = AppWidgetManager.getInstance(this);
            if (action != null && action.equals(tag_widget_click_action)) {
                Track track = getTrack();
                if (track.mLoved) {
                    Toast.makeText(this, "Unloving " + track.mTitle,
                            Toast.LENGTH_SHORT).show();
                    mLfs.unlove(track);
                } else {
                    Toast.makeText(this, "Loving " + track.mTitle,
                            Toast.LENGTH_SHORT).show();
                    mLfs.love(track);
                }
                mgr.updateAppWidget(me, buildUpdate(this, track.mArtist,
                        track.mTitle));
                return;
            }
            if (action != null && action.equals(tag_widget_new_track_action)) {
                String artist = intent.getStringExtra("artist");
                String title = intent.getStringExtra("title");
                mgr.updateAppWidget(me, buildUpdate(this, artist, title));
                return;
            }
            mgr.updateAppWidget(me, buildUpdate(this));
        }

        private RemoteViews buildUpdate(Context context, String artist,
                                        String title) {
            String tag = "Love&Tag.TagWidget.UpdateService.buildUpdate (CSS)";
            Log.d(tag, "Found track: " + artist + ", " + title);
            Track track = new Track(artist, title);
            setTrack(track);
            RemoteViews views = buildUpdate(context);
            // Construct the RemoteViews object
            Log.d(tag, "Setting text view");
            views.setTextViewText(R.id.tagWidgetLabel, title);
            // TODO: This is rather wasteful, we don't need to connect to
            // last.fm on every change of track
            if (mLfs.isLoved(track)) {
                track.mLoved = true;
                setTrack(track);
            }
            return views;
        }

        private RemoteViews buildUpdate(Context context) {
            String tag = "Love&Tag.TagWidget.UpdateService.buildUpdate";
            Log.d(tag, "Starting");
            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.tag_widget);

            Intent i = new Intent(this, TagWidget.class);
            i.setAction(tag_widget_click_action);
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.tagWidgetButton, pi);

            return views;
        }
    }
}
