package uk.co.spookypeanut.loveandtag;

/**
 * Copyright (c) 2014 Henry Bush
 * Distributed under the GNU GPL v3. For full terms see the file COPYING.
 */

import android.content.Context;
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
import android.widget.Toast;

public class LoginActivity extends ActionBarActivity {
    private AutoCompleteTextView mUsernameView;
    private EditText mPasswordView;
    private FrameLayout mProgress;

    public void attemptLogin() {
        // If the views have errors on them due to previous calls to this
        // method, clear them out
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        String username = mUsernameView.getText().toString();
        if (TextUtils.isEmpty(username)) {
            // Flag to the user that this field can't be empty
            mUsernameView.setError(getString(R.string.error_field_required));
            mUsernameView.requestFocus();
            return;
        }
        String password = mPasswordView.getText().toString();
        if (TextUtils.isEmpty(password)) {
            // Flag to the user that this field can't be empty
            mPasswordView.setError(getString(R.string.error_field_required));
            mPasswordView.requestFocus();
            return;
        }
        Md5Maker md5m = new Md5Maker();
        // The last.fm docs for getMobileSession here:
        // http://www.last.fm/api/show/auth.getMobileSession
        // are out of date. Instead of sending username and password,
        // it is now required to sending username and auth token,
        // generated as depicted below.
        String authToken = md5m.encode(username + md5m.encode(password));
        new LoginCall().execute(username, authToken);
        showWaitingDialog();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mUsernameView = (AutoCompleteTextView) findViewById(R.id.username);
        mPasswordView = (EditText) findViewById(R.id.password);
        mProgress = (FrameLayout) findViewById(R.id.login_progress);
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

    private void showWaitingDialog() {
        // Display the circle thing to show us that login is in progress
        mProgress.setVisibility(FrameLayout.VISIBLE);
    }

    private void hideWaitingDialog() {
        mProgress.setVisibility(FrameLayout.GONE);
    }

    private class LoginCall extends AsyncTask<String, String, String> {
        LastfmSession mLfs = new LastfmSession();
        @Override
        protected String doInBackground(String... params) {
            final String tag = "LoginCall.doInBackground";
            String username = params[0];
            String authToken = params[1];
            boolean result;
            try {
                result = mLfs.logIn(username, authToken);
            }
            catch (InvalidCredentialsException e) {
                return getString(R.string.login_bad_credentials);
            }
            Log.i(tag, "Result: " + result);
            return "";
        }

        protected void onPostExecute(String result) {
            final String tag = "LoginCall.onPostExecute";
            if (mLfs.isLoggedIn()) {
                Log.i(tag, "Logged in");
                setResult(RESULT_OK);
                finish();
            } else {
                Log.i(tag, "Not logged in");
                hideWaitingDialog();
                Context c = App.getContext();
                String msg = result;
                if (msg.equals("")) {
                    msg = getString(R.string.login_generic_failure);
                }
                Toast.makeText(c, msg, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
