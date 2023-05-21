package id.ac.umn.umndrive.history;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import id.ac.umn.umndrive.Utils;
import id.ac.umn.umndrive.databinding.ActivityHistoryBinding;

public class HistoryActivity extends AppCompatActivity {
    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        id.ac.umn.umndrive.databinding.ActivityHistoryBinding binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String type = getIntent().getStringExtra("type");
        adapter = new HistoryAdapter(type);

        binding.rv.setAdapter(adapter);
        binding.rv.setLayoutManager(new LinearLayoutManager(this));

        getHistory();
    }

    private void getHistory() {
        String field = Objects.equals(getIntent().getStringExtra("type"), "Ride") ? "cust" : "driver";
        String id = Utils.getStringFromPref(this, "id");

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("histories").whereEqualTo(field + "Id", id).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Map<String, Object>> datas = new ArrayList<>();

                    for (DocumentChange doc :
                            queryDocumentSnapshots.getDocumentChanges()) {
                        Map<String, Object> data = doc.getDocument().getData();
                        datas.add(data);
                    }

                    adapter.submitList(datas);
                })
                .addOnFailureListener(e -> Utils.showToast(HistoryActivity.this, e.getMessage()));
    }
}