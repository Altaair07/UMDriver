package id.ac.umn.umndrive;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.util.HashMap;

import id.ac.umn.umndrive.databinding.ActivityRegisterBinding;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private Uri imageUri;

    ActivityResultLauncher<Intent> startActivityIntent = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() != null) {
                    imageUri = result.getData().getData();
                    binding.ivAvatar.setImageURI(imageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.ivAvatar.setOnClickListener(view -> ImagePicker.with(this)
                .compress(1024)            //Final image size will be less than 1 MB(Optional)
                .maxResultSize(1080, 1080)    //Final image resolution will be less than 1080 x 1080(Optional)
                .createIntent(intent -> {
                    startActivityIntent.launch(intent);
                    return null;
                }));

        binding.btnRegister.setOnClickListener(view -> {
            if (imageUri == null || binding.etName.getText().toString().isEmpty() ||
                    binding.etEmail.getText().toString().isEmpty() ||
                    binding.etNim.getText().toString().isEmpty() ||
                    binding.etPassword.getText().toString().isEmpty()) {
                Utils.showToast(RegisterActivity.this, "Please fill all field!");
            } else if (!binding.etPassword.getText().toString()
                    .equals(binding.etPassword.getText().toString())) {
                Utils.showToast(RegisterActivity.this, "Password missmatch!");
            } else {
                register();
            }
        });
    }

    private void register() {
        onLoading(true);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        String email = binding.etEmail.getText().toString();
        String password = binding.etPassword.getText().toString();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (authResult.getUser() != null) {
                        FirebaseUser user = authResult.getUser();
                        storeData(user.getUid());
                    }
                }).addOnFailureListener(e -> {
                    onLoading(false);
                    Utils.showToast(RegisterActivity.this, e.getMessage());
                });
    }

    private void storeData(String id) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storage.getReference(id).child("pp").putFile(imageUri).addOnSuccessListener(taskSnapshot ->
                taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    storeData(id, imageUrl);
                })).addOnFailureListener(e -> {
            onLoading(false);
            Utils.showToast(RegisterActivity.this, e.getMessage());
        });
    }

    private void storeData(String id, String imageUrl) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        HashMap<String, String> data = new HashMap<String, String>() {{
            put("id", id);
            put("name", binding.etName.getText().toString());
            put("phone", binding.etPhone.getText().toString());
            put("email", binding.etEmail.getText().toString());
            put("password", binding.etPassword.getText().toString());
            put("nim", binding.etNim.getText().toString());
            put("imageUrl", imageUrl);
        }};
        firestore.collection("users").document(id).set(data).addOnSuccessListener(unused -> {
            Utils.showToast(RegisterActivity.this, "Registrasi berhasil, silahkan login!");
            finish();
        }).addOnFailureListener(e -> {
            onLoading(false);
            Utils.showToast(RegisterActivity.this, e.getMessage());
        });
    }

    private void onLoading(boolean value) {
        if (value) {
            binding.btnRegister.setVisibility(View.GONE);
            binding.pbRegister.setVisibility(View.VISIBLE);
        } else {
            binding.btnRegister.setVisibility(View.VISIBLE);
            binding.pbRegister.setVisibility(View.GONE);
        }
    }
}