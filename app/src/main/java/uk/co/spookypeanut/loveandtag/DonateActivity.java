package uk.co.spookypeanut.loveandtag;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import uk.co.spookypeanut.loveandtag.util.IabHelper;
import uk.co.spookypeanut.loveandtag.util.IabResult;
import uk.co.spookypeanut.loveandtag.util.Purchase;


public class DonateActivity extends ActionBarActivity {
    static final String ITEM_SKU = "uk.co.spookypeanut.loveandtag.donate";
    static final int RC_DONATE = 137;

    IabHelper mHelper;
    boolean mIabWorks = false;
    Button mDonateButton;

    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener
            = new IabHelper.OnIabPurchaseFinishedListener() {
        final String tag = "mPurchaseFinishedListener";
        public void onIabPurchaseFinished(IabResult result,
                                          Purchase purchase)
        {
            if (result.isFailure()) {
                // Handle error
                Log.e(tag, "Failure in purchase");
                return;
            }
            if (purchase.getSku().equals(ITEM_SKU)) {
                Log.e(tag, "Purchase succeeded");
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final String tag = "DonateActivity.onCreate";
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donate);
        mHelper = new IabHelper(this, getString(R.string.billing_licence_key));
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    Log.d(tag, "In-app Billing setup failed: " + result);
                    mIabWorks = false;
                } else {
                    Log.d(tag, "In-app Billing is set up OK");
                    mIabWorks = true;
                }
            }
        });
        mDonateButton = (Button) findViewById(R.id.donate_button);
        mDonateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                donateClicked();
            }
        });
    }

    private void donateClicked() {
        mHelper.launchPurchaseFlow(this, ITEM_SKU, RC_DONATE,
                mPurchaseFinishedListener, "my_purchase_token");
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mHelper.handleActivityResult(requestCode, resultCode, data)) {
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_donate, menu);
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
