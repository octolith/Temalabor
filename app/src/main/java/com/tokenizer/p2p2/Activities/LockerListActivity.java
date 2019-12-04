package com.tokenizer.p2p2.Activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tokenizer.p2p2.Database.LockerDatabase;
import com.tokenizer.p2p2.Domain.LockerListAdapter;
import com.tokenizer.p2p2.Model.Locker;
import com.tokenizer.p2p2.R;

import java.util.List;

public class LockerListActivity extends AppCompatActivity implements LockerListAdapter.ItemClickListener {

    private RecyclerView recyclerView;
    private LockerListAdapter lockerListAdapter;
    private RecyclerView.LayoutManager layoutManager;

    private FloatingActionButton floatingActionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locker_list);

        floatingActionButton = findViewById(R.id.floatingActionButton);

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LockerListActivity.this, ReserveActivity.class));
            }
        });

        recyclerView = findViewById(R.id.lockerListRecyclerView);
        // use a linear layout manager
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        // specify an adapter
        lockerListAdapter = new LockerListAdapter(getApplicationContext());

        lockerListAdapter.setClickListener(this);
        recyclerView.setAdapter(lockerListAdapter);
    }

    private void retrieveLocker() {
        LockerDatabase.getInstance(this).lockerDao().loadAll().observe(this, new Observer<List<Locker>>() {
            @Override
            public void onChanged(@Nullable List<Locker> lockers) {
                lockerListAdapter.setLockerList(lockers);
            }
        });
    }

    @Override
    public void onItemClick(View view, int position) {

        // debug
        Toast.makeText(this, "You clicked " + ((LockerListAdapter) lockerListAdapter).getItem(position).getNumber() + " on row number " + position, Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, ReserveActivity.class);
        intent.putExtra("locker", ((LockerListAdapter) lockerListAdapter).getItem(position));
        startActivity(intent);
    }

    public void onReserveButtonClick(View view) {
        Intent intent = new Intent(this, ReserveActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }
}
