package com.example.cryptoalert;

import android.content.*;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private List<CryptoAlert> alertList = new ArrayList<>();
    private AlertAdapter adapter;
    private SharedPreferences sp;
    private Gson gson = new Gson();
    private static final int REQ_PICK_RINGTONE = 1234;
    private Uri selectedRingtoneUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    sp = getSharedPreferences("prefs", MODE_PRIVATE);
    loadData(); // Load existing alerts from storage
    // Fetch symbols at startup and populate search/autocomplete
    fetchSymbols();
    String uriStr = sp.getString("ringtone", null);
    if (uriStr != null) selectedRingtoneUri = Uri.parse(uriStr);

        // Setup RecyclerView
        RecyclerView rv = findViewById(R.id.rvAlerts);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AlertAdapter(alertList, position -> {
            alertList.remove(position);
            saveData();
            adapter.notifyItemRemoved(position);
        });
        rv.setAdapter(adapter);

        // Setup Search (Simple hardcoded list for example, 
        // but you can populate this via the Binance API fetch we discussed)
    AutoCompleteTextView search = findViewById(R.id.searchCoin);
    // initial adapter empty; will be populated when symbols are fetched
    ArrayAdapter<String> searchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
    search.setAdapter(searchAdapter);

        // Setup Add Button
        findViewById(R.id.btnAdd).setOnClickListener(v -> {
            // Open coin picker activity
            ArrayList<String> symbolsList = new ArrayList<>(symbols);
            if (symbolsList.isEmpty()) {
                Toast.makeText(this, "Loading coin list, please wait...", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent it = new Intent(this, CoinPickerActivity.class);
            it.putStringArrayListExtra(CoinPickerActivity.EXTRA_SYMBOLS, symbolsList);
            startActivityForResult(it, 4321);
        });

        // Start the background engine
        Intent intent = new Intent(this, MonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void showAddDialog(String coin) {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_alert, null);
        EditText etPrice = view.findViewById(R.id.etPrice);
        RadioButton rbAbove = view.findViewById(R.id.rbAbove);
        Button btnPickRingtone = new Button(this);
        btnPickRingtone.setText("Pick Ringtone");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(view);
        layout.addView(btnPickRingtone);

        btnPickRingtone.setOnClickListener(v -> {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
            Uri existing = selectedRingtoneUri;
            if (existing != null) intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing);
            startActivityForResult(intent, REQ_PICK_RINGTONE);
        });

        new AlertDialog.Builder(this)
                .setTitle("New Alert: " + coin)
                .setView(layout)
                .setPositiveButton("Set Alert", (d, w) -> {
                    String priceStr = etPrice.getText().toString();
                    if (!priceStr.isEmpty()) {
                        double price = Double.parseDouble(priceStr);
                        boolean isAbove = rbAbove.isChecked();
                        CryptoAlert alert = new CryptoAlert(coin, price, isAbove);
                        alertList.add(alert);
                        saveData();
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Please enter a target price", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveData() {
        String json = gson.toJson(alertList);
        sp.edit().putString("alerts", json).apply();
        if (selectedRingtoneUri != null) sp.edit().putString("ringtone", selectedRingtoneUri.toString()).apply();
        
        // Restart service to pick up new alert list
        Intent intent = new Intent(this, MonitorService.class);
        startService(intent);
    }

    private void loadData() {
        String json = sp.getString("alerts", "[]");
        alertList = gson.fromJson(json, new TypeToken<List<CryptoAlert>>(){}.getType());
        if (alertList == null) alertList = new ArrayList<>();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_RINGTONE && resultCode == RESULT_OK) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                selectedRingtoneUri = uri;
                Toast.makeText(this, "Ringtone selected", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (requestCode == 4321 && resultCode == RESULT_OK) {
            String sym = data.getStringExtra(CoinPickerActivity.EXTRA_SELECTED);
            if (sym != null) showAddDialog(sym);
            return;
        }
    }

    private final List<String> symbols = new ArrayList<>();

    private void fetchSymbols() {
        new Thread(() -> {
            try {
                okhttp3.OkHttpClient c = new okhttp3.OkHttpClient();
                okhttp3.Request req = new okhttp3.Request.Builder().url("https://api.binance.com/api/v3/exchangeInfo").build();
                okhttp3.Response resp = c.newCall(req).execute();
                if (!resp.isSuccessful()) throw new RuntimeException("HTTP " + resp.code());
                String body = resp.body().string();
                org.json.JSONObject json = new org.json.JSONObject(body);
                org.json.JSONArray arr = json.optJSONArray("symbols");
                List<String> list = new ArrayList<>();
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        org.json.JSONObject s = arr.optJSONObject(i);
                        if (s == null) continue;
                        String sym = s.optString("symbol", null);
                        if (sym != null && !sym.isEmpty()) list.add(sym);
                    }
                }
                runOnUiThread(() -> {
                    symbols.clear(); symbols.addAll(list);
                    AutoCompleteTextView search = findViewById(R.id.searchCoin);
                    ArrayAdapter<String> a = (ArrayAdapter<String>) search.getAdapter();
                    a.clear(); a.addAll(symbols);
                    a.notifyDataSetChanged();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to load coin list", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void fetchCoinListAndShowDialog(String prefill) {
        // Fetch exchange info from Binance to get symbols
        new Thread(() -> {
            try {
                okhttp3.Request req = new okhttp3.Request.Builder().url("https://api.binance.com/api/v3/exchangeInfo").build();
                okhttp3.Response resp = new okhttp3.OkHttpClient().newCall(req).execute();
                String body = resp.body().string();
                org.json.JSONObject json = new org.json.JSONObject(body);
                org.json.JSONArray arr = json.getJSONArray("symbols");
                List<String> symbols = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    org.json.JSONObject s = arr.getJSONObject(i);
                    if (s.getBoolean("isSpot") || s.optBoolean("isSpot", false)) { // include spot by default
                        symbols.add(s.getString("symbol"));
                    } else {
                        symbols.add(s.getString("symbol"));
                    }
                }
                runOnUiThread(() -> showPickerDialog(symbols, prefill));
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to load coin list", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void showPickerDialog(List<String> symbols, String prefill) {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_alert, null);
        AutoCompleteTextView act = new AutoCompleteTextView(this);
        ArrayAdapter<String> adapterSymbols = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, symbols);
        act.setAdapter(adapterSymbols);
        if (prefill != null && !prefill.isEmpty()) act.setText(prefill);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(act);
        layout.addView(view);

        new AlertDialog.Builder(this)
                .setTitle("Choose coin and target")
                .setView(layout)
                .setPositiveButton("Next", (d, w) -> {
                    String coin = act.getText().toString().toUpperCase().trim();
                    if (!coin.isEmpty()) showAddDialog(coin);
                    else Toast.makeText(this, "Please select a coin", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}