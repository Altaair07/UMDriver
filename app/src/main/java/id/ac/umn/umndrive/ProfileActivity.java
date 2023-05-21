package id.ac.umn.umndrive;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import id.ac.umn.umndrive.databinding.ActivityProfileBinding;
import id.ac.umn.umndrive.editProfile.EditProfileActivity;
import id.ac.umn.umndrive.registerDriver.RegisterDriverActivity;

public class ProfileActivity extends AppCompatActivity {
    private ActivityProfileBinding binding;
    private HashMap<String, Object> userData;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getUserData();

        binding.logout.setOnClickListener(v -> {
            Utils.clearPref(this);

            Intent i = new Intent(ProfileActivity.this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        });

        binding.setting.setOnClickListener(view -> {
            Intent i = new Intent(this, EditProfileActivity.class);
            startActivity(i);
        });
        binding.registerDriver.setOnClickListener(view -> {
            Intent i = new Intent(this, RegisterDriverActivity.class);
            i.putExtra("userData", userData);
            startActivity(i);
        });
    }

    private void getUserData() {
        onLoading(true);

        String id = Utils.getStringFromPref(this, "id");
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("users").document(id).addSnapshotListener((value, error) -> {
            if (error != null) {
                onLoading(false);
                Utils.showToast(ProfileActivity.this, error.getMessage());
            } else if (value != null) {
                userData = (HashMap<String, Object>) value.getData();
                binding.username.setText((String) value.get("name"));
                binding.email.setText((String) value.get("email"));
                binding.phone.setText((String) value.get("phone"));

                if (value.get("imageUrl") != null) {
                    Glide.with(ProfileActivity.this)
                            .load(value.get("imageUrl")).into(binding.ivAvatar);
                }

                getDriverStatus(id);
            }
        });
    }

    private void getDriverStatus(String id) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("drivers").whereEqualTo("userId", id)
                .addSnapshotListener((value, error) -> {
                    onLoading(false);

                    if (error != null) {
                        Utils.showToast(ProfileActivity.this, error.getMessage());
                    } else if (value != null) {
                        if (!value.isEmpty()) {
                            Map<String, Object> data =
                                    value.getDocumentChanges().get(0).getDocument().getData();

                            if ((boolean) data.get("isCar") && (boolean) data.get("isMotor")) {
                                binding.registerDriver.setVisibility(View.GONE);
                            }
                        }
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