package hu.bme.iit.nfc.lockers.Domain;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import hu.bme.iit.nfc.lockers.Model.Locker;
import hu.bme.iit.nfc.lockers.R;

import java.util.List;

public class LockerAdapter extends RecyclerView.Adapter<LockerAdapter.LockerViewHolder> {
    private List<Locker> lockerList;
    private ItemClickListener itemClickListener;
    private Context context;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class LockerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // each data item is just a string in this case
        public TextView textView;
        public LockerViewHolder(View view) {
            super(view);
            textView = view.findViewById(R.id.recyclerview_row_textview);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (itemClickListener != null) itemClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    public LockerAdapter(Context context) {
        this.context = context;
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public LockerAdapter.LockerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // create a new view
        View view = LayoutInflater.from(context).inflate(R.layout.recyclerview_row, parent, false);
        return new LockerViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)

    @Override
    public void onBindViewHolder(@NonNull LockerAdapter.LockerViewHolder viewHolder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        viewHolder.textView.setText(
                context.getResources().getString(
                        R.string.locker_list_item, lockerList.get(position).getNumber()));
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