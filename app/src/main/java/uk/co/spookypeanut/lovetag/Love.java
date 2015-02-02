package uk.co.spookypeanut.lovetag;

/**
 * Copyright (c) 2014 Henry Bush
 * Distributed under the GNU GPL v3. For full terms see the file COPYING.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Love extends ActionBarActivity {
    LastfmSession mLfs;
    UrlMaker mUrlMaker;
    Context mCurrentContext = this;
    String mNowPlayingTitle;
    String mNowPlayingArtist;
    List<LastfmSession.RecentTrack> mRecentTracks;
    ListEntry mPodView;
    MediaController mMediaController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String tag = "Love&Tag.Love.onCreate";

        setContentView(R.layout.activity_love);

        IntentFilter iF = new IntentFilter();
        iF.addAction("com.android.music.metachanged");
        iF.addAction("com.android.music.playstatechanged");
        iF.addAction("com.android.music.playbackcomplete");
        iF.addAction("com.android.music.queuechanged");
        registerReceiver(mReceiver, iF);

        mPodView = new ListEntry(mCurrentContext);

        mUrlMaker = new UrlMaker();
        // This snippet should be used whenever getting a session. It's
        // the most elegant way I can figure out to do this (the only
        // inelegance is duplication of this snippet)
        mLfs = new LastfmSession();
        if (!mLfs.isLoggedIn()) {
            Intent i = new Intent();
            i.setClass(this, LoginActivity.class);
            startActivityForResult(i, getResources().getInteger(
                    R.integer.rc_log_in));
            return;
        }
        updateRecent();
    }

    private void setMediaController() {
        // To possibly be re-introduced later
        /*
        String tag = "Love&Tag.Love.setMediaController";
        MediaSessionManager msm = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
        List<MediaController> mc_list = msm.getActiveSessions(null);
        Log.d(tag, "Found " + mc_list.size());
        for (MediaController mc : mc_list) {
            MediaMetadata md = mc.getMetadata();
            Log.d(tag, md.getString(md.METADATA_KEY_ARTIST));
        }*/
    }

    private void setRecentTracks(List<LastfmSession.RecentTrack> tracks) {
        mRecentTracks = tracks;
        LinearLayout rtLayout;
        rtLayout = (LinearLayout)findViewById(R.id.recentTracksLayout);
        rtLayout.removeAllViews();
        for (LastfmSession.RecentTrack track : tracks) {
            ListEntry list_entry = new ListEntry(mCurrentContext);
            rtLayout.addView(list_entry);
            list_entry.setMusic(track);
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String tag = "Love&Tag.Love.onActivityResult";
        Log.i(tag, "Starting");
        Log.i(tag, "requestCode: " + requestCode + ", resultCode: " + resultCode);
        // Check which request we're responding to
        if (requestCode == getResources().getInteger(R.integer.rc_log_in)) {
            if (resultCode == RESULT_OK) {
                mLfs = new LastfmSession();
            } else {
                Log.e(tag, "Log in failed");
            }
            return;
        }
        if (requestCode == getResources().getInteger(R.integer.rc_tag_input)) {
            if (resultCode == RESULT_OK) {
                ArrayList<String> tagList;
                tagList = data.getStringArrayListExtra("tagList");
                String artist = data.getStringExtra("artist");
                String title = data.getStringExtra("title");
                Log.d(tag, "Tagging using: " + tagList.toString());
                TagCall tc = new TagCall();
                String tag_cat = TextUtils.join(",", tagList);
                tc.execute(artist, title, tag_cat);
                Log.d(tag, "Submitted tag");
            } else {
                Log.e(tag, "Tagging aborted");
            }
            return;
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String tag = "Love&Tag.Love.mReceiver.onReceive";
            String action = intent.getAction();
            String artist = intent.getStringExtra("artist");
            String track = intent.getStringExtra("track");
            mNowPlayingArtist = artist;
            mNowPlayingTitle = track;
            mPodView.setMusic(artist, track);

            LinearLayout podLayout;
            podLayout = (LinearLayout)findViewById(R.id.playingOnDeviceLayout);
            podLayout.removeAllViews();
            podLayout.addView(mPodView);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_love, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateRecent() {
        GetRecent gr = new GetRecent();
        gr.execute();
    }

    public class ListEntry extends LinearLayout {
        String mArtist;
        TextView mArtistView;
        String mTitle;
        TextView mTitleView;
        boolean mLoved;
        ImageButton mLovedView;

        public ListEntry(Context context) {
            super(context);
            View.inflate(context, R.layout.view_listentry, this);
            mArtistView = (TextView) findViewById(R.id.artist);
            mTitleView = (TextView) findViewById(R.id.title);
            mLovedView = (ImageButton) findViewById(R.id.lovebutton);
            final ImageButton loveButton = (ImageButton) findViewById(R.id
                    .lovebutton);
            loveButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String tag;
                    tag = "Love&Tag.Love.ListEntry.loveButton.onClick";
                    if (false == mLoved) {
                        LoveCall lc = new LoveCall();
                        lc.execute(mArtist, mTitle);
                        Log.d(tag, "Submitted love");
                        return;
                    }
                    UnloveCall ulc = new UnloveCall();
                    ulc.execute(mArtist, mTitle);
                    Log.d(tag, "Submitted unlove");
                }
            });
            final ImageButton tagButton = (ImageButton) findViewById(R.id
                    .tagbutton);
            tagButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent i = new Intent();
                    i.setClass(App.getContext(), TagInput.class);
                    i.putExtra("artist", mArtist);
                    i.putExtra("title", mTitle);
                    startActivityForResult(i, getResources().getInteger(
                            R.integer.rc_tag_input));
                    return;

                }
            });
        }

        public void setMusic(LastfmSession.RecentTrack track) {
            setMusic(track.mArtist, track.mTitle, track.mLoved);
        }

        public void setMusic(String artist, String title) {
            setMusic(artist, title, false);
        }

        public void setMusic(String artist, String title, boolean loved) {
            String tag = "Love&Tag.Love.ListEntry.setMusic";
            Log.d(tag, "Setting: " + artist + ", " + title + ", " +
                    String.valueOf(loved));
            mArtist = artist;
            mTitle = title;
            mLoved = loved;
            try {
                update();
            }
            catch (NullPointerException e) {
                Log.w(tag, "Caught NullPointerException");
            }
        }
        private void update() {
            mArtistView.setText(mArtist);
            mTitleView.setText(mTitle);
            if (mLoved == true) {
                mLovedView.setImageDrawable(getDrawable(R.drawable.lovetrue));
            } else if (mLoved == false) {
                mLovedView.setImageDrawable(getDrawable(R.drawable.lovefalse));
            }
        }
    }

    private class UnloveCall extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            String track = params[0];
            String artist = params[1];
            String tag = "Love&Tag.Love.UnloveCall.doInBackground";
            boolean result = mLfs.unlove(track, artist);
            if (result == true) {
                Log.i(tag, "Unlove succeeded");
                setResult(RESULT_OK);
            } else {
                Log.i(tag, "Unlove failed");
                setResult(RESULT_CANCELED);
            }
            return "";
        }
        protected void onPostExecute(String result) {
            updateRecent();
        }
    }
    private class LoveCall extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            String track = params[0];
            String artist = params[1];
            String tag = "Love&Tag.Love.LoveCall.doInBackground";
            boolean result = mLfs.love(track, artist);
            if (result == true) {
                Log.i(tag, "Love succeeded");
                setResult(RESULT_OK);
            } else {
                Log.i(tag, "Love failed");
                setResult(RESULT_CANCELED);
            }
            return "";
        }
        protected void onPostExecute(String result) {
            updateRecent();
        }
    }
    private class TagCall extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            String track = params[0];
            String artist = params[1];
            String tag_list = params[2];
            String tag = "Love&Tag.Love.TagCall.doInBackground";
            boolean result = mLfs.tag(track, artist, tag_list);
            if (result == true) {
                Log.i(tag, "Tag succeeded");
                setResult(RESULT_OK);
            } else {
                Log.i(tag, "Tag failed");
                setResult(RESULT_CANCELED);
            }
            return "";
        }
    }
    private class GetRecent extends AsyncTask<String, String, String> {
        List<LastfmSession.RecentTrack> mTempTracks = new ArrayList<>();
        @Override
        protected String doInBackground(String... params) {
            String tag = "Love&Tag.Love.TagCall.doInBackground";
            mTempTracks = mLfs.getRecent();
            return "";
        }
        protected void onPostExecute(String result) {
            setRecentTracks(mTempTracks);
        }
    }
}
