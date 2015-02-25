package uk.co.spookypeanut.lovetag;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class TagInput extends ActionBarActivity {
    Track mTrack;
    LastfmSession mLfs;
    final ArrayList<String> mActiveTagList = new ArrayList<>();
    final ArrayList<String> mInactiveTagList = new ArrayList<>();
    final ArrayList<ActiveElement> mAllTagList = new ArrayList<>();
    ActiveAdapter mTagAdaptor;
    private TextWatcher inputTextWatcher = new TextWatcher() {
        public void afterTextChanged(Editable s) { }
        public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            updateOkButton();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String tag = "Love&Tag.TagInput.onCreate";
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
        String artist = this.getIntent().getStringExtra("artist");
        String title = this.getIntent().getStringExtra("title");
        mTrack = new Track(artist, title);
        setContentView(R.layout.activity_tag_input);
        ((TextView) findViewById(R.id.tag_artist)).setText(artist);
        ((TextView) findViewById(R.id.tag_title)).setText(title);
        checkLoved();
        final EditText tagEntry = (EditText) findViewById(R.id.tagInputBox);
        final Button cancelButton = (Button) findViewById(R.id.tag_cancel);
        final Button okButton = (Button) findViewById(R.id.tag_ok);
        final ImageButton loveButton = (ImageButton) findViewById(R.id.tag_love_button);
        mTagAdaptor = new ActiveAdapter(this, mAllTagList);
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
                        String current_text = tagEntry.getText().toString();
                        addToList(Arrays.asList(current_text), true);
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
                    // If something is entered in the input box,
                    // assume it's a tag
                    addToList(Arrays.asList(current_text), true);
                }
                if (mActiveTagList.size() == 0) {
                    return;
                }
                Intent resultData = new Intent();
                Log.d(tag, "Tagging " + mTrack.mTitle + " with " +
                        mActiveTagList.toString());
                TagCall tc = new TagCall();
                tc.execute(mTrack.mArtist, mTrack.mTitle,
                           TextUtils.join(",", mActiveTagList));
                // TODO: Put a little "working" animation in
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
                if (mTrack.mLoved == true) {
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
                ActiveElement item = mAllTagList.get(index);
                List<String> tag_list = Arrays.asList(item.mLabel);
                removeFromList(tag_list);
                addToList(tag_list, !item.mActive);
                updateOkButton();
                updateList();
            }
        });
        GetExistingCall gec = new GetExistingCall();
        gec.execute();
    }

    private void checkLoved() {
        IsLovedCall ilc = new IsLovedCall();
        ilc.execute(mTrack);
    }

    private void setLoved(boolean loved) {
        mTrack.mLoved = loved;
        ImageButton love_button = (ImageButton) findViewById(R.id.tag_love_button);
        Drawable d;
        if (loved == true) {
            d = ContextCompat.getDrawable(this, R.drawable.lovetrue);
        } else {
            d = ContextCompat.getDrawable(this, R.drawable.lovefalse);
        }
        love_button.setImageDrawable(d);
    }

    private void removeFromList(List<String> tag_list) {
        mActiveTagList.removeAll(tag_list);
        mInactiveTagList.removeAll(tag_list);
        updateList();
    }

    private void addToList(List<String> tag_list, boolean active) {
        String tag = "Love&Tag.TagInput.addToList";
        if (active) {
            mActiveTagList.addAll(tag_list);
        } else {
            mInactiveTagList.addAll(tag_list);
        }
        ArrayList<String> remove = new ArrayList<>();
        for (String t : mInactiveTagList) {
            if (mActiveTagList.contains(t)) {
                remove.add(t);
            }
        }
        mInactiveTagList.removeAll(remove);
        updateList();
    }

    private void updateOkButton() {
        final Button okButton = (Button) findViewById(R.id.tag_ok);
        final EditText tagEntry = (EditText) findViewById(R.id.tagInputBox);
        String current_text = tagEntry.getText().toString();
        if (mActiveTagList.size() == 0 && current_text.equals("")) {
            okButton.setEnabled(false);
        } else {
            okButton.setEnabled(true);
        }
    }

    private void updateList() {
        mAllTagList.clear();
        for (String t : mActiveTagList) {
            ActiveElement ae = new ActiveElement(t, true);
            mAllTagList.add(ae);
        }
        for (String t : mInactiveTagList) {
            ActiveElement ae = new ActiveElement(t, false);
            mAllTagList.add(ae);
        }
        mTagAdaptor.notifyDataSetChanged();
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
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private class GetExistingCall extends AsyncTask<String, String, String> {
        List<String> mTempTags = new ArrayList<>();
        @Override
        protected String doInBackground(String... params) {
            String tag = "Love&Tag.TagInput.GetExistingCall.doInBackground";
            mTempTags = mLfs.getTags();
            return "";
        }
        protected void onPostExecute(String result) {
            addToList(mTempTags, false);
        }
    }

    private class ActiveElement {
        String mLabel;
        boolean mActive;

        public ActiveElement(String label, boolean active) {
            mLabel = label;
            mActive = active;
        }
    }

    public class ActiveAdapter extends ArrayAdapter<ActiveElement> {
        private final Context context;
        private final ArrayList<ActiveElement> values;

        public ActiveAdapter(Context context, ArrayList<ActiveElement>
                values) {
            super(context, android.R.layout.simple_list_item_1, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            String tag = "Love&Tag.TagInput.ActiveAdapter.getView";
            View view = super.getView(position, convertView, parent);
            TextView textView = (TextView) view.findViewById(android.R.id.text1);
            ActiveElement ae = getItem(position);
            textView.setText(ae.mLabel);
            if (ae.mActive) {
                textView.setTextColor(Color.BLACK);
            } else {
                textView.setTextColor(Color.LTGRAY);
            }
            return view;
        }
    }
    private class IsLovedCall extends AsyncTask<Track, String, String> {
        Track mReturnTrack;

        @Override
        protected String doInBackground(Track... params) {
            String tag = "Love&Tag.TagInput.IsLovedCall.doInBackground";
            mReturnTrack = params[0];
            boolean is_loved = mLfs.isLoved(mReturnTrack);
            mReturnTrack.mLoved = is_loved;
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
            if (result == true) {
                msg = getString(R.string.unlove_success);
            } else {
                msg = getString(R.string.unlove_failed);
            }
            return msg;
        }
        protected void onPostExecute(String result) {
            String tag = "Love&Tag.TagInput.UnloveCall.onPostExecute";
            Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();
            Log.i(tag, result);
            checkLoved();
        }
    }
    private class LoveCall extends AsyncTask<Track, String, String> {
        @Override
        protected String doInBackground(Track... params) {
            Track track = params[0];
            String tag = "Love&Tag.TagInput.LoveCall.doInBackground";
            boolean result = mLfs.love(track);
            String msg;
            if (result == true) {
                msg = getString(R.string.love_success);
            } else {
                msg = getString(R.string.love_failed);
            }
            return msg;
        }
        protected void onPostExecute(String result) {
            String tag = "Love&Tag.TagInput.LoveCall.onPostExecute";
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
            finish();
        }
    }
}
