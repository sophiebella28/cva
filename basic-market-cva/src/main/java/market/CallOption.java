package market;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import simudyne.core.abm.Agent;

import java.util.HashMap;

public class CallOption extends Derivative {
    AssetType assetType;

    Trader buyer;
    Trader seller;
    int amountOfAsset = 0;
    double agreedValue = 0;

    HashMap<Double, Double> expectedExposure = new HashMap<>();

    public CallOption(long startTick, long endTick, double discountFactor, Trader buyer, Trader seller, int amountOfAsset, AssetType assetType) {
        super(startTick, endTick, discountFactor);
        this.buyer = buyer;
        this.seller = seller;
        this.amountOfAsset = amountOfAsset;
        this.assetType = assetType;
        calculateStartingValue(assetType.getPrice());
    }
    @Override
    protected void calculateStartingValue(double stockPrice) {
        agreedValue = stockPrice;
    }

    @Override
    public void calculateExpectedExposure(long duration, double stockPrice, RandomGenerator generator, Agent<Globals> trader, Globals globals) {
        if (trader == buyer) {
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
                    double mtm = floatingLeg - fixedLeg;
                    if (mtm > 0) {
                        double prevExposure = expectedExposure.getOrDefault(j * timeStep, 0.0);
                        expectedExposure.put(j * timeStep, prevExposure + mtm / 250);
                    }

                }
            }
        }

    }

    @Override
    public double getCurrentValue(double currentTick, double timeStep, double interestRate, double stockVolatility) {
        double stockPrice = assetType.getPrice();
        double sigmaRootT = stockVolatility * Math.sqrt(currentTick * timeStep);
        double d1 = (Math.log(stockPrice/agreedValue) + (interestRate + Math.pow(stockVolatility,2)/2) * (currentTick * timeStep))/ (sigmaRootT);
        double d2 = d1 - sigmaRootT;
        NormalDistribution normal = new NormalDistribution();
        double c = stockPrice * normal.cumulativeProbability(d1) - agreedValue * Math.exp(-interestRate * (currentTick * timeStep)) * normal.cumulativeProbability(d2);
        return c * amountOfAsset;
    }

    @Override
    protected double getExpectedExposure(long atTick, double timeStep) {
        return expectedExposure.getOrDefault(atTick * timeStep, 0.0);
    }


}
