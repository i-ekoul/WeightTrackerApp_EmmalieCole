package edu.snhu.cs360.emmalie;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Project 3 requirement: Check credentials against DB. Allow create account.
 * Persist the userId in SharedPreferences and continue to the SMS screen.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String PREFS = "prefs";
    private static final String KEY_USER_ID = "user_id";

    private AppDatabaseHelper db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = new AppDatabaseHelper(this);

        EditText editUser = findViewById(R.id.nameText);
        EditText editPass = findViewById(R.id.editPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnCreate = findViewById(R.id.btnCreateAccount);

        btnLogin.setOnClickListener(v -> {
            String u = trim(editUser.getText());
            String p = trim(editPass.getText());
            if (TextUtils.isEmpty(u) || TextUtils.isEmpty(p)) {
                Toast.makeText(this, R.string.login_missing, Toast.LENGTH_SHORT).show();
                return;
            }
            long userId = db.checkLogin(u, p);
            if (userId > 0) {
                saveUserId(userId);
                startActivity(new Intent(this, SmsActivity.class));
                finish();
            } else {
                Toast.makeText(this, R.string.login_failed, Toast.LENGTH_SHORT).show();
            }
        });

        btnCreate.setOnClickListener(v -> {
            String u = trim(editUser.getText());
            String p = trim(editPass.getText());
            if (TextUtils.isEmpty(u) || TextUtils.isEmpty(p)) {
                Toast.makeText(this, R.string.login_missing, Toast.LENGTH_SHORT).show();
                return;
            }
            long newId = db.createUser(u, p);
            if (newId > 0) {
                saveUserId(newId);
                Toast.makeText(this, R.string.account_created, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, SmsActivity.class));
                finish();
            } else {
                Toast.makeText(this, R.string.account_exists, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static String trim(@Nullable CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }

    private void saveUserId(long id) {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        sp.edit().putLong(KEY_USER_ID, id).apply();
    }
}
