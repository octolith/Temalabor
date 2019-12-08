package hu.bme.iit.nfc.lockers.Activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import hu.bme.iit.nfc.lockers.Domain.LockerProcess;
import hu.bme.iit.nfc.lockers.R;

public class NfcActionActivity extends AppCompatActivity {

    LocalBroadcastManager localBroadcastManager;
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("hu.bme.iit.nfc.lockers.nfcactivity.close")){
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_action);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        LockerProcess.getInstance().setActivity(NfcActionActivity.this);
        localBroadcastManager = LocalBroadcastManager.getInstance(NfcActionActivity.this);
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction("hu.bme.iit.nfc.lockers.nfcactivity.close");
        localBroadcastManager.registerReceiver(broadcastReceiver, mIntentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        localBroadcastManager.unregisterReceiver(broadcastReceiver);
    }
}
