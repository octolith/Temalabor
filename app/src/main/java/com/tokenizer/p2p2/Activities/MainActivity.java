package com.tokenizer.p2p2.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.tokenizer.p2p2.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onReserveButtonClick(View view) {
        Intent intent = new Intent(this, ReserveActivity.class);
        startActivity(intent);
    }

    public void onOpenButtonClick(View view) {
        Intent intent = new Intent(this, OpenActivity.class);
        startActivity(intent);
    }

    public void onCloseButtonClick(View view) {
        Intent intent = new Intent(this, CloseActivity.class);
        startActivity(intent);
    }

    public void onReleaseButtonClick(View view) {
        Intent intent = new Intent(this, ReleaseActivity.class);
        startActivity(intent);
    }

    public void onLockersButtonClick(View view) {
        Intent intent = new Intent(this, LockerListActivity.class);
        startActivity(intent);
    }
}
