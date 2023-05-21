package id.ac.umn.umndrive.history;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import id.ac.umn.umndrive.databinding.ListItemHistoryBinding;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryHolder> {
    private final List<Map<String, Object>> datas = new ArrayList<Map<String, Object>>();
    private final String type;

    HistoryAdapter(String type) {
        this.type = type;
    }

    @NonNull
    @Override
    public HistoryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ListItemHistoryBinding binding = ListItemHistoryBinding.inflate(inflater, parent, false);
        return new HistoryHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryHolder holder, int position) {
        holder.bind(datas.get(position));
    }

    @Override
    public int getItemCount() {
        return datas.size();
    }


    void submitList(List<Map<String, Object>> datas) {
        this.datas.clear();
        this.datas.addAll(datas);
        notifyDataSetChanged();
    }

    class HistoryHolder extends RecyclerView.ViewHolder {
        private final ListItemHistoryBinding binding;

        public HistoryHolder(ListItemHistoryBinding binding) {
            super(binding.getRoot());

            this.binding = binding;
        }

        void bind(Map<String, Object> data) {
            String name = Objects.equals(type, "Drive") ? "Nama penumpang: " + data.get("custName") : "Nama driver: " + data.get("driverName");
            binding.tvName.setText(name);

            String nim = Objects.equals(type, "Drive") ? "NIM: " + data.get("custNim") : "NIM: " + data.get("driverNim");
            binding.tvNim.setText(nim);

            binding.tvAmount.setText("Rp" + data.get("total"));

            Date date = new Date();
            date.setTime((Long) data.get("orderDate"));

            SimpleDateFormat format = new SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault());
            binding.tvDate.setText(format.format(date));
        }
    }
}
