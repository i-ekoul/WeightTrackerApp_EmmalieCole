package edu.snhu.cs360.emmalie;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class SmsActivity extends AppCompatActivity {

    // Keep as fields (used inside the permission callback)
    private TextView textSmsStatus;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    textSmsStatus.setText(getString(R.string.sms_status_granted));
                    Toast.makeText(this, getString(R.string.sms_toast_granted), Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, WeightsActivity.class));
                    finish();
                } else {
                    textSmsStatus.setText(getString(R.string.sms_status_denied));
                    Toast.makeText(this, getString(R.string.sms_toast_denied), Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms);

        textSmsStatus = findViewById(R.id.textSmsStatus);
        Button btnRequestSms = findViewById(R.id.btnRequestSms);

        updateStatusText();

        btnRequestSms.setOnClickListener(v -> {
            if (!hasTelephonyHardware()) {
                Toast.makeText(this, getString(R.string.sms_no_telephony), Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, WeightsActivity.class));
                finish();
                return;
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                    == PackageManager.PERMISSION_GRANTED) {
                textSmsStatus.setText(getString(R.string.sms_status_granted));
                startActivity(new Intent(this, WeightsActivity.class));
                finish();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.SEND_SMS);
            }
        });
    }

    private void updateStatusText() {
        boolean granted = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
        textSmsStatus.setText(granted
                ? getString(R.string.sms_status_granted)
                : getString(R.string.sms_status_denied));
    }

    private boolean hasTelephonyHardware() {
        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        return tm != null && tm.getPhoneType() != TelephonyManager.PHONE_TYPE_NONE;
    }
}
