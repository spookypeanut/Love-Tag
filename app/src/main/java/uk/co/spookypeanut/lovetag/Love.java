package uk.co.spookypeanut.lovetag;

/**
 * Copyright (c) 2014 Henry Bush
 * Distributed under the GNU GPL v3. For full terms see the file COPYING.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Love extends ActionBarActivity {
    LastfmSession mLfs;
    UrlMaker mUrlMaker;
    Context mCurrentContext = this;
    String mNowPlayingTitle;
    String mNowPlayingArtist;
    ListEntry mPodView;
    private LayoutInflater mInflater;

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

        /*
        final Button button = (Button) findViewById(R.id.lovebutton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String tag;
                tag = "Love&Tag.Love.onClick";
                LoveCall lc = new LoveCall();
                lc.execute("Sleeper", "Pyrotechnician");
                Log.d(tag, "Submitted love");
            }
        });
*/
        mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPodView = new ListEntry(mCurrentContext);
        LinearLayout podLayout;
        podLayout = (LinearLayout)findViewById(R.id.playingOnDeviceLayout);
        podLayout.addView(mPodView);

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
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Log.i(tag, "Succeeded");
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
            String track = intent.getStringExtra("track");
            mNowPlayingArtist = artist;
            mNowPlayingTitle = track;
            mPodView.setMusic(artist, track);
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

    public class ListEntry extends LinearLayout {
        String mArtist;
        TextView mArtistView;
        String mTitle;
        TextView mTitleView;

        String mLoved;
        public ListEntry(Context context) {
            super(context);
            View.inflate(context, R.layout.view_listentry, this);
            mArtistView = (TextView) findViewById(R.id.artist);
            mTitleView = (TextView) findViewById(R.id.title);
            final ImageButton loveButton = (ImageButton) findViewById(R.id
                    .lovebutton);
            loveButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String tag;
                    tag = "Love&Tag.Love.ListEntry.onClick";
                    LoveCall lc = new LoveCall();
                    lc.execute(mArtist, mTitle);
                    Log.d(tag, "Submitted love");
                }
            });
        }


        public void setMusic(String artist, String title) {
            String tag = "Love&Tag.Love.ListEntry.setMusic";
            mArtist = artist;
            mTitle = title;
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
        }
    }

    private class LoveCall extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            String track = params[0];
            String artist = params[1];
            String tag = "Love&Tag.Love.LoveCall.doInBackground";
            boolean result = mLfs.love(track, artist);
            Log.i(tag, "Result: " + result);
            if (result == true) {
                Log.i(tag, "Love succeeded");
                setResult(RESULT_OK);
                finish();
            } else {
                Log.i(tag, "Love failed");
                setResult(RESULT_CANCELED);
                finish();
            }
            return "";
        }

        protected void onPostExecute(String result) {

        }
    }
}


