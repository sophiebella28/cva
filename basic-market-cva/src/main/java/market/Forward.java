package market;

public class Forward extends Derivative {
    AssetType assetType;
    Trader floating;
    Trader fixed;
    int amountOfAsset = 0 ;
    double agreedValue = 0;
    // todo: research whether forwards have interest rates

    public Forward(Trader fixed, Trader floating, long startTick, long endTick, double discountFactor, AssetType assetType, int amountOfAsset) {
        super(startTick, endTick, discountFactor);
        this.fixed = fixed;
        this.floating = floating;
        this.assetType = assetType;
        this.amountOfAsset = amountOfAsset;
        calculateStartingValue();
    }

    @Override
    protected void calculateStartingValue() {
        agreedValue = assetType.getPrice() * Math.exp(discountFactor * (endTick - startTick));
    }

    public double getAgreedValue() {
        return agreedValue;
    }
}
