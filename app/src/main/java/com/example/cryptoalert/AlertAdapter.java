package com.example.cryptoalert;

import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.ViewHolder> {
    private List<CryptoAlert> alerts;
    private OnDeleteListener listener;

    public interface OnDeleteListener { void onDelete(int position); }

    public AlertAdapter(List<CryptoAlert> alerts, OnDeleteListener listener) {
        this.alerts = alerts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alert, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CryptoAlert alert = alerts.get(position);
        holder.tvSymbol.setText(alert.symbol);
        String dir = alert.isAbove ? "Above" : "Below";
        holder.tvTarget.setText("Target: " + alert.targetPrice + " (" + dir + ")");
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(position));
    }

    @Override
    public int getItemCount() { return alerts.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSymbol, tvTarget;
        ImageButton btnDelete;
        public ViewHolder(View v) {
            super(v);
            tvSymbol = v.findViewById(R.id.tvSymbol);
            tvTarget = v.findViewById(R.id.tvTarget);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
}