package uk.co.spookypeanut.loveandtag;

/**
 * Copyright (c) 2014 Henry Bush
 * Distributed under the GNU GPL v3. For full terms see the file COPYING.
 */

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

public class LoginActivity extends ActionBarActivity {
    private AutoCompleteTextView mUsernameView;
    private EditText mPasswordView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mUsernameView = (AutoCompleteTextView) findViewById(R.id.username);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.lovebutton || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mLogInButton = (Button) findViewById(R.id.log_in_button);
        mLogInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });
    }

    /**
     * Attempts to sign in to the account specified by the login form.
     * If there are form errors (invalid username, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();

        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            mPasswordView.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            mUsernameView.requestFocus();
            return;
        }

        Md5Maker md5m;
        md5m = new Md5Maker();
        String authToken = md5m.encode(username + md5m.encode(password));
        new LoginCall().execute(username, authToken);
        showWaitingDialog();
    }

    private void showWaitingDialog() {
        FrameLayout pd = (FrameLayout) findViewById(R.id.login_progress);
        pd.setVisibility(FrameLayout.VISIBLE);
    }

    private class LoginCall extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            final String tag = "LoginCall.doInBackground";
            String username = params[0];
            String authToken = params[1];
            LastfmSession lfs;
            lfs = new LastfmSession();
            boolean result = lfs.logIn(username, authToken);
            Log.i(tag, "Result: " + result);
            if (lfs.isLoggedIn()) {
                Log.i(tag, "Logged in");
                setResult(RESULT_OK);
                finish();
            } else {
                Log.i(tag, "Not logged in");
                setResult(RESULT_CANCELED);
                finish();
            }
            return "";

        }

        protected void onPostExecute(String result) {

        }
    }
}



