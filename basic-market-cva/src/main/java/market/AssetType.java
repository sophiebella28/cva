package market;

import org.apache.commons.math3.random.RandomGenerator;

public interface AssetType {

    double getPrice();
    void updatePrice(RandomGenerator random);
}

