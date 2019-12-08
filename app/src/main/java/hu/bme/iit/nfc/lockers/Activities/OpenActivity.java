package hu.bme.iit.nfc.lockers.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import hu.bme.iit.nfc.lockers.Domain.LockerProcessSingleton;
import hu.bme.iit.nfc.lockers.Model.Locker;
import hu.bme.iit.nfc.lockers.Domain.ProcessState;
import hu.bme.iit.nfc.lockers.R;

public class OpenActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_open);
        Intent i = getIntent();
        if(i.hasExtra("locker")) {
            locker = i.getParcelableExtra("locker");
            LockerProcessSingleton.getInstance().setLocker(locker);
        }
        LockerProcessSingleton.getInstance().setProcessState(ProcessState.STARTINGOPEN);
    }
}
