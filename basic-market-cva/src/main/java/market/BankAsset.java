package market;

import org.apache.commons.math3.random.RandomGenerator;


public class BankAsset implements AssetType{
    double price = 4.0;

    @Override
    public double getPrice() {
        return price;
    }


    @Override
    public void updatePrice(RandomGenerator random) {
        price = price + random.nextGaussian();
    }
}
