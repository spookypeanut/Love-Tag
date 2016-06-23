package uk.co.spookypeanut.loveandtag;

/**
 * Copyright (c) 2014 Henry Bush
 * Distributed under the GNU GPL v3. For full terms see the file COPYING.
 */

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;

public class TagInputActivity extends AppCompatActivity {
    final TagList mTagList = new TagList();
    Track mTrack;
    LastfmSession mLfs;
    TagList mOrigTags = new TagList();
    // This keeps a track of tags that have actually been manually untagged,
    // so that we don't accidentally remove all existing tags because we
    // haven't managed to get the tags from last.fm yet. I'm not sure this
    // *could* happen, but I don't want to risk it.
    TagAdapter mTagAdaptor;
    TagList mFreqTags = new TagList();

    private void checkLoved() {
        IsLovedCall ilc = new IsLovedCall();
        ilc.execute(mTrack);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String tag = "TagInputAct.onCreate";
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
        setContentView(R.layout.activity_tag_input);
        // Because this activity has a default launchMode,
        // we have to call this manually
        onNewIntent(this.getIntent());
        findViewById(R.id.ti_initialProgressBar).setVisibility(View.VISIBLE);
        GetTagsCall gec = new GetTagsCall();
        gec.execute(mTrack);
        Log.d(tag, "Tags on track: " + mOrigTags.toString());
        final FloatingActionButton addButton =
                (FloatingActionButton) findViewById(R.id.add_button);
        final ImageButton loveButton = (ImageButton) findViewById(R.id.tag_love_button);
        mTagAdaptor = new TagAdapter(this, mTagList);
        ListView tagListView = (ListView) findViewById(R.id.tagList);
        tagListView.setAdapter(mTagAdaptor);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final AutoCompleteTextView tagEntry = new AutoCompleteTextView(v.getContext());

                ArrayAdapter<String> adapter = new ArrayAdapter<>(v.getContext(),
                 android.R.layout.simple_dropdown_item_1line, mFreqTags.getAsStrings());

                tagEntry.setHint("Enter new tag");
                tagEntry.setAdapter(adapter);

                new AlertDialog.Builder(v.getContext())
                        .setTitle("Enter Tag")
                        .setView(tagEntry)
                        .setPositiveButton("Add", new DialogInterface
                                .OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                final String newTag =
                                        tagEntry.getText().toString();
                                Log.d("TagInputActivity", "newTag: " + newTag);
                                addNewTag(newTag);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                        .show();

            }
        });
        loveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTrack.mLoved) {
                    UnloveCall ulc = new UnloveCall();
                    ulc.execute(mTrack);
                } else {
                    LoveCall lc = new LoveCall();
                    lc.execute(mTrack);
                }
            }
        });
        tagListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int index, long id) {
                Tag tag = mTagList.get(index);
                boolean active = mTagList.get(index).mActive;
                if (active) {
                    UntagCall uc = new UntagCall();
                    uc.execute(mTrack.mArtist, mTrack.mTitle, tag.mName);
                } else {
                    TagCall tc = new TagCall();
                    tc.execute(mTrack.mArtist, mTrack.mTitle, tag.mName);
                }
                mTagList.get(index).mActive = !active;
                updateList();
            }
        });
    }

    public boolean addNewTag(String tag_name) {
        // According to last.fm: "Tags must be shorter than 256 characters
        // and may only contain letters, numbers, hyphens, spaces and colons"
        boolean regex_valid = Pattern.matches("^[A-Za-z0-9-: ]*$", tag_name);
        if (tag_name.length() >= 256 || !regex_valid) {
            Toast.makeText(this, R.string.ti_invalid_tag, Toast.LENGTH_LONG)
                                                                        .show();
            Log.e("addNewTag", "Invalid tag: " + tag_name);
            return false;
        }
        Tag tag = new Tag(tag_name);
        tag.mActive = true;
        mTagList.add(tag);
        updateList();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_tag_input, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        final String tag = "TIAct.onNewIntent";
        String artist = intent.getStringExtra("artist");
        String title = intent.getStringExtra("title");
        Log.i(tag, "Track: " + title + ", " + artist);
        mTrack = new Track(artist, title);
        updateTrack();
    }

    private void setLoved(boolean loved) {
        mTrack.mLoved = loved;
        ImageButton love_button = (ImageButton) findViewById(R.id
                .tag_love_button);
        Drawable d;
        if (loved) {
            d = ContextCompat.getDrawable(this, R.drawable.lovetrue);
        } else {
            d = ContextCompat.getDrawable(this, R.drawable.lovefalse);
        }
        love_button.setImageDrawable(d);
    }

    private void showWaitingDialog() {
        FrameLayout pd = (FrameLayout) findViewById(R.id
                .progressBarHolder);
        pd.setVisibility(FrameLayout.VISIBLE);
    }

    private void updateList() {
        findViewById(R.id.ti_initialProgressBar).setVisibility(View.GONE);
        Collections.sort(mTagList);
        mTagAdaptor.notifyDataSetChanged();
    }

    private void updateTrack() {
        ((TextView) findViewById(R.id.tag_artist)).setText(mTrack.mArtist);
        ((TextView) findViewById(R.id.tag_title)).setText(mTrack.mTitle);
        checkLoved();
    }

    private class GetTagsCall extends AsyncTask<Track, String, String> {
        TagList mTrackTags = new TagList();
        @Override
        protected String doInBackground(Track... params) {
            Track t = params[0];
            if (!t.isComplete()) {
                mTrackTags.mValid = false;
            } else {
                mTrackTags = mLfs.getTrackTags(t);
            }
            mFreqTags = mLfs.getGlobalTags();
            return "";
        }
        protected void onPostExecute(String result) {
            if (mFreqTags.size() == 0) {
                TextView warn;
                warn = (TextView) findViewById(R.id.ti_connectionwarning);
                warn.setText(R.string.ti_no_global_tags);
                warn.setVisibility(View.VISIBLE);
            }
            if (!mTrackTags.mValid || !mFreqTags.mValid) {
                TextView warn;
                warn = (TextView) findViewById(R.id.ti_connectionwarning);
                warn.setText(R.string.ti_connection_warning);
                warn.setVisibility(View.VISIBLE);
            }
            mTagList.addAll(mTrackTags);
            mOrigTags = mTrackTags;
            updateList();
        }
    }

    public class TagAdapter extends ArrayAdapter<Tag> {
        public TagAdapter(Context context, ArrayList<Tag> values) {
            super(context, R.layout.view_taglistentry, values);
            final String tag = "TagInputAct.TagAdapter";
            Log.v(tag, "Constructor");
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            TagListEntryView textView;
            textView = (TagListEntryView) view.findViewById(android.R.id.text1);
            Tag tag_obj = getItem(position);
            textView.setText(tag_obj.mName);
            textView.mActive = tag_obj.mActive;
            return view;
        }
    }
    private class IsLovedCall extends AsyncTask<Track, String, String> {
        Track mReturnTrack;

        @Override
        protected String doInBackground(Track... params) {
            mReturnTrack = params[0];
            mReturnTrack.mLoved = mLfs.isLoved(mReturnTrack);
            return "";
        }

        protected void onPostExecute(String result) {
            setLoved(mReturnTrack.mLoved);
        }
    }

    private class UnloveCall extends AsyncTask<Track, String, String> {
        @Override
        protected String doInBackground(Track... params) {
            Track track = params[0];
            boolean result = mLfs.unlove(track);
            String msg;
            if (result) {
                msg = getString(R.string.unlove_success);
            } else {
                msg = getString(R.string.unlove_failed);
            }
            return msg;
        }
        protected void onPostExecute(String result) {
            final String tag = "TIAct.Unlove.onPostExec";
            Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();
            Log.i(tag, result);
            checkLoved();
        }
    }
    private class LoveCall extends AsyncTask<Track, String, String> {
        @Override
        protected String doInBackground(Track... params) {
            Track track = params[0];
            boolean result = mLfs.love(track);
            String msg;
            if (result) {
                msg = getString(R.string.love_success);
            } else {
                msg = getString(R.string.love_failed);
            }
            return msg;
        }
        protected void onPostExecute(String result) {
            final String tag = "TIAct.Love.onPostExec";
            Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();
            Log.i(tag, result);
            checkLoved();
        }
    }

    private class TagCall extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            String artist = params[0];
            String title = params[1];
            String tag_cat = params[2];
            Track track = new Track(artist, title, false);
            boolean result = mLfs.tag(track, tag_cat);
            String msg;
            if (result) {
                setResult(RESULT_OK);
                msg = getString(R.string.tag_success);
            } else {
                setResult(RESULT_CANCELED);
                msg = getString(R.string.tag_failed);
            }
            return msg;
        }

        protected void onPostExecute(String result) {
            final String tag = "TIAct.Tag.onPostExec";
            Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();
            Log.i(tag, result);
            GetTagsCall gec = new GetTagsCall();
            gec.execute(mTrack);
        }
    }

    private class UntagCall extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            String artist = params[0];
            String title = params[1];
            String tag_name = params[2];
            Track track = new Track(artist, title);
            boolean result = mLfs.untag(track, tag_name);
            String msg;
            if (result) {
                setResult(RESULT_OK);
                msg = getString(R.string.untag_success);
            } else {
                setResult(RESULT_CANCELED);
                msg = getString(R.string.untag_failed);
            }
            return msg;
        }
        protected void onPostExecute(String result) {
            final String tag = "TIAct.Untag.onPostExec";
            Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();
            Log.i(tag, result);
        }
    }
}
