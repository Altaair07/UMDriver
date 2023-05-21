package id.ac.umn.umndrive.driveDetail;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;

import id.ac.umn.umndrive.databinding.ActivityDriveDetailBinding;

public class DriveDetailActivity extends AppCompatActivity {
    private ActivityDriveDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDriveDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        HashMap<String, Object> currentOrder =
                (HashMap<String, Object>) getIntent().getSerializableExtra("data");

        binding.tvName.setText((String) currentOrder.get("custName"));
        binding.tvAddress.setText((String) currentOrder.get("destination"));
        binding.tvNim.setText((String) currentOrder.get("custNim"));
        binding.tvTotal.setText("Rp " + currentOrder.get("total"));

        binding.btnHome.setOnClickListener(view -> finish());
    }
}