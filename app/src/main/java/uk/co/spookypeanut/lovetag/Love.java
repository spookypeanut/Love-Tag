package uk.co.spookypeanut.lovetag;

/**
 * Copyright (c) 2014 Henry Bush
 * Distributed under the GNU GPL v3. For full terms see the file COPYING.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.session.MediaController;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
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
import java.util.List;

public class Love extends ActionBarActivity implements SwipeRefreshLayout.OnRefreshListener {
    LastfmSession mLfs;
    UrlMaker mUrlMaker;
    Context mCurrentContext = this;
    Track mNowPlaying;
    List<Track> mRecentTracks;
    ListEntry mPodView;
    MediaController mMediaController;
    private SwipeRefreshLayout mSwipeRefreshLayout;

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

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeLayout);
        mSwipeRefreshLayout.setOnRefreshListener(this);
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

    @Override
    public void onRefresh() {
        updateRecent();
    }

    public void onResume(Bundle icicle) {
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

    private void setRecentTracks(List<Track> tracks) {
        String tag = "Love&Tag.Love.setRecentTracks";
        mRecentTracks = tracks;
        LinearLayout rtLayout;
        rtLayout = (LinearLayout)findViewById(R.id.recentTracksLayout);
        rtLayout.removeAllViews();
        List<Track> present_list = new ArrayList<>();
        if (mNowPlaying != null) {
            present_list.add(mNowPlaying);
        }
        for (Track track : tracks) {
            if (!track.isIn(present_list)) {
                ListEntry list_entry = new ListEntry(mCurrentContext);
                rtLayout.addView(list_entry);
                list_entry.setMusic(track);
                present_list.add(track);
            }
        }
        mSwipeRefreshLayout.setRefreshing(false);
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
                TagCall tc = new TagCall();
                tc.execute(artist, title, TextUtils.join(",", tagList));
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
            String title = intent.getStringExtra("track");
            Track track;
            track = new Track(artist, title, false);
            mPodView.setMusic(track);

            LinearLayout podLayout;
            podLayout = (LinearLayout)findViewById(R.id.playingOnDeviceLayout);
            podLayout.removeAllViews();
            podLayout.addView(mPodView);
            TextView label = (TextView)findViewById(R.id.podLabel);
            label.setVisibility(View.VISIBLE);
            podLayout.setVisibility(View.VISIBLE);
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
        mSwipeRefreshLayout.setRefreshing(true);
        GetRecent gr = new GetRecent();
        gr.execute();
    }

    public class ListEntry extends LinearLayout {
        Track mTrack;
        TextView mArtistView;
        TextView mTitleView;
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
                    if (false == mTrack.mLoved) {
                        LoveCall lc = new LoveCall();
                        lc.execute(mTrack);
                        Log.d(tag, "Submitted love");
                        return;
                    }
                    UnloveCall ulc = new UnloveCall();
                    ulc.execute(mTrack);
                    Log.d(tag, "Submitted unlove");
                }
            });
            final ImageButton tagButton = (ImageButton) findViewById(R.id
                    .tagbutton);
            tagButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String tag = "Love&Tag.Love.ListEntry.tagButton.onClick";
                    Intent i = new Intent();
                    i.setClass(App.getContext(), TagInput.class);
                    // TODO: Maybe I should make the class parcellable
                    i.putExtra("artist", mTrack.mArtist);
                    i.putExtra("title", mTrack.mTitle);
                    startActivityForResult(i, getResources().getInteger(
                            R.integer.rc_tag_input));
                    return;

                }
            });
        }

        public void setMusic(Track track) {
            String tag = "Love&Tag.Love.ListEntry.setMusic";
            mTrack = track;
            Log.v(tag, "Adding: " + mTrack.mArtist +
                    ", " + mTrack.mTitle +
                    ", " + String.valueOf(mTrack.mLoved));
            try {
                update();
            }
            catch (NullPointerException e) {
                Log.w(tag, "Caught NullPointerException");
            }
        }
        private void update() {
            mArtistView.setText(mTrack.mArtist);
            mTitleView.setText(mTrack.mTitle);
            if (mTrack.mLoved == true) {
                mLovedView.setImageDrawable(getDrawable(R.drawable.lovetrue));
            } else if (mTrack.mLoved == false) {
                mLovedView.setImageDrawable(getDrawable(R.drawable.lovefalse));
            }
        }
    }

    private class UnloveCall extends AsyncTask<Track, String, String> {
        @Override
        protected String doInBackground(Track... params) {
            Track track = params[0];
            boolean result = mLfs.unlove(track);
            String msg;
            if (result == true) {
                setResult(RESULT_OK);
                msg = getString(R.string.unlove_success);
            } else {
                setResult(RESULT_CANCELED);
                msg = getString(R.string.unlove_failed);
            }
            return msg;
        }
        protected void onPostExecute(String result) {
            String tag = "Love&Tag.Love.UnloveCall.onPostExecute";
            Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();
            Log.i(tag, result);
            updateRecent();
        }
    }
    private class LoveCall extends AsyncTask<Track, String, String> {
        @Override
        protected String doInBackground(Track... params) {
            Track track = params[0];
            String tag = "Love&Tag.Love.LoveCall.doInBackground";
            boolean result = mLfs.love(track);
            String msg;
            if (result == true) {
                setResult(RESULT_OK);
                msg = getString(R.string.love_success);
            } else {
                setResult(RESULT_CANCELED);
                msg = getString(R.string.love_failed);
            }
            return msg;
        }
        protected void onPostExecute(String result) {
            String tag = "Love&Tag.Love.LoveCall.onPostExecute";
            Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();
            Log.i(tag, result);
            updateRecent();
        }
    }
    private class TagCall extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            String artist = params[0];
            String title = params[1];
            String tag_cat = params[2];
            String tag = "Love&Tag.Love.TagCall.doInBackground";
            Track track = new Track(artist, title, false);
            boolean result = mLfs.tag(track, tag_cat);
            String msg;
            if (result == true) {
                setResult(RESULT_OK);
                msg = getString(R.string.tag_success);
            } else {
                setResult(RESULT_CANCELED);
                msg = getString(R.string.tag_failed);
            }
            return msg;
        }
        protected void onPostExecute(String result) {
            String tag = "Love&Tag.Love.TagCall.onPostExecute";
            Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();
            Log.i(tag, result);
        }

    }
    private class GetRecent extends AsyncTask<String, String, String> {
        List<Track> mTempTracks = new ArrayList<>();
        @Override
        protected String doInBackground(String... params) {
            String tag = "Love&Tag.Love.GetRecent.doInBackground";
            mTempTracks = mLfs.getRecent();
            return "";
        }
        protected void onPostExecute(String result) {
            setRecentTracks(mTempTracks);
        }
    }
}
