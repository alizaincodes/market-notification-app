package com.example.cryptoalert;

import android.app.*;
import android.content.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
import androidx.core.app.NotificationCompat;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class MonitorService extends Service {
    private final OkHttpClient client = new OkHttpClient();
    private ScheduledExecutorService scheduler;
    private MediaPlayer mediaPlayer;
    private List<CryptoAlert> alerts = new ArrayList<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP_RINGING".equals(intent.getAction())) {
            stopRinging();
        }
        loadAlerts();
        startForeground(1, createNotification("Monitoring Crypto Markets..."));
        scheduleTask();
        return START_STICKY;
    }

    private void scheduleTask() {
        if (scheduler != null) scheduler.shutdown();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::checkPrices, 0, 1, TimeUnit.MINUTES);
    }

    private void checkPrices() {
        for (CryptoAlert alert : alerts) {
            Request request = new Request.Builder()
                .url("https://api.binance.com/api/v3/ticker/price?symbol=" + alert.symbol)
                .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        double currentPrice = json.getDouble("price");
                        boolean triggered = alert.isAbove ? (currentPrice >= alert.targetPrice) : (currentPrice <= alert.targetPrice);
                        
                        if (triggered) {
                            new Handler(Looper.getMainLooper()).post(() -> startAlarm(alert.symbol, currentPrice));
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
                @Override
public void onFailure(Call call, IOException e) {
    // This tells the user something is wrong without crashing the app
    new Handler(Looper.getMainLooper()).post(() -> 
        Toast.makeText(getApplicationContext(), "Network Error: Checking Binance failed.", Toast.LENGTH_SHORT).show()
    );
}
            });
        }
    }

    private void startAlarm(String symbol, double price) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) return;
        
        Uri ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        mediaPlayer = MediaPlayer.create(this, ringtone);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        showTriggerNotification(symbol, price);
    }

    private void showTriggerNotification(String symbol, double price) {
        Intent intent = new Intent(this, MonitorService.class).setAction("STOP_RINGING");
        PendingIntent pi = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Notification n = new NotificationCompat.Builder(this, "ALERT_CHAN")
            .setContentTitle("PRICE ALERT: " + symbol)
            .setContentText("Price hit: " + price + ". Click to stop alarm.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setFullScreenIntent(pi, true)
            .setOngoing(true)
            .setContentIntent(pi)
            .build();

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(2, n);
    }

    private void stopRinging() {
        if (mediaPlayer != null) { mediaPlayer.stop(); mediaPlayer.release(); mediaPlayer = null; }
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(2);
    }

    private void loadAlerts() {
        SharedPreferences sp = getSharedPreferences("prefs", MODE_PRIVATE);
        String json = sp.getString("alerts", "[]");
        alerts = new Gson().fromJson(json, new TypeToken<List<CryptoAlert>>(){}.getType());
    }

    private Notification createNotification(String msg) {
        NotificationChannel chan = new NotificationChannel("MONITOR_CHAN", "Monitor", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(chan);
        return new NotificationCompat.Builder(this, "MONITOR_CHAN")
            .setContentTitle("Crypto Sentinel")
            .setContentText(msg)
            .setSmallIcon(android.R.drawable.ic_menu_compass).build();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}