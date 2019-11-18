package com.tokenizer.p2p2;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class OpenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open);
        LockerProcessSingleton securitySingletonInstance = LockerProcessSingleton.getInstance();
        securitySingletonInstance.setProcessState(ProcessState.STARTINGOPEN);
    }
}
