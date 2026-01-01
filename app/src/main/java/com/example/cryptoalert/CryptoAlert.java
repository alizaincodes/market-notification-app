package com.example.cryptoalert;

public class CryptoAlert {
    public String symbol;
    public double targetPrice;
    public boolean isAbove;

    public CryptoAlert(String symbol, double targetPrice, boolean isAbove) {
        this.symbol = symbol;
        this.targetPrice = targetPrice;
        this.isAbove = isAbove;
    }
}