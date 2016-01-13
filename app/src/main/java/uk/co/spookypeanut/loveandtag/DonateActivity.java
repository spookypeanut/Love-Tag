package uk.co.spookypeanut.loveandtag;

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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import uk.co.spookypeanut.loveandtag.util.IabHelper;
import uk.co.spookypeanut.loveandtag.util.IabResult;
import uk.co.spookypeanut.loveandtag.util.Purchase;



public class DonateActivity extends ActionBarActivity {
//    static final String ITEM_SKU = "donate001";
//    static final String ITEM_SKU = "";
//    static final String ITEM_SKU = "android.test.purchased";
    static final int RC_DONATE = 137;
    ArrayList<String> ITEMS = new ArrayList<>();

    IabHelper mHelper;
    boolean mIabWorks = false;
    Button mDonateButton;
    TextView mDonateMessage;
    String mCurrentItem;
    int mCurrentIndex;

    private void fillItemsList() {
        ITEMS.add("donate001");
        ITEMS.add("donate002");
        ITEMS.add("donate003");
        ITEMS.add("donate004");
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
            if (ITEMS.contains(purchase.getSku())) {
                Log.e(tag, "Purchase succeeded");
                updateItem();
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

    private void updateItem() {
        final String tag = "DonateActivity.updateIndex";
        try {
            mCurrentIndex = getHighestPurchaseIndex() + 1;
        }
        catch (NoPurchasesException e) {
            mCurrentIndex = 0;
        }
        catch (RemoteException e) {
            Toast.makeText(this, R.string.remote_error_on_donate,
                    Toast.LENGTH_SHORT).show();
            Log.e(tag, "RemoteException when updating index");
        }
        try {
            mCurrentItem = ITEMS.get(mCurrentIndex);
            mDonateButton.setEnabled(true);
        }
        catch (IndexOutOfBoundsException e) {
            mDonateButton.setEnabled(false);
        }
        Resources res = getResources();
        String[] ty_msgs = res.getStringArray(R.array.donate_messages);
        String[] ty_button = res.getStringArray(R.array.donate_button_labels);
        mDonateMessage.setText(ty_msgs[mCurrentIndex]);
        mDonateButton.setText(ty_button[mCurrentIndex]);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final String tag = "DonateActivity.onCreate";
        super.onCreate(savedInstanceState);
        fillItemsList();
        setContentView(R.layout.activity_donate);
        mHelper = new IabHelper(this, getString(R.string.billing_licence_key));
        mDonateMessage = (TextView) findViewById(R.id.donate_message);
        mDonateButton = (Button) findViewById(R.id.donate_button);
        Log.d(tag, "About to run startSetup");
        IabHelper.OnIabSetupFinishedListener list = new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                final String tag = "IabSetupFinished";
                if (!result.isSuccess()) {
                    Log.i(tag, "In-app Billing setup failed: " + result);
                    mDonateMessage.setText(R.string.donate_setup_failed);
                    mDonateButton.setText(R.string.donate_button_setup_failed);
                    mIabWorks = false;
                } else {
                    Log.i(tag, "In-app Billing is set up OK");
                    mIabWorks = true;
                    updateItem();
                }
            }
        };
        Log.d(tag, "Declared listener");
        mHelper.startSetup(list);
        mDonateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                donateClicked();
            }
        });
    }

    private void donateClicked() {
        mHelper.launchPurchaseFlow(this, mCurrentItem, RC_DONATE,
                mPurchaseFinishedListener, "my_purchase_token");
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
