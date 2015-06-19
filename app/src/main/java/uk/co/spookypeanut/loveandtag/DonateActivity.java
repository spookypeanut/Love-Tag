package uk.co.spookypeanut.loveandtag;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import uk.co.spookypeanut.loveandtag.util.IabHelper;
import uk.co.spookypeanut.loveandtag.util.IabResult;
import uk.co.spookypeanut.loveandtag.util.Purchase;



public class DonateActivity extends ActionBarActivity {
//    static final String ITEM_SKU = "donate01";
//    static final String ITEM_SKU = "";
    static final String ITEM_SKU = "android.test.purchased";
    static final int RC_DONATE = 137;
    ArrayList<String> ITEMS = new ArrayList<>();


    IabHelper mHelper;
    boolean mIabWorks = false;
    Button mDonateButton;
    TextView mDonateMessage;

    private void fillItemsList() {
        ITEMS.add("android.test.purchased");
    }

    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener
            = new IabHelper.OnIabPurchaseFinishedListener() {
        final String tag = "mPurchaseFin..Listener";
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

    public class NoPurchasesException extends Exception {
        public NoPurchasesException(String message) {
            super(message);
        }
        public NoPurchasesException(String message, Throwable throwable) {
            super(message, throwable);
        }
    }

    public int getHighestPurchaseIndex() throws NoPurchasesException,
            RemoteException {
        String tag = "DonateActivity.getHighestPurchaseIndex";
        if (mHelper == null) {
            Log.wtf(tag, "mHelper is null");
        }
        List<String> purchases = mHelper.listPurchases();
        for (int p=0; p<purchases.size(); p++) {
            String purchase = purchases.get(p);
            Log.i(tag, "Purchase found: " + purchase);
        }
        String item;
        int final_value = -1;
        for (int i=0; i<ITEMS.size(); i++) {
            item = ITEMS.get(i);
            if (!purchases.contains(item)) {
                Log.d(tag, "Doesn't contain " + item);
                break;
            }
            final_value = i;
        }
        if (final_value < 0) {
            throw new NoPurchasesException("No purchases present");
        }
        return final_value;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final String tag = "DonateActivity.onCreate";
        super.onCreate(savedInstanceState);
        fillItemsList();
        setContentView(R.layout.activity_donate);
        mHelper = new IabHelper(this, getString(R.string.billing_licence_key));
        Log.d(tag, "About to run startSetup");
        IabHelper.OnIabSetupFinishedListener list = new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                final String tag = "BLAH";
                if (!result.isSuccess()) {
                    Log.d(tag, "In-app Billing setup failed: " + result);
                    mIabWorks = false;
                } else {
                    Log.d(tag, "In-app Billing is set up OK");
                    mIabWorks = true;
                    updateMessage();
                }
            }
        };
        Log.d(tag, "Declared listener");
        mHelper.startSetup(list);
        mDonateButton = (Button) findViewById(R.id.donate_button);
        mDonateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                donateClicked();
            }
        });
        mDonateMessage = (TextView) findViewById(R.id.donate_message);
    }

    private void updateMessage() {
        final String tag = "DonateActivity.updateMessage";
        int highest_index;
        try {
            highest_index = getHighestPurchaseIndex();
        }
        catch (NoPurchasesException e) {
            return;
        }
        catch (RemoteException e) {
            Log.e(tag, "RemoteException when trying to get purchases");
            return;
        }
        Resources res = getResources();
        String[] ty_msgs = res.getStringArray(R.array.thank_you_messages);
        mDonateMessage.setText(ty_msgs[highest_index]);
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
//        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }
}
