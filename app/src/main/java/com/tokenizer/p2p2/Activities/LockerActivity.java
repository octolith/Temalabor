package com.tokenizer.p2p2.Activities;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;

import com.tokenizer.p2p2.Model.Locker;
import com.tokenizer.p2p2.R;

public class LockerActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_locker);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent i = getIntent();
        if(i.hasExtra("locker")) {
            locker = i.getParcelableExtra("locker");
        }
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
