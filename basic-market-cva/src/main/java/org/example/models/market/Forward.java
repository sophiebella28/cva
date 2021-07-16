package org.example.models.market;

public class Forward extends Derivative {
    AssetType fixed;
    AssetType floating;
    // todo: research whether forwards have interest rates

    public Forward(AssetType fixed, AssetType floating, long startTick, long endTick, double discountFactor) {
        super(startTick, endTick, discountFactor);
        this.fixed = fixed;
        this.floating = floating;

    }
}
