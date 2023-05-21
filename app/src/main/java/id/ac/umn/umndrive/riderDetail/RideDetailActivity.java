package id.ac.umn.umndrive.riderDetail;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.util.HashMap;

import id.ac.umn.umndrive.HomeActivity;
import id.ac.umn.umndrive.databinding.ActivityRideDetailBinding;

public class RideDetailActivity extends AppCompatActivity {
    ActivityRideDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityRideDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        HashMap<String, Object> currentOrder =
                (HashMap<String, Object>) getIntent().getSerializableExtra("data");

        binding.tvName.setText((String) currentOrder.get("driverName"));
        binding.tvNim.setText((String) currentOrder.get("driverNim"));
        Glide.with(this).load(currentOrder.get("driverImage")).into(binding.ivAvatar);
        binding.tvTotal.setText("Rp " + currentOrder.get("total"));

        binding.btnHome.setOnClickListener(view -> {
            Intent i = new Intent(this, HomeActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        });
    }
}