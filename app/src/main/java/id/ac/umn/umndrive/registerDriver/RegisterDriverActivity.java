package id.ac.umn.umndrive.registerDriver;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import id.ac.umn.umndrive.Utils;
import id.ac.umn.umndrive.databinding.ActivityRegisterDriverBinding;
import id.ac.umn.umndrive.uploadFile.UploadFileActivity;

public class RegisterDriverActivity extends AppCompatActivity {
    protected ActivityRegisterDriverBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterDriverBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getDriverStatus();

        binding.llMotor.setOnClickListener(view -> moveToUpload(true));
        binding.llCar.setOnClickListener(view -> moveToUpload(false));
    }

    void moveToUpload(boolean isMotor) {
        HashMap<String, Object> userData = (HashMap<String, Object>) getIntent().getSerializableExtra("userData");

        String type = "motor";

        if (!isMotor) {
            type = "mobil";
        }

        Intent i = new Intent(this, UploadFileActivity.class);
        i.putExtra("data", type);
        i.putExtra("userData", userData);
        startActivity(i);
    }

    private void getDriverStatus() {
        onLoading(true);

        String id = Utils.getStringFromPref(this, "id");
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("drivers").whereEqualTo("userId", id)
                .addSnapshotListener((value, error) -> {
                    onLoading(false);

                    if (error != null) {
                        Utils.showToast(RegisterDriverActivity.this, error.getMessage());
                    } else if (value != null) {
                        if (!value.isEmpty()) {
                            Map<String, Object> data =
                                    value.getDocumentChanges().get(0).getDocument().getData();

                            if ((boolean) data.get("isCar")) {
                                binding.llCar.setVisibility(View.GONE);
                            }

                            if ((boolean) data.get("isMotor")) {
                                binding.llMotor.setVisibility(View.GONE);
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