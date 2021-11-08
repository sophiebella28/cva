package market;

import simudyne.core.annotations.Variable;


public class BankAsset implements AssetType{

    @Variable
    double price = 9.9;

    @Override
    public double getPrice() {
        return price;
    }

    // the asset that is being traded
    // currently the model is only using one but it could be expanded to use multiple

    @Override
    public double updatePrice(double priceChange) {
        price = Math.abs(price + priceChange);
        return price;
    }
}
