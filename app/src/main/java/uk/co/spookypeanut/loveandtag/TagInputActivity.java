package uk.co.spookypeanut.loveandtag;

/**
 * Copyright (c) 2014 Henry Bush
 * Distributed under the GNU GPL v3. For full terms see the file COPYING.
 */

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;

public class TagInputActivity extends ActionBarActivity {
    final TagList mTagList = new TagList();
    Track mTrack;
    LastfmSession mLfs;
    TagList mOrigTags = new TagList();
    // This keeps a track of tags that have actually been manually untagged,
    // so that we don't accidentally remove all existing tags because we
    // haven't managed to get the tags from last.fm yet. I'm not sure this
    // *could* happen, but I don't want to risk it.
    TagList mActallyUntagged = new TagList();
    TagAdapter mTagAdaptor;
    private TextWatcher inputTextWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int st, int c, int a) { }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            updateOkButton();
        }

        public void afterTextChanged(Editable s) { }
    };

    private void checkLoved() {
        IsLovedCall ilc = new IsLovedCall();
        ilc.execute(mTrack);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String tag = "TagInputActivity.onCreate";
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
        final EditText tagEntry = (EditText) findViewById(R.id.tagInputBox);
        final Button cancelButton = (Button) findViewById(R.id.tag_cancel);
        final Button okButton = (Button) findViewById(R.id.tag_ok);
        final ImageButton loveButton = (ImageButton) findViewById(R.id.tag_love_button);
        mTagAdaptor = new TagAdapter(this, mTagList);
        ListView tagListView = (ListView) findViewById(R.id.tagList);
        tagListView.setAdapter(mTagAdaptor);
        tagEntry.addTextChangedListener(inputTextWatcher);
        tagEntry.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                updateOkButton();
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if ((keyCode == KeyEvent.KEYCODE_ENTER ||
                         keyCode == KeyEvent.KEYCODE_DPAD_CENTER)) {
                        // When enter is pressed in the field,
                        // add the tag and reset the input
                        Tag tag = new Tag(tagEntry.getText().toString());
                        tag.mActive = true;
                        mTagList.add(tag);
                        updateList();
                        tagEntry.setText("");
                        tagEntry.requestFocus();
                        return true;
                    }
                }
                return false;
            }
        });
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String current_text = tagEntry.getText().toString();
                if (!current_text.equals("")) {
                    Tag tag_obj = new Tag(current_text);
                    // If something is entered in the input box,
                    // assume it's a tag to be used
                    tag_obj.mActive = true;
                    mTagList.add(tag_obj);
                    updateList();
                }
                if (mTagList.size() == 0) {
                    return;
                }
                TagList for_tagging = mTagList.getActiveList();
                for_tagging.removeAll(mOrigTags);
                Log.d(tag, "For tagging: " + for_tagging.toString());
                TagList for_untagging = (TagList) mOrigTags.clone();
                for_untagging.removeAll(mTagList.getActiveList());
                Log.d(tag, "For untagging: " + for_untagging.toString());
                Log.d(tag, "Tagging " + mTrack.mTitle + " with " +
                        for_tagging.toString());
                TagCall tc = new TagCall();
                tc.execute(mTrack.mArtist, mTrack.mTitle,
                        TextUtils.join(",", for_tagging));
                for (Tag tag_obj : for_untagging) {
                    if (!mActallyUntagged.contains(tag_obj)) continue;
                    UntagCall uc = new UntagCall();
                    uc.execute(mTrack.mArtist, mTrack.mTitle, tag_obj.mName);
                }
                showWaitingDialog();
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(tag, "User cancelled tagging");
                setResult(RESULT_CANCELED);
                finish();
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
                boolean previous = mTagList.get(index).mActive;
                if (previous) {
                    // If we are de-activating the tag, we should also
                    // add it to the actually deactivated list
                    mActallyUntagged.add(mTagList.get(index));
                }
                mTagList.get(index).mActive = !previous;
                updateOkButton();
                updateList();
            }
        });
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
        final String tag = "TagInputActivity.onNewIntent";
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

    private void updateOkButton() {
        final Button okButton = (Button) findViewById(R.id.tag_ok);
        final EditText tagEntry = (EditText) findViewById(R.id.tagInputBox);
        String current_text = tagEntry.getText().toString();
        if (mTagList.getActiveList().size() == 0 && current_text.equals("")) {
            okButton.setEnabled(false);
        } else {
            okButton.setEnabled(true);
        }
    }

    private void updateTrack() {
        ((TextView) findViewById(R.id.tag_artist)).setText(mTrack.mArtist);
        ((TextView) findViewById(R.id.tag_title)).setText(mTrack.mTitle);
        checkLoved();
    }

    private class GetTagsCall extends AsyncTask<Track, String, String> {
        TagList mFreqTags = new TagList();
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
            if (!mTrackTags.mValid || !mFreqTags.mValid) {
                TextView warn;
                warn = (TextView) findViewById(R.id.ti_connectionwarning);
                warn.setText(R.string.ti_connection_warning);
                warn.setVisibility(View.VISIBLE);
            }
            mTagList.addAll(mTrackTags);
            mOrigTags = mTrackTags;
            mTagList.addAll(mFreqTags);
            updateList();
        }
    }

    public class TagAdapter extends ArrayAdapter<Tag> {
        public TagAdapter(Context context, ArrayList<Tag> values) {
            super(context, R.layout.view_taglistentry, values);
            final String tag = "TagInputActivity.TagAdapter";
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
            final String tag = "TagInputActivity.UnloveCall.onPostExecute";
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
            final String tag = "TagInputActivity.LoveCall.onPostExecute";
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
            final String tag = "Love.TagCall.onPostExecute";
            Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();
            Log.i(tag, result);
            finish();
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
            final String tag = "Love.UntagCall.onPostExecute";
            Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();
            Log.i(tag, result);
            finish();
        }
    }
}
