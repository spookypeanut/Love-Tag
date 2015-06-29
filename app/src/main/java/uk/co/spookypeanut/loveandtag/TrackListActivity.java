package uk.co.spookypeanut.loveandtag;

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
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.List;

import uk.co.spookypeanut.loveandtag.util.IabHelper;
import uk.co.spookypeanut.loveandtag.util.IabResult;
import uk.co.spookypeanut.loveandtag.util.Purchase;

public class TrackListActivity extends ActionBarActivity implements
        SwipeRefreshLayout.OnRefreshListener {
    public final static String METACHANGED = "com.android.music.metachanged";
    public final static String PLAYSTATECHANGED = "com.android.music" +
                                                  ".playstatechanged";
    LastfmSession mLfs;
    UrlMaker mUrlMaker;
    Context mCurrentContext = this;
    Track mNowPlaying;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String tag = "TrackListActivity.mReceiver.onReceive";
            String action = intent.getAction();
            String artist = intent.getStringExtra("artist");
            if (artist == null) return;
            String title = intent.getStringExtra("track");
            Log.d(tag, "Got new track: " + title + " (" + action + ")");
            mNowPlaying = new Track(artist, title, false);
            updatePod();
        }
    };
    boolean mIabWorks = false;
    // This is never visible. It's the autocorrected version of the currently
    // playing track, so we don't end up having both "burnout" and "Burnout"
    // in the list
    Track mAlternatePodTrack;
    List<Track> mRecentTracks;
    TextView mErrorMessage;
    ListEntry mPodView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private void donateClicked() {
        final String tag = "TrackListActivity.donateClicked";
        Intent i = new Intent();
        i.setClass(App.getContext(), DonateActivity.class);
        Log.d(tag, "Starting donate activity");
        startActivityForResult(i, getResources().getInteger(
                R.integer.rc_donate));
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final String tag = "TrackListActivity.onActivityResult";
        Log.i(tag, "requestCode: " + requestCode + ", resultCode: " + resultCode);
        // Check which request we're responding to
        if (requestCode == getResources().getInteger(R.integer.rc_log_in)) {
            if (resultCode == RESULT_OK) {
                mLfs = new LastfmSession();
            } else {
                Log.e(tag, "Log in failed");
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        final String tag = "TrackListActivity.onResume";
        Log.d(tag, "Updating");
        updateAll();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String tag = "TrackListActivity.onCreate";
        setContentView(R.layout.activity_track_list);

        IntentFilter iF = new IntentFilter();
        iF.addAction(METACHANGED);
        iF.addAction(PLAYSTATECHANGED);
        registerReceiver(mReceiver, iF);

        mPodView = new ListEntry(mCurrentContext);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeLayout);
        Log.d(tag, "setOnRefreshListener");
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
        }

    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_track_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final String tag = "TrackListActivity.onOptionsItemSelected";
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
        if (id == R.id.action_donate) {
             donateClicked();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        final String tag = "TrackListActivity.onRefresh";
        Log.d(tag, "Updating");
        updateAll();
    }

    private void setNoTracks() {
        LinearLayout rtLayout;
        rtLayout = (LinearLayout)findViewById(R.id.recentTracksLayout);
        ProgressBar progress;
        progress = (ProgressBar)findViewById(R.id.tl_initialProgressBar);
        if (progress != null) progress.setVisibility(View.GONE);
        if (mErrorMessage != null) {
            return;
        }
        mErrorMessage = new TextView(mCurrentContext);
        mErrorMessage.setTextSize(18);
        mErrorMessage.setGravity(Gravity.CENTER_HORIZONTAL);
        rtLayout.addView(mErrorMessage);
        String msg = getString(R.string.tl_problems_communicating);
        mErrorMessage.setText(msg);
    }

    private void setPodTrack(Track track) {
        if (track == null) {
            return;
        }
        mPodView.setMusic(track);

        LinearLayout podLayout;
        podLayout = (LinearLayout)findViewById(R.id.playingOnDeviceLayout);
        podLayout.removeAllViews();
        podLayout.addView(mPodView);
        TextView label = (TextView)findViewById(R.id.podLabel);
        label.setVisibility(View.VISIBLE);
        podLayout.setVisibility(View.VISIBLE);
    }

    private void setRecentTracks(List<Track> tracks) {
        mRecentTracks = tracks;
        if (tracks.size() == 0) {
            setNoTracks();
            mSwipeRefreshLayout.setRefreshing(false);
            return;
        }
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
                list_entry.findViewById(R.id.artist).setSelected(true);
                list_entry.setMusic(track);
                present_list.add(track);
            }
        }
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void updateAll() {
        final String tag = "TrackListActivity.updateAll";
        Log.d(tag, "Updating");
        if (!mLfs.isLoggedIn()) return;
        updatePod();
        if (updateRecent()) {
            mSwipeRefreshLayout.setRefreshing(true);
        }
    }

    private boolean updatePod() {
        final String tag = "TrackListActivity.updatePod";
        if (mNowPlaying == null || ! mLfs.isLoggedIn()) return false;
        Log.d(tag, mNowPlaying.toString());
        TrackInfoCall ilc = new TrackInfoCall();
        ilc.execute(mNowPlaying);
        Log.d(tag, "Checking if " + mNowPlaying.mTitle + " is loved");
        return true;
    }

    private boolean updateRecent() {
        if (!mLfs.isLoggedIn()) return false;
        GetRecent gr = new GetRecent();
        gr.execute();
        return true;
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
                    final String tag = "TrackListActivity.ListEntry" +
                                       ".loveButton.onClick";
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
                    final String tag = "TrackListActivity.ListEntry" +
                                       ".tagButton.onClick";
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
            final String tag = "TrackListActivity.ListEntry.setMusic";
            mTrack = track;
            Log.d(tag, "Adding: " + mTrack.mArtist +
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
            final String tag = "TrackListActivity.UnloveCall.onPostExecute";
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
            final String tag = "TrackListActivity.LoveCall.onPostExecute";
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
        TrackInfoCall() {
            final String tag = "TrackListActivity.TrackInfoCall";
            Log.d(tag, "Constructor");
        }

        @Override
        protected String doInBackground(Track... params) {
            try {
                mNewTrack = mLfs.getTrackInfo(params[0]);
            }
            catch (InvalidObjectException e) {
                e.printStackTrace();
                return "";
            }
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
