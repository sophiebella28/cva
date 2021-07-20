package market;

public class Forward extends Derivative {
    AssetType assetType;
    Trader floating;
    Trader fixed;
    // todo: research whether forwards have interest rates

    public Forward(Trader fixed, Trader floating, long startTick, long endTick, double discountFactor, AssetType assetType) {
        super(startTick, endTick, discountFactor);
        this.fixed = fixed;
        this.floating = floating;
        this.assetType = assetType;
    }
}
