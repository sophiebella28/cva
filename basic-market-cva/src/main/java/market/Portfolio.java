package market;

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

    public double updateCva(long currentTick, long ticksPerStep, double hazardRate, double recoveryRate) {
        // find the longest time in the portfolio
        if (isEmpty()) {
            cvaPercent = 0;
        } else {
            long last = Collections.max(derivativeList, Comparator.comparingLong(derivative -> derivative.endTick)).endTick;
            double cvaSum = 0;
            for (Derivative derivative : derivativeList) {
                if (derivative.endTick >= currentTick) {
                    for (long i = 0; i < last - currentTick; i += ticksPerStep) {
                        double expectedExposure = derivative.getExpectedExposure(i);

                        double defaultProb = derivative.getDefaultProb(i, hazardRate, ticksPerStep);

                        double discountFactor = derivative.getDiscountFactor(i);

                        double cvaIndividual = expectedExposure * defaultProb * discountFactor;

                        cvaSum += cvaIndividual;

                    }
                }

            }
            cvaPercent = (1 -recoveryRate) * cvaSum;
        }
        return cvaPercent;
    }

    public void closeTrades(long currentTick) {
        for (Derivative derivative : derivativeList) {
            if (derivative.endTick < currentTick) {
                if (derivative instanceof Forward) {
                    Forward forward = (Forward) derivative;
                    forward.floating.numberOfAssets += forward.amountOfAsset;
                    forward.fixed.numberOfAssets += forward.agreedValue * forward.amountOfAsset;
                    totalValue +=  forward.amountOfAsset * ( forward.agreedValue - forward.assetType.getPrice());
                    // need a measure of whether or not this was actually lost idk
                }
            }

        }
    }
}
