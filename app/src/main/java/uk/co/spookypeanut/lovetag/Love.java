package uk.co.spookypeanut.lovetag;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class Love extends ActionBarActivity {
    LastfmSession mLfs;
    UrlMaker mUrlMaker;
    Context mCurrentContext = this;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        String tag = "Love&Tag.Love.onCreate";
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_love);

        IntentFilter iF = new IntentFilter();
        iF.addAction("com.android.music.metachanged");
        iF.addAction("com.android.music.playstatechanged");
        iF.addAction("com.android.music.playbackcomplete");
        iF.addAction("com.android.music.queuechanged");

        registerReceiver(mReceiver, iF);

        final Button button = (Button) findViewById(R.id.login);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            }
        });

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

protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    String tag = "Love&Tag.Love.onActivityResult";
    Log.i(tag, "Starting");
    // Check which request we're responding to
    if (requestCode == getResources().getInteger(R.integer.rc_log_in)) {
        // Make sure the request was successful
        if (resultCode == RESULT_OK) {
            if (data.getBooleanExtra("success", false)) {
                Log.i(tag, "Failed");
            } else {
                Log.i(tag, "Succeeded");
            }
        } else {
            Log.e("Love&Tag.Love", "Log in failed");
        }
    }
}

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");
            Log.v("tag ", action + " / " + cmd);
            String artist = intent.getStringExtra("artist");
            String album = intent.getStringExtra("album");
            String track = intent.getStringExtra("track");
            Log.v("tag", artist + ":" + album + ":" + track);
            Toast.makeText(Love.this, track, Toast.LENGTH_SHORT).show();
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
}


