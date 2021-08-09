package market;

import org.apache.commons.math3.random.RandomGenerator;

import java.util.HashMap;

public class Forward extends Derivative {
    AssetType assetType;
    Trader floating;
    Trader fixed;
    int amountOfAsset = 0;
    double agreedValue = 0;
    // todo: research whether forwards have interest rates

    HashMap<Double, Double> expectedExposure = new HashMap<>();

    public Forward(Trader fixed, Trader floating, long startTick, long endTick, double discountFactor, AssetType assetType, int amountOfAsset, double timeStep) {
        super(startTick, endTick, discountFactor);
        this.fixed = fixed;
        this.floating = floating;
        this.assetType = assetType;
        this.amountOfAsset = amountOfAsset;
        calculateStartingValue(timeStep);
    }

    @Override
    protected void calculateStartingValue(double timeStep) {
        agreedValue = assetType.getPrice() * Math.exp(discountFactor * (endTick - startTick) * timeStep);
    }

    public double getAgreedValue() {
        return agreedValue;
    }

    @Override
    public void calculateExpectedExposure(long duration, double timeStep, double stockPrice,RandomGenerator generator, Trader trader) {
        // data gathered from historical apple stock prices for the last 14 years

        double mu = 0.33 * timeStep;
        System.out.println("mu is " + mu);
        double sigma = Math.sqrt(Math.pow(0.323,2) * timeStep);
        System.out.println("sigma is " + sigma);
        for (int i = 0; i < 250; i++) {
            // todo: consider taking an uneven sample of time points
            double sampleStockPrice = stockPrice;
            for (int j = 0; j < duration; j++) {
                double stockChange = timeStep * mu * sampleStockPrice + sigma * Math.sqrt(timeStep) * sampleStockPrice * generator.nextGaussian();

                //System.out.println(stockChange);
                sampleStockPrice += stockChange;

                double fixedLeg = agreedValue;
                double floatingLeg = sampleStockPrice;
                double mtm = (trader == floating) ? fixedLeg - floatingLeg : floatingLeg - fixedLeg;
                if (mtm > 0) {
                    double prevExposure = expectedExposure.getOrDefault(j * timeStep, 0.0);
                    expectedExposure.put(j * timeStep, prevExposure + mtm / 250);
                }

            }



        }

        System.out.println("expected exposure at 2 ticks is " + expectedExposure.get( 2 * timeStep) );
    }




    @Override
    protected double getExpectedExposure(long atTick, double timeStep) {
        return expectedExposure.getOrDefault(atTick * timeStep, 0.0);
    }
}
