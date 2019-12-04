package com.tokenizer.p2p2.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.tokenizer.p2p2.Domain.LockerProcessSingleton;
import com.tokenizer.p2p2.Model.Locker;
import com.tokenizer.p2p2.Domain.ProcessState;
import com.tokenizer.p2p2.R;

public class OpenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open);
        Intent i = getIntent();
        if(i.hasExtra("locker")) {
            Locker locker = (Locker) i.getParcelableExtra("locker");
        }
        LockerProcessSingleton securitySingletonInstance = LockerProcessSingleton.getInstance();
        securitySingletonInstance.setProcessState(ProcessState.STARTINGOPEN);
    }
}
