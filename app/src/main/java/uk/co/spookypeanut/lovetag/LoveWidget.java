package uk.co.spookypeanut.lovetag;

/**
 * Copyright (c) 2014 Henry Bush
 * Distributed under the GNU GPL v3. For full terms see the file COPYING.
 */

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

public class LoveWidget extends AppWidgetProvider {
    static final String love_widget_click_action = "love_widget_click";
    static final String love_widget_new_track_action = "love_widget_new_track";

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        final String tag = "LoveWidget.onReceive";
        String action = intent.getAction();
        if (action == null) {
            Log.d(tag, "null action");
            context.startService(new Intent(context, UpdateService.class));
            return;
        }
        Log.d(tag, "non-null action: " + intent.getAction());
        super.onReceive(context, intent);
        String test = love_widget_click_action;
        if (action.equals(test)) {
            Log.d(tag, "Widget button clicked");
            Intent i = new Intent(context, UpdateService.class);
            i.setAction(love_widget_click_action);
            context.startService(i);
            return;
        }
        if (action.equals(TrackListActivity.METACHANGED) ||
                action.equals(TrackListActivity.PLAYSTATECHANGED)) {
            String artist = intent.getStringExtra("artist");
            if (artist == null) return;
            String title = intent.getStringExtra("track");
            Log.d(tag, "Got new track: " + title + " (" + action + ")");
            Intent i = new Intent(context,  UpdateService.class);
            i.setAction(love_widget_new_track_action);
            i.putExtra("artist", artist);
            i.putExtra("title", title);
            context.startService(i);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final String tag = "LoveWidget.onUpdate";
        Log.d(tag, "Starting");
        // There may be multiple widgets active, so update all of them
        Intent i = new Intent(context, UpdateService.class);
        context.startService(i);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        final String tag = "LoveWidget.onEnabled";
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
            super("LoveWidget$UpdateService");
            final String tag = "LoveWidget.UpdateService";
            Log.d(tag, "Constructor");
        }

        @Override
        public void onCreate() {
            super.onCreate();
            final String tag = "LoveWidget.UpdateService.onCreate";
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
        }

        public void setTrack(Track track) {
            SharedPreferences.Editor editor = mSettings.edit();
            // TODO: Can we use the same one for both widgets?
            editor.putString("lw_artist", track.mArtist);
            editor.putString("lw_title", track.mTitle);
            editor.putBoolean("lw_loved", track.mLoved).apply();
        }

        public Track getTrack() {
            Track nowPlaying = null;
            // TODO: Can we use the same one for both widgets?
            String artist = mSettings.getString("lw_artist", "");
            String title = mSettings.getString("lw_title", "");
            boolean loved = mSettings.getBoolean("lw_loved", false);
            if (!artist.equals("")) {
                nowPlaying = new Track(artist, title, loved);
            }
            return nowPlaying;
        }

        @Override
        public void onHandleIntent(Intent intent) {
            final String tag = "LoveWidget.UpdateService.onHandleIntent";
            String action = intent.getAction();
            Log.d(tag, "Handling intent: " + action);
            ComponentName me = new ComponentName(this, LoveWidget.class);
            AppWidgetManager mgr = AppWidgetManager.getInstance(this);
            if (action != null && action.equals(love_widget_click_action)) {
                Track track = getTrack();
                if (track == null) return;
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
            if (action != null && action.equals(love_widget_new_track_action)) {
                String artist = intent.getStringExtra("artist");
                String title = intent.getStringExtra("title");
                mgr.updateAppWidget(me, buildUpdate(this, artist, title));
                return;
            }
            mgr.updateAppWidget(me, buildUpdate(this));
        }

        private RemoteViews buildUpdate(Context context, String artist,
                                        String title) {
            final String tag = "LoveWidget.UpdateService.buildUpdate (CSS)";
            Log.d(tag, "Found track: " + artist + ", " + title);
            Track track = new Track(artist, title);
            setTrack(track);
            RemoteViews views = buildUpdate(context);
            // Construct the RemoteViews object
            Log.d(tag, "Setting text view");
            views.setTextViewText(R.id.loveWidgetLabel, title);
            if (mLfs.isLoved(track)) {
                track.mLoved = true;
                setTrack(track);
                views.setImageViewResource(R.id.loveWidgetButton,
                        R.drawable.lovetrue);
            } else {
                views.setImageViewResource(R.id.loveWidgetButton,
                        R.drawable.lovefalse);
            }

            return views;
        }

        private RemoteViews buildUpdate(Context context) {
            final String tag = "LoveWidget.UpdateService.buildUpdate";
            Log.d(tag, "Starting");
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.love_widget);

            Intent i = new Intent(this, LoveWidget.class);
            i.setAction(love_widget_click_action);
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.loveWidgetButton, pi);

            return views;
        }
    }
}
