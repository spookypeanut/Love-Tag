package uk.co.spookypeanut.loveandtag;

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

public class TagWidget extends AppWidgetProvider {
    static final String tag_widget_new_track_action = "tag_widget_new_track";

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        final String tag = "TagWidget.onReceive";
        String action = intent.getAction();
        if (action == null) {
            Log.d(tag, "null action");
            context.startService(new Intent(context, UpdateService.class));
            return;
        }
        Log.d(tag, "non-null action: " + intent.getAction());
        super.onReceive(context, intent);

        if (action.equals(TrackListActivity.METACHANGED) ||
                action.equals(TrackListActivity.PLAYSTATECHANGED)) {
            String artist = intent.getStringExtra("artist");
            if (artist == null) return;
            String title = intent.getStringExtra("track");
            Log.d(tag, "Got new track: " + title + " (" + action + ")");
            Intent i = new Intent(context, UpdateService.class);
            i.setAction(tag_widget_new_track_action);
            i.putExtra("artist", artist);
            i.putExtra("title", title);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startService(i);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final String tag = "TagWidget.onUpdate";
        Log.d(tag, "Starting");
        Intent i = new Intent(context, UpdateService.class);
        context.startService(i);
    }

    public static class UpdateService extends IntentService {
        LastfmSession mLfs = new LastfmSession();
        private SharedPreferences mSettings;
        public UpdateService() {
            super("TagWidget$UpdateService");
            final String tag = "TagWidget.UpdateService";
            Log.d(tag, "Constructor");
        }

        private RemoteViews buildUpdate(Context context, String artist,
                                        String title) {
            final String tag = "Widget.buildUpdate CSS";
            Log.d(tag, "Found track: " + artist + ", " + title);
            Track track = new Track(artist, title);
            setTrack(track);
            String text;
            if (tryLogin()) {
                Log.d(tag, "Logged in");
                text = title;
            } else {
                Log.d(tag, "Not logged in");
                text = getString(R.string.widget_not_logged_in_message);
                setTrack(new Track(text, ""));
            }
            RemoteViews views = buildUpdate(context);
            // Construct the RemoteViews object
            Log.d(tag, "Setting text view");
            views.setTextViewText(R.id.tw_label, text);
            return views;
        }

        private RemoteViews buildUpdate(Context context) {
            final String tag = "Widget.buildUpdate";
            Log.d(tag, "Starting");
            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.tag_widget);
            Intent i = new Intent();
            i.setClass(context, TagInputActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Track track = getTrack();
            if (track == null || track.mArtist == null) return views;
            i.putExtra("artist", track.mArtist);
            i.putExtra("title", track.mTitle);
            PendingIntent pendingIntent = PendingIntent.getActivity(context,
                    0, i, PendingIntent.FLAG_CANCEL_CURRENT);
            views.setOnClickPendingIntent(R.id.tw_button, pendingIntent);
            if (tryLogin()) {
                Log.d(tag, "Logged in");
                String text = "";
                views.setTextViewText(R.id.tw_label, text);
            } else {
                Log.d(tag, "Not logged in");
                String text = getString(R.string.widget_not_logged_in_message);
                views.setTextViewText(R.id.tw_label, text);
            }
            return views;
        }

        public Track getTrack() {
            final String tag = "TagWidget.UpdateService.getTrack";
            Track nowPlaying = null;
            String artist = mSettings.getString("tw_artist", "");
            String title = mSettings.getString("tw_title", "");
            boolean loved = mSettings.getBoolean("tw_loved", false);
            if (!artist.equals("")) {
                nowPlaying = new Track(artist, title, loved);
                Log.d(tag, nowPlaying.mTitle + ", " + nowPlaying.mArtist);
            }
            if (nowPlaying == null) {
                Log.d(tag, "No track stored");
            }
            return nowPlaying;
        }

        public void setTrack(Track track) {
            final String tag = "Widget.setTrack";
            Log.d(tag, track.mTitle + ", " + track.mArtist);
            SharedPreferences.Editor editor = mSettings.edit();
            editor.putString("tw_artist", track.mArtist);
            editor.putString("tw_title", track.mTitle);
            editor.putBoolean("tw_loved", track.mLoved).apply();
        }

        @Override
        public void onCreate() {
            super.onCreate();
            mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        }

        @Override
        public void onHandleIntent(Intent intent) {
            final String tag = "Widget.onHandleIntent";
            String action = intent.getAction();
            Log.d(tag, "Handling intent: " + action);
            ComponentName me = new ComponentName(this, TagWidget.class);
            AppWidgetManager mgr = AppWidgetManager.getInstance(this);
            if (action != null && action.equals(tag_widget_new_track_action)) {
                String artist = intent.getStringExtra("artist");
                if (artist == null) return;
                String title = intent.getStringExtra("title");
                mgr.updateAppWidget(me, buildUpdate(this, artist, title));
                return;
            }
            mgr.updateAppWidget(me, buildUpdate(this));
        }

        private boolean tryLogin() {
            return mLfs.isLoggedIn();
        }
    }
}
