package hu.bme.iit.nfc.lockers.Activities;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.View;
import android.widget.Toast;

import hu.bme.iit.nfc.lockers.Database.LockerDatabase;
import hu.bme.iit.nfc.lockers.Domain.LockerAdapter;
import hu.bme.iit.nfc.lockers.Domain.LockerProcessSingleton;
import hu.bme.iit.nfc.lockers.Domain.ProcessState;
import hu.bme.iit.nfc.lockers.Model.Locker;
import hu.bme.iit.nfc.lockers.R;

import java.util.List;

public class LockersActivity extends AppCompatActivity implements LockerAdapter.ItemClickListener {

    private RecyclerView recyclerView;
    private LockerAdapter lockerAdapter;
    private LockerDatabase lockerDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lockers);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LockersActivity.this, ReserveActivity.class));
            }
        });

        recyclerView = findViewById(R.id.lockerListRecyclerView);
        // use a linear layout manager
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // specify an adapter
        lockerAdapter = new LockerAdapter(getApplicationContext());
        lockerAdapter.setClickListener(this);
        recyclerView.setAdapter(lockerAdapter);

        lockerDatabase = LockerDatabase.getInstance(getApplicationContext());

        retrieveLockers();
    }

    private void retrieveLockers() {
        lockerDatabase.lockerDao().loadAll().observe(this, new Observer<List<Locker>>() {
            @Override
            public void onChanged(@Nullable List<Locker> lockers) {
                lockerAdapter.setLockerList(lockers);
            }
        });
    }

    @Override
    public void onItemClick(View view, int position) {

        Toast.makeText(this, "You clicked "
                + lockerAdapter.getItem(position).getNumber()
                + " on row number " + position, Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, LockerActivity.class);
        intent.putExtra("locker", lockerAdapter.getItem(position));
        startActivity(intent);
    }

    public void onReserveButtonClick(View view) {
        startActivity(new Intent(this, ReserveActivity.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        LockerProcessSingleton.getInstance().setProcessState(ProcessState.NONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LockerProcessSingleton.getInstance().setProcessState(ProcessState.NONE);
    }

}
