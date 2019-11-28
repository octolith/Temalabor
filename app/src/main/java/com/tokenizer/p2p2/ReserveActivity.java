package com.tokenizer.p2p2;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class ReserveActivity extends AppCompatActivity {

    private LockerDatabase lockerDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reserve);
        LockerProcessSingleton securitySingletonInstance = LockerProcessSingleton.getInstance();
        securitySingletonInstance.setProcessState(ProcessState.STARTINGRESERVE);
        this.lockerDatabase = LockerDatabase.getInstance(this);
    }
}
