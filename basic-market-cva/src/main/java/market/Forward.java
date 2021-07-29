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

    HashMap<Double, Double> expectedExposure;

    public Forward(Trader fixed, Trader floating, double startTime, double endTime, double discountFactor, AssetType assetType, int amountOfAsset) {
        super(startTime, endTime, discountFactor);
        this.fixed = fixed;
        this.floating = floating;
        this.assetType = assetType;
        this.amountOfAsset = amountOfAsset;
        calculateStartingValue();
        expectedExposure = new HashMap<>();
    }

    @Override
    protected void calculateStartingValue() {
        agreedValue = assetType.getPrice() * Math.exp(discountFactor * (endTime - startTime));
    }

    public double getAgreedValue() {
        return agreedValue;
    }

    @Override
    public void calculateExpectedExposure(double duration, double timeStep, double interestRate, double meanRev, double equilibrium, double volatility, double swapRate, RandomGenerator generator) {

        for (int i = 0; i < 250; i++) {
            // todo: consider taking an uneven sample of time points
            double sampleInterest = interestRate;
            for (double j = 0; j < duration; j += timeStep) {
                j = Math.round(j * 100) / 100.0;

                sampleInterest = sampleInterest + meanRev * (equilibrium - sampleInterest) * timeStep + generator.nextGaussian() * Math.sqrt(timeStep) * volatility;
                double total = 0.0;
                double finalDiscount = 0.0;
                for (double k = j; k < duration; k += timeStep) {
                    k = Math.round(j * 100) / 100.0;
                    double B = (1 - Math.exp(-meanRev * k)) / meanRev;
                    double A = Math.exp(((B - k) * (Math.pow(meanRev, 2) * equilibrium - Math.pow(volatility, 2) / 2)) / Math.pow(meanRev, 2) - (Math.pow(volatility, 2) * Math.pow(B, 2) / (4 * meanRev)));
                    double discount = A * Math.exp(-B * sampleInterest);
                    total += discount;
                    if (k == Math.round((duration - timeStep) * 100) / 100.0) {
                        finalDiscount = discount;
                    }
                }
                double fixedLeg = swapRate * total * 0.25;
                double floatingLeg = 1 - finalDiscount;
                double mtm = fixedLeg - floatingLeg;
                if (mtm > 0) {
                    double prevExposure = (expectedExposure.get(j) == null) ? expectedExposure.get(j) : 0.0;
                    expectedExposure.put(j, prevExposure + mtm / 250);
                }

            }

        }
    }

    @Override
    protected double getExpectedExposure(double atTime) {
        return expectedExposure.get(atTime);
    }
}
