package com.example.cryptoalert;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class SymbolAdapter extends RecyclerView.Adapter<SymbolAdapter.H> {
    private final List<String> data = new ArrayList<>();
    private final List<String> all = new ArrayList<>();
    private final OnSelect cb;

    interface OnSelect { void onSelect(String symbol); }

    public SymbolAdapter(List<String> initial, OnSelect cb) {
        this.cb = cb;
        updateList(initial);
    }

    public void updateList(List<String> list) {
        all.clear(); all.addAll(list);
        filter("");
    }

    public void filter(String q) {
        data.clear();
        String qq = q == null ? "" : q.trim().toUpperCase();
        if (qq.isEmpty()) data.addAll(all);
        else {
            for (String s : all) if (s.toUpperCase().contains(qq)) data.add(s);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public H onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new H(v);
    }

    @Override
    public void onBindViewHolder(@NonNull H holder, int position) {
        String s = data.get(position);
        holder.tv.setText(s);
        holder.itemView.setOnClickListener(v -> cb.onSelect(s));
    }

    @Override public int getItemCount() { return data.size(); }

    static class H extends RecyclerView.ViewHolder {
        TextView tv;
        H(@NonNull View v) { super(v); tv = v.findViewById(android.R.id.text1); }
    }
}
