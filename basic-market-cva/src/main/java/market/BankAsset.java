package market;

import org.apache.commons.math3.random.RandomGenerator;
import simudyne.core.annotations.Variable;


public class BankAsset implements AssetType{

    @Variable
    double price = 4.0;

    @Override
    public double getPrice() {
        return price;
    }


    @Override
    public double updatePrice(double priceChange) {
        price = Math.abs(price + priceChange);
        return price;
    }
}
