package id.ac.umn.umndrive;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    Button login;
    EditText username, password;
    TextView register;
    ProgressBar pb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        String id = Utils.getStringFromPref(this, "id");
        if (!id.isEmpty()) {
            moveToHome();
        }

        login = findViewById(R.id.login);
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        register = findViewById(R.id.register);
        pb = findViewById(R.id.pb_login);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (username.getText().toString().equals("") || password.getText().toString().equals("")) {
                    Toast.makeText(LoginActivity.this, "Username atau password kosong", Toast.LENGTH_SHORT).show();
                } else {
                    login();
                }
            }
        });

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        moveTaskToBack(true);
    }

    private void login() {
        onLoading(true);

        String email = username.getText().toString();
        String passwordStr = password.getText().toString();

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        Log.d("TAG", email + " " + passwordStr);
        firestore.collection("users")
                .whereEqualTo("email", email)
                .whereEqualTo("password", passwordStr)
                .get().addOnSuccessListener(queryDocumentSnapshots -> {
                    onLoading(false);

                    if (queryDocumentSnapshots.isEmpty()) {
                        Utils.showToast(LoginActivity.this, "Invalid email/password!");
                    } else {
                        DocumentChange doc = queryDocumentSnapshots.getDocumentChanges().get(0);
                        Map<String, Object> data = doc.getDocument().getData();
                        Utils.storeStringToPref(LoginActivity.this, "id", (String) data.get("id"));
                        Utils.storeStringToPref(LoginActivity.this, "name", (String) data.get("name"));
                        Utils.storeStringToPref(LoginActivity.this, "nim", (String) data.get("nim"));
                        Utils.storeStringToPref(LoginActivity.this, "imageUrl", (String) data.get("imageUrl"));

                        moveToHome();
                    }
                }).addOnFailureListener(e -> {
                    onLoading(false);
                    Utils.showToast(LoginActivity.this, e.getMessage());
                });
    }

    private void moveToHome() {
        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
    }

    private void onLoading(boolean value) {
        if (value) {
            login.setVisibility(View.GONE);
            pb.setVisibility(View.VISIBLE);
        } else {
            login.setVisibility(View.VISIBLE);
            pb.setVisibility(View.GONE);
        }
    }
}