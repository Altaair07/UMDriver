package id.ac.umn.umndrive.uploadFile;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import id.ac.umn.umndrive.R;
import id.ac.umn.umndrive.Utils;
import id.ac.umn.umndrive.databinding.ActivityUploadFileBinding;
import id.ac.umn.umndrive.registerDone.RegisterDoneActivity;

public class UploadFileActivity extends AppCompatActivity {
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private final List<String> fileNames = new ArrayList<String>() {{
        add("vehicle");
        add("studentId");
        add("sim");
        add("stnk");
    }};
    private final HashMap<String, String> images = new HashMap<>();

    ActivityUploadFileBinding binding;
    Uri vehicleUri;
    Uri studentUri;
    Uri simUri;
    Uri stnkUri;

    ActivityResultLauncher<Intent> vehicleIntent = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() != null) {
                    vehicleUri = result.getData().getData();
                    binding.ibVehicle.setImageBitmap(handleImage(vehicleUri));
                }
            });
    ActivityResultLauncher<Intent> studentIntent = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() != null) {
                    studentUri = result.getData().getData();
                    binding.ibStudentCard.setImageBitmap(handleImage(studentUri));
                }
            });
    ActivityResultLauncher<Intent> simIntent = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() != null) {
                    simUri = result.getData().getData();
                    binding.ibSim.setImageBitmap(handleImage(simUri));
                }
            });
    ActivityResultLauncher<Intent> stnkIntent = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() != null) {
                    stnkUri = result.getData().getData();
                    binding.ibStnk.setImageBitmap(handleImage(stnkUri));
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityUploadFileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String type = getIntent().getStringExtra("data");
        HashMap<String, Object> userData = (HashMap<String, Object>) getIntent().getSerializableExtra("userData");
        binding.tvName.setText((String) userData.get("name"));
        binding.tvEmail.setText((String) userData.get("email"));

        if (Objects.equals(type, "motor")) {
            binding.ivIcon.setImageResource(R.drawable.ic_baseline_delivery_dining_24);
            binding.tvSim.setText("SIM C");
        }

        binding.ibVehicle.setOnClickListener(view -> launchImage("vehicle"));
        binding.ibStudentCard.setOnClickListener(view -> launchImage("student"));
        binding.ibSim.setOnClickListener(view -> launchImage("sim"));
        binding.ibStnk.setOnClickListener(view -> launchImage("stnk"));
        binding.btnSend.setOnClickListener(view -> {
            if (vehicleUri != null && studentUri != null && simUri != null && stnkUri != null) {
                storeImage(type);
            }
        });
    }

    private void launchImage(String type) {
        ActivityResultLauncher<Intent> i = vehicleIntent;

        if (Objects.equals(type, "student")) {
            i = studentIntent;
        } else if (Objects.equals(type, "sim")) {
            i = simIntent;
        } else if (Objects.equals(type, "stnk")) {
            i = stnkIntent;
        }

        ActivityResultLauncher<Intent> finalI = i;
        ImagePicker.with(this)
                .compress(1024)            //Final image size will be less than 1 MB(Optional)
                .maxResultSize(1080, 1080)    //Final image resolution will be less than 1080 x 1080(Optional)
                .createIntent(intent -> {
                    finalI.launch(intent);
                    return null;
                });
    }

    private void storeImage(String type) {
        onLoading(true);
        images.clear();

        String id = Utils.getStringFromPref(this, "id");
        storeImage(id, 0, type);
    }

    private void storeImage(String id, int i, String type) {
        List<Uri> uris = new ArrayList<Uri>() {{
            add(vehicleUri);
            add(studentUri);
            add(simUri);
            add(stnkUri);
        }};

        if (i < fileNames.size()) {
            storage.getReference(id).child(fileNames.get(i)).putFile(uris.get(i))
                    .addOnSuccessListener(taskSnapshot -> {
                        taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
                            images.put(fileNames.get(i) + " - " + type, uri.toString());
                            storeImage(id, i + 1, type);
                        });
                    }).addOnFailureListener(e -> {
                        onLoading(false);
                        Utils.showToast(UploadFileActivity.this, e.getMessage());
                    });
        } else {
            storeData(type);
        }
    }

    private void storeData(String type) {
        String id = Utils.getStringFromPref(this, "id");
        boolean isCar = type.equalsIgnoreCase("mobil");
        boolean isMotor = type.equalsIgnoreCase("motor");

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("drivers").document(id).set(new HashMap<String, Object>() {{
            put("userId", id);
            put("isCar", isCar);
            put("isMotor", isMotor);
            put("status", "pending");
            put("createdAt", new Date().getTime());
            putAll(images);
        }}).addOnSuccessListener(unused -> {
            onLoading(false);
            Intent intent = new Intent(UploadFileActivity.this, RegisterDoneActivity.class);
            startActivity(intent);
            finish();
        }).addOnFailureListener(e -> {
            onLoading(false);
            Utils.showToast(UploadFileActivity.this, e.getMessage());
        });
    }

    private void onLoading(boolean value) {
        if (value) {
            binding.loading.loadingScreen.setVisibility(View.VISIBLE);
        } else {
            binding.loading.loadingScreen.setVisibility(View.GONE);
        }
    }

    private Bitmap handleImage(Uri uri) {
        String path = uri.getPath();
        Matrix matrix = new Matrix();
        matrix.postRotate(Utils.getImageOrientation(path));

       try {
           Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
           Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                   bitmap.getHeight(), matrix, true);

           return rotatedBitmap;
       } catch (Exception e) {
           return null;
       }
    }
}