package id.ac.umn.umndrive.verify;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Timer;
import java.util.TimerTask;

import id.ac.umn.umndrive.databinding.ActivityVerifyBinding;
import in.aabhasjindal.otptextview.OTPListener;

public class VerifyActivity extends AppCompatActivity {
    private ActivityVerifyBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityVerifyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.otpTv.setOtpListener(new OTPListener() {
            @Override
            public void onInteractionListener() {
            }

            @Override
            public void onOTPComplete(String otp) {
                finish();
                Toast.makeText(VerifyActivity.this, "Registrasi berhasil!", Toast.LENGTH_SHORT).show();
            }
        });

        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            int i = 60;

            @SuppressLint("SetTextI18n")
            public void run() {
                runOnUiThread(() -> {
                    if (i >= 0) {
                        binding.tvImer.setText("(00:" + i-- + ")");
                    } else {
                        binding.tvImer.setVisibility(View.GONE);
                        binding.btnResend.setVisibility(View.VISIBLE);
                    }
                });
            }
        };
        timer.scheduleAtFixedRate(task, 0, 1000);
    }
}