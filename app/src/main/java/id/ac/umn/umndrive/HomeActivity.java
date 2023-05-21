package id.ac.umn.umndrive;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import id.ac.umn.umndrive.databinding.ActivityHomeBinding;
import id.ac.umn.umndrive.drive.DriveActivity;
import id.ac.umn.umndrive.history.HistoryActivity;
import id.ac.umn.umndrive.ride.RideActivity;

public class    HomeActivity extends AppCompatActivity {
    LinearLayout profile;
    TextView username;
    private ActivityHomeBinding binding;
    private boolean isDriver = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        profile = findViewById(R.id.profile);
        username = findViewById(R.id.username);

        profile.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ProfileActivity.class)
                .putExtra("username", username.getText().toString())));
        binding.drive.setOnClickListener(view -> {
            if (isDriver) {
                Intent i = new Intent(this, DriveActivity.class);
                startActivity(i);
            } else {
                Utils.showToast(HomeActivity.this, "You are not a driver!");
            }
        });
        binding.ride.setOnClickListener(view -> {
            Intent i = new Intent(this, RideActivity.class);
            startActivity(i);
        });
        binding.tvRideViewall.setOnClickListener(view -> {
            Intent i = new Intent(this, HistoryActivity.class);
            i.putExtra("type", "Ride");
            startActivity(i);
        });
        binding.tvDriveViewall.setOnClickListener(view -> {
            if (isDriver) {
                Intent i = new Intent(this, HistoryActivity.class);
                i.putExtra("type", "Drive");
                startActivity(i);
            } else {
                Utils.showToast(HomeActivity.this, "You are not a driver!");
            }
        });

        getUserData();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        moveTaskToBack(true);
    }

    private void getUserData() {
        onLoading(true);

        String id = Utils.getStringFromPref(this, "id");

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("users").document(id).get().addOnSuccessListener(documentSnapshot -> {
            onLoading(false);

            if (documentSnapshot.getData() == null) {
                return;
            }

            if (documentSnapshot.getData().get("name") != null) {
                String username = (String) documentSnapshot.getData().get("name");
                this.username.setText("Hello, " + username);
            }

            if (documentSnapshot.get("lastDrive") != null) {
                Long lastDrive = (Long) documentSnapshot.getData().get("lastDrive");
                Date date = new Date();
                date.setTime(lastDrive);

                SimpleDateFormat format = new SimpleDateFormat("dd MMMM yyy, HH:mm", Locale.getDefault());
                binding.tvDriveHistoryMsg.setText(format.format(date));
            }

            if (documentSnapshot.get("lastRide") != null) {
                Long lastRide = (Long) documentSnapshot.getData().get("lastRide");
                Date date = new Date();
                date.setTime(lastRide);

                SimpleDateFormat format = new SimpleDateFormat("dd MMMM yyy, HH:mm", Locale.getDefault());
                binding.tvRideHistoryMsg.setText(format.format(date));
            }

        }).addOnFailureListener(e -> {
            onLoading(false);
            Utils.showToast(HomeActivity.this, e.getMessage());
        });

        firestore.collection("drivers").document(id)
                .addSnapshotListener((value, error) -> {
                    if (value.getData() != null) {
                        Map<String, Object> data = value.getData();
                        if ((boolean) data.get("isCar") || (boolean) data.get("isMotor")) {
                            isDriver = true;
                        }
                    }

                    if (error != null) {
                        Utils.showToast(HomeActivity.this, error.getMessage());
                    }
                });
    }

    private void onLoading(boolean value) {
        if (value) {
            binding.loading.loadingScreen.setVisibility(View.VISIBLE);
        } else {
            binding.loading.loadingScreen.setVisibility(View.GONE);
        }
    }
}