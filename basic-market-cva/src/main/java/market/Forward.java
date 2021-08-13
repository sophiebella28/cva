package market;

import org.apache.commons.math3.random.RandomGenerator;
import simudyne.core.abm.Agent;

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
        calculateStartingValue(assetType.getPrice());
    }



    public double getAgreedValue() {
        return agreedValue;
    }

    @Override
    public void calculateExpectedExposure(long duration, double stockPrice, RandomGenerator generator, Agent<Globals> trader, Globals globals) {
        // data gathered from historical apple stock prices for the last 14 years
        double timeStep = globals.timeStep;
        double mu = globals.mean;
        double sigma = globals.volatility;
        for (int i = 0; i < 250; i++) {
            // todo: consider taking an uneven sample of time points
            double sampleStockPrice = stockPrice;
            for (int j = 0; j < duration; j++) {
                double stockChange = timeStep * mu * sampleStockPrice + sigma * Math.sqrt(timeStep) * sampleStockPrice * generator.nextGaussian();

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
    }

    @Override
    public double getCurrentValue(double currentTick, double timeStep, double interestRate, double stockVolatility) {
        double stockPrice = assetType.getPrice();
        double f = stockPrice - agreedValue * Math.exp(-interestRate * (endTick - currentTick * timeStep));
        return f * agreedValue;
    }


    @Override
    protected double getExpectedExposure(long atTick, double timeStep) {
        return expectedExposure.getOrDefault(atTick * timeStep, 0.0);
    }

    @Override
    protected void calculateStartingValue(double stockPrice) {
        agreedValue = stockPrice;
    }
}
