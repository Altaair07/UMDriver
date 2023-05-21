package id.ac.umn.umndrive.editProfile;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import id.ac.umn.umndrive.Utils;
import id.ac.umn.umndrive.databinding.ActivityEditProfileBinding;

public class EditProfileActivity extends AppCompatActivity {
    private ActivityEditProfileBinding binding;
    private Bitmap imageBitmap;

    ActivityResultLauncher<Intent> startActivityIntent = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    imageBitmap = handleImage(imageUri);
                    binding.ivAvatar.setImageBitmap(imageBitmap);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getUserData();

        binding.ivAvatar.setOnClickListener(view -> ImagePicker.with(this)
                .compress(1024)            //Final image size will be less than 1 MB(Optional)
                .maxResultSize(1080, 1080)    //Final image resolution will be less than 1080 x 1080(Optional)
                .createIntent(intent -> {
                    startActivityIntent.launch(intent);
                    return null;
                }));

        binding.update.setOnClickListener(view -> {
            String username = binding.username.getText().toString();
            String email = binding.email.getText().toString();
            String phone = binding.phone.getText().toString();
//            String password = binding.password.getText().toString();

            if (username.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Fill all fields!", Toast.LENGTH_SHORT).show();
            } else {
                HashMap<String, Object> map = new HashMap<>();
                map.put("name", username);
                map.put("email", email);
                map.put("phone", phone);
//                map.put("password", password);

                if (imageBitmap != null) {
                    changeImage(map);
                } else {
                    storeData(map, null);
                }
            }
        });
    }

    private void getUserData() {
        String id = Utils.getStringFromPref(this, "id");
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("users").document(id).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Map<String, Object> data = documentSnapshot.getData();

                    if (data != null) {
                        binding.username.setText((String) data.get("name"));
                        binding.email.setText((String) data.get("email"));
                        binding.phone.setText((String) data.get("phone"));
//                       binding.password.setText((String) data.get("password"));

                        if (data.get("imageUrl") != null) {
                            Glide.with(EditProfileActivity.this).load(data.get("imageUrl"))
                                    .into(binding.ivAvatar);
                        }
                    }
                })
                .addOnFailureListener(e -> Utils.showToast(EditProfileActivity.this, e.getMessage()));
    }

    private void changeImage(Map<String, Object> userData) {
        onLoading(true);

        String id = Utils.getStringFromPref(this, "id");
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storage.getReference(id).child("pp").putFile(getImageUri(this, imageBitmap))
                .addOnSuccessListener(taskSnapshot -> {
                    onLoading(false);

                    taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            storeData(userData, uri.toString());
                        }
                    });

                })
                .addOnFailureListener(e -> {
                    onLoading(false);
                    Utils.showToast(EditProfileActivity.this, e.getMessage());
                });
    }

    private void storeData(Map<String, Object> userData, String imageUrl) {
        onLoading(true);

        if (imageUrl != null) {
            userData.put("imageUrl", imageUrl);
        }

        String id = Utils.getStringFromPref(this, "id");
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("users").document(id).update(userData)
                .addOnSuccessListener(unused -> {
                    onLoading(false);
                    Utils.showToast(EditProfileActivity.this, "Profile updated!");
                    finish();
                })
                .addOnFailureListener(e -> {
                    onLoading(false);
                    Utils.showToast(EditProfileActivity.this, e.getMessage());
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

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }
}