package org.example.models.market;

import java.util.HashMap;

public abstract class Derivative {
    long startTick;
    public long endTick;
    double discountFactor;
    HashMap<Long, Double> defaultProbability;

    public Derivative(long startTick, long endTick, double discountFactor) {
        this.startTick = startTick;
        this.endTick = endTick;
        this.discountFactor = discountFactor;
        defaultProbability = new HashMap<>();
    }

    // returns the expected exposure of this derivative at the given time as a percentage
    double getExpectedExposure(long atTick) {
        return Math.sqrt(atTick - startTick) * 0.01;
    }

    double getDiscountFactor(long atTick) {
        return Math.exp(discountFactor * (atTick - startTick));
    }

    double getDefaultProb(long atTick, double riskPercent, long ticksPerStep) {
       /* if (defaultProbability.size() == 0) {
            defaultProbability.put(atTick, Math.exp(-atTick * riskPercent));
        } else if (defaultProbability.get(atTick) == null) {
            defaultProbability.put(atTick, defaultProbability.get( (atTick - ticksPerStep)) * Math.exp(-ticksPerStep * riskPercent));
        }
        return defaultProbability.get(atTick);*/
        return 0.3;
    }

}
