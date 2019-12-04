package com.tokenizer.p2p2.Domain;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tokenizer.p2p2.Model.Locker;
import com.tokenizer.p2p2.R;

import java.util.List;

public class LockerListAdapter extends RecyclerView.Adapter<LockerListAdapter.LockerViewHolder> {
    private List<Locker> lockerList;
    private ItemClickListener itemClickListener;
    private Context context;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class LockerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // each data item is just a string in this case
        public TextView textView;
        public LockerViewHolder(TextView v) {
            super(v);
            textView = v;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (itemClickListener != null) itemClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    public LockerListAdapter(Context context) {
        this.context = context;
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public LockerListAdapter(List<Locker> lockerList) {
        this.lockerList = lockerList;
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public LockerListAdapter.LockerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // create a new view
        TextView v = (TextView) LayoutInflater.from(context)
                .inflate(R.layout.recyclerview_row, parent, false);

        LockerViewHolder vh = new LockerViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)

    @Override
    public void onBindViewHolder(@NonNull LockerListAdapter.LockerViewHolder viewHolder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        viewHolder.textView.setText(lockerList.get(position).getNumber());
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if(lockerList == null) {
            return 0;
        }
        return lockerList.size();
    }

    public void setLockerList(List<Locker> lockerList) {
        this.lockerList = lockerList;
        notifyDataSetChanged();
    }

    public List<Locker> getLockerList() {
        return lockerList;
    }

    // convenience method for getting data at click position
    public Locker getItem(int id) {
        return lockerList.get(id);
    }

    // allows clicks events to be caught
    public void setClickListener(ItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}