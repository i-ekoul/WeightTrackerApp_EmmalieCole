package edu.snhu.cs360.emmalie;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

/**
 * Weights screen with goal + kg/lbs toggle.
 * Data is stored in the DB as kg; unit & goal are persisted in SharedPreferences per user.
 */
public class WeightsActivity extends AppCompatActivity {

    // prefs
    private static final String PREFS = "prefs";
    private static final String KEY_USER_ID = "user_id";

    // per-user keys
    private static String keyUnit(long userId) { return "unit_" + userId; }           // "kg" or "lbs"
    private static String keyGoal(long userId) { return "goal_kg_" + userId; }        // double (kg)

    private static String defaultUnit() {
        String c = Locale.getDefault().getCountry();
        return ("US".equalsIgnoreCase(c) || "LR".equalsIgnoreCase(c) || "MM".equalsIgnoreCase(c))
                ? "lbs" : "kg";
    }

    // goal compare always done in kg
    private double goalKg = Double.NaN;

    // optional SMS number (empty means disabled)
    private static final String ALERT_PHONE = "";

    private AppDatabaseHelper db;
    private WeightAdapter adapter;

    private long userId = -1L;

    private EditText editDate;
    private EditText editWeight;

    // goal UI
    private EditText editGoal;
    private TextView textGoalStatus;

    // unit UI
    private SwitchCompat switchUnit;

