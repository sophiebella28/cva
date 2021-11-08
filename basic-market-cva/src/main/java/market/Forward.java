package market;

public class Forward extends Derivative {
    AssetType assetType;
    Trader buyer;
    Trader seller;
    int amountOfAsset;
    double agreedValue = 0;

    // a forward derivative
    public Forward(Trader seller, Trader buyer, long startTick, long endTick, double discountFactor, AssetType assetType, int amountOfAsset, double timeStep) {
        super(startTick, endTick, discountFactor);
        this.seller = seller;
        this.buyer = buyer;
        this.assetType = assetType;
        this.amountOfAsset = amountOfAsset;
        calculateStartingValue(assetType.getPrice());
    }


    // exposure of a forward is the difference between agreed and current price
    @Override
    protected double uniqueExposureCalculation(double price, Trader trader) {
        if (trader == buyer) {
            return agreedValue - price;
        } else {
            return price - agreedValue;
        }
    }

    // current value as valued by the equation in gregory's book
    @Override
    public double getCurrentValue(double currentTick, double timeStep, double interestRate, double stockVolatility, Trader owner) {
        double stockPrice = assetType.getPrice();
        double f = (stockPrice - agreedValue) * Math.exp(-interestRate * ((currentTick - startTick) * timeStep));

        return (owner == seller) ? f * agreedValue : -f * agreedValue;
    }

    @Override
    public double getAgreedValue() {
        return agreedValue;
    }


    // expected exposure map is populated during the cva calculation
    @Override
    protected double getExpectedExposure(long atTick, double timeStep) {
        return expectedExposure.getOrDefault(atTick * timeStep, 0.0);
    }



    @Override
    protected void calculateStartingValue(double stockPrice) {
        agreedValue = stockPrice;
    }

    @Override
    protected Trader getCounterparty(Trader current) {
        if (current == buyer) {
            return seller;
        }
        return buyer;

    }
}
