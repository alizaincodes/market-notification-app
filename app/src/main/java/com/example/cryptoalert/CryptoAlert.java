package com.example.cryptoalert;

public class CryptoAlert {
    public String symbol;
    public double targetPrice;
    public boolean isAbove;
    public String id;

    public CryptoAlert() {}

    public CryptoAlert(String symbol, double targetPrice, boolean isAbove) {
        this.symbol = symbol;
        this.targetPrice = targetPrice;
        this.isAbove = isAbove;
        this.id = java.util.UUID.randomUUID().toString();
    }
}