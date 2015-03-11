package uk.co.spookypeanut.lovetag;

/**
 * Copyright (c) 2014 Henry Bush
 * Distributed under the GNU GPL v3. For full terms see the file COPYING.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
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

public class TrackListActivity extends ActionBarActivity implements SwipeRefreshLayout.OnRefreshListener {
    LastfmSession mLfs;
    UrlMaker mUrlMaker;
    Context mCurrentContext = this;
    Track mNowPlaying;
    // This is never visible. It's the autocorrected version of the currently
    // playing track, so we don't end up having both "burnout" and "Burnout"
    // in the list
    Track mAlternatePodTrack;
    List<Track> mRecentTracks;
    ListEntry mPodView;
//    MediaController mMediaController;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    public final static String METACHANGED = "com.android.music.metachanged";
    public final static String PLAYSTATECHANGED = "com.android.music" +
                                                  ".playstatechanged";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_list);

        IntentFilter iF = new IntentFilter();
        iF.addAction(METACHANGED);
        iF.addAction(PLAYSTATECHANGED);
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
        updateAll();
    }

    @Override
    public void onRefresh() {
        updateAll();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAll();
    }

    /*
    private void setMediaController() {
        // To possibly be re-introduced later
        String tag = "Love&Tag.Love.setMediaController";
        MediaSessionManager msm = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
        List<MediaController> mc_list = msm.getActiveSessions(null);
        Log.d(tag, "Found " + mc_list.size());
        for (MediaController mc : mc_list) {
            MediaMetadata md = mc.getMetadata();
            Log.d(tag, md.getString(md.METADATA_KEY_ARTIST));
        }
    }
    */

    private void setRecentTracks(List<Track> tracks) {
        mRecentTracks = tracks;
        LinearLayout rtLayout;
        rtLayout = (LinearLayout)findViewById(R.id.recentTracksLayout);
        rtLayout.removeAllViews();
        List<Track> present_list = new ArrayList<>();
        if (mNowPlaying != null) {
            present_list.add(mNowPlaying);
        }
        if (mAlternatePodTrack != null) {
            present_list.add(mAlternatePodTrack);
        }
        for (Track track : tracks) {
            if (!track.isIn(present_list)) {
                ListEntry list_entry = new ListEntry(mCurrentContext);
                rtLayout.addView(list_entry);
                list_entry.findViewById(R.id.title).setSelected(true);
                list_entry.setMusic(track);
                present_list.add(track);
            }
        }
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void setPodTrack(Track track) {
        mPodView.setMusic(track);

        LinearLayout podLayout;
        podLayout = (LinearLayout)findViewById(R.id.playingOnDeviceLayout);
        podLayout.removeAllViews();
        podLayout.addView(mPodView);
        TextView label = (TextView)findViewById(R.id.podLabel);
        label.setVisibility(View.VISIBLE);
        podLayout.setVisibility(View.VISIBLE);
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
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String tag = "Love&Tag.Love.mReceiver.onReceive";
            String action = intent.getAction();
            String artist = intent.getStringExtra("artist");
            if (artist == null) return;
            String title = intent.getStringExtra("track");
            Log.d(tag, "Got new track: " + title + " (" + action + ")");
            mNowPlaying = new Track(artist, title, false);
            updatePod();
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_track_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_logout) {
            mLfs.logOut();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateAll() {
        if (!mLfs.isLoggedIn()) return;
        mSwipeRefreshLayout.setRefreshing(true);
        updateRecent();
        updatePod();
    }

    private void updatePod() {
        String tag = "Love&Tag.Love.updatePod";
        if (mNowPlaying == null || ! mLfs.isLoggedIn()) return;
        TrackInfoCall ilc = new TrackInfoCall();
        Log.d(tag, mNowPlaying.toString());
        ilc.execute(mNowPlaying);
        Log.d(tag, "Checking if " + mNowPlaying.mTitle + " is loved");
    }

    private void updateRecent() {
        if (!mLfs.isLoggedIn()) return;
        GetRecent gr = new GetRecent();
        gr.execute();
    }

    public class ListEntry extends LinearLayout {
        Track mTrack;
        TextView mArtistView;
        TextView mTitleView;
        Context mContext;

        public ListEntry(Context context) {
            super(context);
            mContext = context;
            View.inflate(context, R.layout.view_listentry, this);
            mArtistView = (TextView) findViewById(R.id.artist);
            mTitleView = (TextView) findViewById(R.id.title);
            final ImageButton loveButton = (ImageButton) findViewById(R.id
                    .lovebutton);
            loveButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String tag;
                    tag = "Love&Tag.Love.ListEntry.loveButton.onClick";
                    mSwipeRefreshLayout.setRefreshing(true);
                    if (!mTrack.mLoved) {
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
                    .entry_tagbutton);
            tagButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String tag = "Love&Tag.Love.ListEntry.tagButton.onClick";
                    Intent i = new Intent();
                    i.setClass(App.getContext(), TagInputActivity.class);
                    i.putExtra("artist", mTrack.mArtist);
                    i.putExtra("title", mTrack.mTitle);
                    Log.d(tag, "Starting activity");
                    startActivityForResult(i, getResources().getInteger(
                            R.integer.rc_tag_input));
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
            Drawable d;
            if (mTrack.mLoved) {
                d = ContextCompat.getDrawable(mContext, R.drawable.lovetrue);
            } else {
                d = ContextCompat.getDrawable(mContext, R.drawable.lovefalse);
            }
            final ImageButton loveButton = (ImageButton) findViewById(R.id
                    .lovebutton);
            loveButton.setImageDrawable(d);
        }
    }

    private class UnloveCall extends AsyncTask<Track, String, String> {
        @Override
        protected String doInBackground(Track... params) {
            Track track = params[0];
            boolean result = mLfs.unlove(track);
            String msg;
            if (result) {
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
            updateAll();
        }
    }
    private class LoveCall extends AsyncTask<Track, String, String> {
        @Override
        protected String doInBackground(Track... params) {
            Track track = params[0];
            boolean result = mLfs.love(track);
            String msg;
            if (result) {
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
            updateAll();
        }
    }
    private class GetRecent extends AsyncTask<String, String, String> {
        List<Track> mTempTracks = new ArrayList<>();
        @Override
        protected String doInBackground(String... params) {
            mTempTracks = mLfs.getRecent();
            return "";
        }
        protected void onPostExecute(String result) {
            setRecentTracks(mTempTracks);
        }
    }
    private class TrackInfoCall extends AsyncTask<Track, String, String> {
        Track mNewTrack;
        Track mOrigTrack;

        @Override
        protected String doInBackground(Track... params) {
            mNewTrack = mLfs.getTrackInfo(params[0]);
            mOrigTrack = params[0];
            if (mNewTrack != null) {
                mOrigTrack.mLoved = mNewTrack.mLoved;
            }
            return "";
        }

        protected void onPostExecute(String result) {
            setPodTrack(mOrigTrack);
            mAlternatePodTrack = mNewTrack;
        }
    }
}
