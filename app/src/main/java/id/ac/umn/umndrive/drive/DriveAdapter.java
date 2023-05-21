package id.ac.umn.umndrive.drive;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import id.ac.umn.umndrive.databinding.ListItemCustomersBinding;

interface OnItemClick {
    void onClick(String id, LatLng latLng, HashMap<String, Object> data);
}

public class DriveAdapter extends RecyclerView.Adapter<DriveAdapter.DriveHolder> {
    final List<HashMap<String, Object>> orders = new ArrayList<>();
    private int selectedPosition = 0;
    private OnItemClick onItemClick;

    DriveAdapter(OnItemClick onItemClick) {
        this.onItemClick = onItemClick;
    }

    @NonNull
    @Override
    public DriveHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ListItemCustomersBinding binding = ListItemCustomersBinding.inflate(inflater, parent, false);
        return new DriveHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DriveHolder holder, int position) {
        holder.itemView.setBackgroundColor(selectedPosition == position ? Color.GREEN : Color.TRANSPARENT);
        holder.bind(orders.get(position));
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    void setOrders(List<HashMap<String, Object>> orders) {
        this.orders.clear();
        this.orders.addAll(orders);
        notifyDataSetChanged();
    }

    void clearData() {
        orders.clear();
        notifyDataSetChanged();
    }

    class DriveHolder extends RecyclerView.ViewHolder {
        ListItemCustomersBinding binding;

        public DriveHolder(ListItemCustomersBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(HashMap<String, Object> data) {
            binding.tvName.setText((String) data.get("custName"));
            binding.tvNim.setText((String) data.get("custNim"));
            binding.tvAddress.setText((String) data.get("custDropoff"));

            itemView.setOnClickListener(view -> {
                if (getBindingAdapterPosition() == RecyclerView.NO_POSITION) return;

                notifyItemChanged(selectedPosition);
                selectedPosition = getBindingAdapterPosition();
                notifyItemChanged(selectedPosition);

                LatLng latLng = new LatLng(
                        (double) data.get("lat"),
                        (double) data.get("lng")
                );
                onItemClick.onClick((String) data.get("custId"), latLng, data);
            });
        }
    }
}
