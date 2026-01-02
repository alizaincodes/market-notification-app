package com.example.cryptoalert;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class CoinPickerActivity extends AppCompatActivity {
    public static final String EXTRA_SYMBOLS = "symbols";
    public static final String EXTRA_SELECTED = "selected_symbol";

    private List<String> symbols = new ArrayList<>();
    private SymbolAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coin_picker);

        RecyclerView rv = findViewById(R.id.rvSymbols);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SymbolAdapter(symbols, this::onSymbolSelected);
        rv.setAdapter(adapter);

        EditText search = findViewById(R.id.searchBox);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int co, int af) {}
            @Override public void onTextChanged(CharSequence s, int st, int be, int co) { adapter.filter(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        ArrayList<String> fromIntent = getIntent().getStringArrayListExtra(EXTRA_SYMBOLS);
        if (fromIntent != null) {
            symbols.clear();
            symbols.addAll(fromIntent);
            adapter.updateList(symbols);
        }
    }

    private void onSymbolSelected(String symbol) {
        Intent out = new Intent();
        out.putExtra(EXTRA_SELECTED, symbol);
        setResult(Activity.RESULT_OK, out);
        finish();
    }
}
