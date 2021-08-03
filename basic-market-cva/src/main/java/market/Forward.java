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
    public void calculateExpectedExposure(long duration, double timeStep, double interestRate, double meanRev, double equilibrium, double volatility, double swapRate, RandomGenerator generator) {

        for (int i = 0; i < 250; i++) {
            // todo: consider taking an uneven sample of time points
            double sampleInterest = interestRate;
            for (int j = 0; j < duration; j++) {
                sampleInterest = sampleInterest + meanRev * (equilibrium - sampleInterest) * timeStep + generator.nextGaussian() * Math.sqrt(timeStep) * volatility;
                double total = 0.0;
                double finalDiscount = 0.0;
                for (int k = 1; k <= (duration - j); k++) {
                    double B = (1 - Math.exp(-meanRev * k * timeStep)) / meanRev;
                    //todo: dont need to recalculate A and B every time -- FIX
                    double A = Math.exp(((B - k * timeStep) * (Math.pow(meanRev, 2) * equilibrium - Math.pow(volatility, 2) / 2)) / Math.pow(meanRev, 2) - (Math.pow(volatility, 2) * Math.pow(B, 2) / (4 * meanRev)));
                    double discount = A * Math.exp(-B * sampleInterest);
                    total += discount;
                    if (k == (duration - j )) {
                        finalDiscount = discount;
                    }
                }
                double fixedLeg = swapRate * total * 0.25;
                double floatingLeg = 1 - finalDiscount;
                double mtm = fixedLeg - floatingLeg;
                if (mtm > 0) {
                    double prevExposure = expectedExposure.getOrDefault(j * timeStep, 0.0);
                    expectedExposure.put(j * timeStep, prevExposure + mtm / 250);
                }

            }

        }
    }




    @Override
    protected double getExpectedExposure(long atTick, double timeStep) {
        return expectedExposure.getOrDefault(atTick * timeStep, 0.0);
    }
}
