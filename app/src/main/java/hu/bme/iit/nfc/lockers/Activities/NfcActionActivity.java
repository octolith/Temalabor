package hu.bme.iit.nfc.lockers.Activities;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;

import hu.bme.iit.nfc.lockers.Domain.LockerProcess;
import hu.bme.iit.nfc.lockers.Domain.ProcessState;
import hu.bme.iit.nfc.lockers.R;

import static hu.bme.iit.nfc.lockers.Domain.ProcessState.NONE;

public class NfcActionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_action);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        LockerProcess.getInstance().setActivity(NfcActionActivity.this);
    }
}
