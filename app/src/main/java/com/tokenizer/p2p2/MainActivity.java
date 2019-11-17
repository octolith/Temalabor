package com.tokenizer.p2p2;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import java.security.Key;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SecuritySingleton securitySingletonInstance = SecuritySingleton.getInstance();
        securitySingletonInstance.setLockerCommand(LockerCommand.RESERVE);
    }
}
