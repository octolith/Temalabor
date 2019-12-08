package hu.bme.iit.nfc.lockers.Activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;

import hu.bme.iit.nfc.lockers.Domain.LockerProcess;
import hu.bme.iit.nfc.lockers.Domain.ProcessState;
import hu.bme.iit.nfc.lockers.Model.Locker;
import hu.bme.iit.nfc.lockers.R;

public class LockerDetailsActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_locker_details);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent i = getIntent();
        if(i.hasExtra("locker")) {
            locker = i.getParcelableExtra("locker");
        }
    }

    public void onOpenButtonClick(View view) {
        Intent intent = new Intent(this, NfcActionActivity.class);
        LockerProcess.getInstance().setLocker(locker);
        LockerProcess.getInstance().setProcessState(ProcessState.STARTINGOPEN);
        startActivity(intent);
    }

    public void onCloseButtonClick(View view) {
        Intent intent = new Intent(this, NfcActionActivity.class);
        LockerProcess.getInstance().setLocker(locker);
        LockerProcess.getInstance().setProcessState(ProcessState.STARTINGCLOSE);
        startActivity(intent);
    }

    public void onReleaseButtonClick(View view) {
        Intent intent = new Intent(this, NfcActionActivity.class);
        LockerProcess.getInstance().setLocker(locker);
        LockerProcess.getInstance().setProcessState(ProcessState.STARTINGRELEASE);
        startActivity(intent);
    }

}
