package market;

import org.apache.commons.math3.random.RandomGenerator;
import simudyne.core.annotations.Variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Portfolio {

    List<Derivative> derivativeList;

    @Variable
    public double cvaPercent;

    @Variable
    public double totalValue;

    public Portfolio() {
        derivativeList = new ArrayList<>();
    }


    public boolean isEmpty() {
        return derivativeList.isEmpty();
    }

    public void add(Derivative derivative) {
        derivativeList.add(derivative);
    }

    public double updateCva(long currentTick, double timeStep, double hazardRate, double recoveryRate, double meanRev, double equilibrium, double volatility, double swapRate, RandomGenerator generator) {
        // find the longest time in the portfolio
        if (isEmpty()) {
            cvaPercent = 0;
        } else {
            long last = Collections.max(derivativeList, Comparator.comparingLong(derivative -> derivative.endTick)).endTick;
            double cvaSum = 0;
            for (Derivative derivative : derivativeList) {
                if (derivative.endTick >= currentTick) {
                    derivative.calculateExpectedExposure(derivative.endTick - currentTick, timeStep, 0.05, meanRev, equilibrium, volatility, swapRate, generator);
                    for (long i = 0; i < last - currentTick; i ++) {

                        double expectedExposure = derivative.getExpectedExposure(i, timeStep);

                        double defaultProb = derivative.getDefaultProb(i, hazardRate, timeStep);

                        double discountFactor = derivative.getDiscountFactor(i, timeStep);

                        double cvaIndividual = expectedExposure * defaultProb * discountFactor;

                        cvaSum += cvaIndividual;

                    }
                }

            }
            cvaPercent = (1 - recoveryRate) * cvaSum;
        }
        return cvaPercent;
    }

    public void closeTrades(long currentTick) {
        for (Derivative derivative : derivativeList) {
            if (derivative.endTick == currentTick) {
                if (derivative instanceof Forward) {
                    Forward forward = (Forward) derivative;
                    forward.floating.numberOfAssets += forward.amountOfAsset;
                    forward.fixed.numberOfAssets -= forward.amountOfAsset;
                    forward.fixed.totalValue += forward.agreedValue * forward.amountOfAsset;
                    forward.floating.totalValue -= forward.agreedValue * forward.amountOfAsset;
                    totalValue += forward.amountOfAsset * ( forward.agreedValue - forward.assetType.getPrice());
                    // need a measure of whether or not this was actually lost idk
                }

            }
        }
    }
}
