package com.tokenizer.p2p2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class LockerDetailsActivity extends AppCompatActivity {

    private Locker locker;

    public Locker getLocker() {
        return locker;
    }

    public void setLocker(Locker locker) {
        this.locker = locker;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locker_details);
    }

    public void onOpenButtonClick(View view) {
        Intent intent = new Intent(this, OpenActivity.class);
        intent.putExtra("locker", locker);
        startActivity(intent);
    }

    public void onCloseButtonClick(View view) {
        Intent intent = new Intent(this, CloseActivity.class);
        intent.putExtra("locker", locker);
        startActivity(intent);
    }

    public void onReleaseButtonClick(View view) {
        Intent intent = new Intent(this, ReleaseActivity.class);
        intent.putExtra("locker", locker);
        startActivity(intent);
    }
}
