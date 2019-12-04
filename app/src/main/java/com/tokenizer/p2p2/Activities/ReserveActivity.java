package com.tokenizer.p2p2.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.tokenizer.p2p2.Database.LockerDatabase;
import com.tokenizer.p2p2.Domain.LockerProcessSingleton;
import com.tokenizer.p2p2.Domain.ProcessState;
import com.tokenizer.p2p2.R;

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
