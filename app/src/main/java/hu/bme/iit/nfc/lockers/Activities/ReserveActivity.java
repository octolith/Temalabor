package hu.bme.iit.nfc.lockers.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import hu.bme.iit.nfc.lockers.Database.LockerDatabase;
import hu.bme.iit.nfc.lockers.Domain.LockerProcessSingleton;
import hu.bme.iit.nfc.lockers.Domain.ProcessState;
import hu.bme.iit.nfc.lockers.R;

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

    @Override
    protected void onPause() {
        super.onPause();
        LockerProcessSingleton.getInstance().setProcessState(ProcessState.NONE);
    }
}