    // current unit selection ("kg" or "lbs")
    private String currentUnit;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weights);

        db = new AppDatabaseHelper(this);

        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        userId = sp.getLong(KEY_USER_ID, -1L);
        if (userId <= 0) {
            Toast.makeText(this, "No user session. Please log in again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // load prefs
        currentUnit = sp.getString(keyUnit(userId), defaultUnit());
        long goalBits = Double.doubleToRawLongBits(Double.NaN);
        goalKg = Double.longBitsToDouble(sp.getLong(keyGoal(userId), goalBits));

        // views
        editDate       = findViewById(R.id.editDate);
        editWeight     = findViewById(R.id.editWeight);
        editGoal       = findViewById(R.id.editGoal);
        Button btnSetGoal = findViewById(R.id.btnSetGoal);
        textGoalStatus = findViewById(R.id.textGoalStatus);
        switchUnit     = findViewById(R.id.switchUnit);

        // initialize switch label/state and listener
        updateUnitSwitchLabel();
        switchUnit.setOnCheckedChangeListener((buttonView, isChecked) -> {
            currentUnit = isChecked ? "kg" : "lbs";
            sp.edit().putString(keyUnit(userId), currentUnit).apply();
            applyUnitToAdapter();
            updateGoalStatus();
            updateUnitSwitchLabel();
        });

        // goal set button
        btnSetGoal.setOnClickListener(v -> {
            String s = safeText(editGoal);
            if (TextUtils.isEmpty(s)) {
                // clear goal
                goalKg = Double.NaN;
                sp.edit().remove(keyGoal(userId)).apply();
                updateGoalStatus();
                return;
            }
            try {
                double val = Double.parseDouble(s);
                goalKg = ("kg".equals(currentUnit)) ? val : lbsToKg(val);
                sp.edit().putLong(keyGoal(userId),
                        Double.doubleToRawLongBits(goalKg)).apply();
                updateGoalStatus();
                Toast.makeText(this, "Goal updated", Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Enter a numeric goal", Toast.LENGTH_SHORT).show();
            }
        });

        // RecyclerView
        RecyclerView recycler = findViewById(R.id.recyclerWeights);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        adapter = new WeightAdapter(new WeightAdapter.OnRowActionListener() {
            @Override
            public void onEdit(@NonNull AppDatabaseHelper.WeightEntry entry) {
                showEditDialog(entry);
            }

            @Override
            public void onDelete(@NonNull AppDatabaseHelper.WeightEntry entry) {
                int rows = db.deleteWeight(entry.id);
                if (rows > 0) {
                    Toast.makeText(WeightsActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                    refreshList();
                } else {
                    Toast.makeText(WeightsActivity.this, "Delete failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
        recycler.setAdapter(adapter);
        applyUnitToAdapter();

        // add button
        Button btnAdd = findViewById(R.id.btnAddWeight);
        btnAdd.setOnClickListener(v -> {
            String dateStr   = safeText(editDate);
            String weightStr = safeText(editWeight);

            if (TextUtils.isEmpty(dateStr) || TextUtils.isEmpty(weightStr)) {
                Toast.makeText(this, "Enter both date and weight", Toast.LENGTH_SHORT).show();
                return;
            }

            double inputVal;
            try {
                inputVal = Double.parseDouble(weightStr);
            } catch (NumberFormatException nfe) {
                Toast.makeText(this, "Weight must be a number", Toast.LENGTH_SHORT).show();
                return;
            }

            double kg = "kg".equals(currentUnit) ? inputVal : lbsToKg(inputVal);

            long rowId = db.insertWeight(userId, dateStr, kg);
            if (rowId > 0) {
                maybeSendGoalSms(kg);
                Toast.makeText(this, "Added", Toast.LENGTH_SHORT).show();
                editDate.setText("");
                editWeight.setText("");
                editDate.requestFocus();
                refreshList();
            } else {
                Toast.makeText(this, "Insert failed", Toast.LENGTH_SHORT).show();
            }
        });

        refreshList();
        updateGoalStatus();
    }

    // Set the switch label to show the active unit
    @SuppressLint("SetTextI18n")
    private void updateUnitSwitchLabel() {
        if (switchUnit == null) return;
        if ("kg".equals(currentUnit)) {
            switchUnit.setChecked(true);
            switchUnit.setText("kg");
        } else {
            switchUnit.setChecked(false);
            switchUnit.setText("lbs");
        }
    }

    private void refreshList() {
        List<AppDatabaseHelper.WeightEntry> rows = db.getWeights(userId);
        adapter.submitList(rows);
        updateGoalStatus();
    }

    private void showEditDialog(AppDatabaseHelper.WeightEntry entry) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, 0);

        final EditText dateInput = new EditText(this);
        dateInput.setHint("YYYY-MM-DD");
        dateInput.setInputType(InputType.TYPE_CLASS_TEXT);
        dateInput.setText(entry.date);
        container.addView(dateInput);

        final EditText weightInput = new EditText(this);
        weightInput.setHint("Weight");
        weightInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        // show in current unit
        double displayVal = "kg".equals(currentUnit) ? entry.weight : kgToLbs(entry.weight);
        weightInput.setText(String.format(Locale.US, "%.1f", displayVal));
        container.addView(weightInput);

        new AlertDialog.Builder(this)
                .setTitle("Edit Entry")
                .setView(container)
                .setPositiveButton("Save", (d, which) -> {
                    String newDate = safeText(dateInput);
                    String newWStr = safeText(weightInput);

                    if (TextUtils.isEmpty(newDate) || TextUtils.isEmpty(newWStr)) {
                        Toast.makeText(this, "Enter both date and weight", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double newInput;
                    try {
                        newInput = Double.parseDouble(newWStr);
                    } catch (NumberFormatException nfe) {
                        Toast.makeText(this, "Weight must be a number", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double newKg = "kg".equals(currentUnit) ? newInput : lbsToKg(newInput);

                    int rows = db.updateWeight(entry.id, newDate, newKg);
                    if (rows > 0) {
                        maybeSendGoalSms(newKg);
                        Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
                        refreshList();
                    } else {
                        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ---------------- SMS helper ----------------

    private boolean canSendSms() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void maybeSendGoalSms(double latestKg) {
        if (!canSendSms()) return;
        if (!Double.isNaN(goalKg) && latestKg <= goalKg) {
            try {
                SmsManager sms = getSystemService(SmsManager.class);

                String unit = currentUnit;
                double latest = "kg".equals(unit) ? latestKg : kgToLbs(latestKg);
                double goal   = "kg".equals(unit) ? goalKg : kgToLbs(goalKg);

                String msg = String.format(Locale.US,
                        "Goal reached! Latest: %.1f %s (goal: %.1f %s).",
                        latest, unit, goal, unit);

                if (!TextUtils.isEmpty(ALERT_PHONE)) {
                    sms.sendTextMessage(ALERT_PHONE, null, msg, null, null);
                    Toast.makeText(this, "Goal SMS sent", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "SMS failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ---------------- goal status ----------------

    private void updateGoalStatus() {
        if (textGoalStatus == null) return;

        if (Double.isNaN(goalKg)) {
            textGoalStatus.setText(getString(R.string.goal_status_none));
            return;
        }

        List<AppDatabaseHelper.WeightEntry> rows = db.getWeights(userId);
        if (rows.isEmpty()) {
            // just show goal value
            double g = "kg".equals(currentUnit) ? goalKg : kgToLbs(goalKg);
            textGoalStatus.setText(
                    String.format(Locale.US, getString(R.string.goal_status_away), g, 0.0, currentUnit));
            return;
        }

        double latestKg = rows.get(0).weight; // query orders DESC by date
        double diffKg = Math.max(0.0, latestKg - goalKg);

        if (diffKg <= 0.0001) {
            textGoalStatus.setText(getString(R.string.goal_status_reached));
        } else {
            double g   = "kg".equals(currentUnit) ? goalKg : kgToLbs(goalKg);
            double diff = "kg".equals(currentUnit) ? diffKg : kgToLbs(diffKg);
            textGoalStatus.setText(
                    String.format(Locale.US, getString(R.string.goal_status_away), g, diff, currentUnit));
        }
    }

    // ---------------- adapter unit formatter ----------------

    @SuppressLint("NotifyDataSetChanged")
    private void applyUnitToAdapter() {
        if (adapter == null) return;
        adapter.setUnitFormatter(kg ->
                "kg".equals(currentUnit)
                        ? String.format(Locale.US, "%.1f kg", kg)
                        : String.format(Locale.US, "%.1f lbs", kgToLbs(kg)));
        adapter.notifyDataSetChanged();
    }

    // ---------------- utils ----------------

    private static String safeText(EditText et) {
        CharSequence cs = et.getText();
        return cs == null ? "" : cs.toString().trim();
    }

    private static double lbsToKg(double lbs) { return lbs * 0.45359237d; }
    private static double kgToLbs(double kg)  { return kg / 0.45359237d; }
}
