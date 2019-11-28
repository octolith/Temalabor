package com.tokenizer.p2p2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import java.util.List;

public class LockerListActivity extends AppCompatActivity implements LockerListAdapter.ItemClickListener {

    private LockerDao lockerDao;
    private List<Locker> lockerLiveData;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locker_list);

        lockerDao = LockerDatabase.getInstance(this).lockerDao();
        lockerLiveData = lockerDao.loadAll();

        recyclerView = findViewById(R.id.lockerListRecyclerView);
        // use a linear layout manager
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        // specify an adapter
        mAdapter = new LockerListAdapter(lockerLiveData);
        ((LockerListAdapter) mAdapter).setClickListener(this);
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onItemClick(View view, int position) {

        // debug
        Toast.makeText(this, "You clicked " + ((LockerListAdapter) mAdapter).getItem(position).getNumber() + " on row number " + position, Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, ReserveActivity.class);
        intent.putExtra("locker", ((LockerListAdapter) mAdapter).getItem(position));
        startActivity(intent);
    }

    public void onReserveButtonClick(View view) {
        Intent intent = new Intent(this, ReserveActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        lockerLiveData = lockerDao.loadAll();
        mAdapter = new LockerListAdapter(lockerLiveData);
        recyclerView.setAdapter(mAdapter);
    }
}
