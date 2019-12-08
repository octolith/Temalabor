package hu.bme.iit.nfc.lockers.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import hu.bme.iit.nfc.lockers.Domain.LockerProcessSingleton;
import hu.bme.iit.nfc.lockers.Domain.ProcessState;
import hu.bme.iit.nfc.lockers.R;

public class ReleaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_release);
        LockerProcessSingleton securitySingletonInstance = LockerProcessSingleton.getInstance();
        securitySingletonInstance.setProcessState(ProcessState.STARTINGRELEASE);
    }
}
