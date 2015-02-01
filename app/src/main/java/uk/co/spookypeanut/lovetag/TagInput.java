package uk.co.spookypeanut.lovetag;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;


public class TagInput extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String tag = "Love&Tag.TagInput.onCreate";
        setContentView(R.layout.activity_tag_input);
        final EditText tagEntry = (EditText) findViewById(R.id.tagInputBox);
        ListView tagListView = (ListView) findViewById(R.id.tagList);
        final ArrayAdapter<String> tagAdaptor;
        final ArrayList<String> tagList = new ArrayList<String>();
        tagAdaptor = new ArrayAdapter<String> (this,
                android.R.layout.simple_list_item_1, tagList);
        tagListView.setAdapter(tagAdaptor);

        tagEntry.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if ((keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)) {
                        tagList.add(0, tagEntry.getText().toString());
                        tagAdaptor.notifyDataSetChanged();
                        tagEntry.setText("");
                        tagEntry.requestFocus();
                        return true;
                    }
                }
                return false;
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
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
