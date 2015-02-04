package uk.co.spookypeanut.lovetag;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
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
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class TagInput extends ActionBarActivity {
    String mArtist;
    String mTitle;
    LastfmSession mLfs;
    final ArrayList<String> mActiveTagList = new ArrayList<>();
    final ArrayList<String> mInactiveTagList = new ArrayList<>();
    final ArrayList<ActiveElement> mAllTagList = new ArrayList<>();
    ActiveAdapter mTagAdaptor;

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
        mArtist = this.getIntent().getStringExtra("artist");
        mTitle = this.getIntent().getStringExtra("title");
        setContentView(R.layout.activity_tag_input);
        final EditText tagEntry = (EditText) findViewById(R.id.tagInputBox);
        mTagAdaptor = new ActiveAdapter(this, mAllTagList);
        ListView tagListView = (ListView) findViewById(R.id.tagList);
        tagListView.setAdapter(mTagAdaptor);
        tagEntry.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if ((keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)) {
                        addToList(Arrays.asList(tagEntry.getText().toString()), true);
                        tagEntry.setText("");
                        tagEntry.requestFocus();
                        return true;
                    }
                }
                return false;
            }
        });
        final Button okButton = (Button) findViewById(R.id.tag_ok);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultData = new Intent();
                Log.d(tag, "Tagging " + mTitle + " with " + mActiveTagList
                        .toString());
                resultData.putExtra("tagList", mActiveTagList);
                resultData.putExtra("artist", mArtist);
                resultData.putExtra("title", mTitle);
                setResult(RESULT_OK, resultData);
                finish();
            }
        });
        final Button cancelButton = (Button) findViewById(R.id.tag_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(tag, "User cancelled tagging");
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        tagListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int index, long id) {
                ActiveElement item = mAllTagList.get(index);
                List<String> tag_list = Arrays.asList(item.mLabel);
                removeFromList(tag_list);
                if (item.mActive) {
                    addToList(tag_list, false);
                } else {
                    addToList(tag_list, true);
                }
                updateList();
            }
        });
        GetExistingCall gec = new GetExistingCall();
        gec.execute();
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
            Log.i(tag, "ActiveElement: " + ae.mLabel + ", " + ae.mActive);
            textView.setText(ae.mLabel);
            if (ae.mActive) {
                textView.setTextColor(Color.BLACK);
            } else {
                textView.setTextColor(Color.LTGRAY);
            }
            return view;
        }
    }
}
