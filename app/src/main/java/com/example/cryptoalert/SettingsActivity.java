package com.example.cryptoalert;

import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    private SharedPreferences sp;
    private static final int REQ_RING = 2222;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        sp = getSharedPreferences("prefs", MODE_PRIVATE);

        EditText etInterval = findViewById(R.id.etInterval);
        int interval = sp.getInt("poll_interval_sec", 60);
        etInterval.setText(String.valueOf(interval));

        Button btnPick = findViewById(R.id.btnPickRingtone);
        btnPick.setOnClickListener(v -> {
            startActivityForResult(new android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER), REQ_RING);
        });

        findViewById(R.id.btnSaveSettings).setOnClickListener(v -> {
            String s = etInterval.getText().toString();
            try {
                int val = Integer.parseInt(s);
                sp.edit().putInt("poll_interval_sec", val).apply();
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
                finish();
            } catch (Exception e) { Toast.makeText(this, "Invalid interval", Toast.LENGTH_SHORT).show(); }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @androidx.annotation.Nullable android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_RING && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                sp.edit().putString("ringtone", uri.toString()).apply();
                Toast.makeText(this, "Ringtone saved", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
