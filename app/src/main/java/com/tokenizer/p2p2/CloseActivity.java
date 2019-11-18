package com.tokenizer.p2p2;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class CloseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_close);
        LockerProcessSingleton securitySingletonInstance = LockerProcessSingleton.getInstance();
        securitySingletonInstance.setProcessState(ProcessState.STARTINGCLOSE);
    }
}
