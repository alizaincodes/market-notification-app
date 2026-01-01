package com.example.cryptoalert;

import android.content.*;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sp = getSharedPreferences("prefs", MODE_PRIVATE);
        loadData(); // Load existing alerts from storage

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
        String[] commonCoins = {"BTCUSDT", "ETHUSDT", "BNBUSDT", "SOLUSDT", "XRPUSDT"};
        ArrayAdapter<String> searchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, commonCoins);
        search.setAdapter(searchAdapter);

        // Setup Add Button
        findViewById(R.id.btnAdd).setOnClickListener(v -> {
            String coin = search.getText().toString().toUpperCase().trim();
            if (!coin.isEmpty()) {
                showAddDialog(coin);
            } else {
                Toast.makeText(this, "Please enter or select a coin", Toast.LENGTH_SHORT).show();
            }
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

        new AlertDialog.Builder(this)
                .setTitle("New Alert: " + coin)
                .setView(view)
                .setPositiveButton("Set Alert", (d, w) -> {
                    String priceStr = etPrice.getText().toString();
                    if (!priceStr.isEmpty()) {
                        double price = Double.parseDouble(priceStr);
                        boolean isAbove = rbAbove.isChecked();
                        
                        alertList.add(new CryptoAlert(coin, price, isAbove));
                        saveData();
                        adapter.notifyDataSetChanged();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveData() {
        String json = gson.toJson(alertList);
        sp.edit().putString("alerts", json).apply();
        
        // Restart service to pick up new alert list
        Intent intent = new Intent(this, MonitorService.class);
        startService(intent);
    }

    private void loadData() {
        String json = sp.getString("alerts", "[]");
        alertList = gson.fromJson(json, new TypeToken<List<CryptoAlert>>(){}.getType());
        if (alertList == null) alertList = new ArrayList<>();
    }
}