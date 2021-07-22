package market;

import java.util.HashMap;

public abstract class Derivative {
    long startTick;
    public long endTick;
    double discountFactor;
    double value;

    HashMap<Long, Double> counterPartySurvives;

    public Derivative(long startTick, long endTick, double discountFactor) {
        this.startTick = startTick;
        this.endTick = endTick;
        this.discountFactor = discountFactor;
        counterPartySurvives = new HashMap<>();
        counterPartySurvives.put((long) 0, 1.00);
    }

    // returns the expected exposure of this derivative at the given time as a percentage
    protected double getExpectedExposure(long atTick) {
        return Math.sqrt(atTick) * 0.08 * 0.01;
    }

    protected double getDiscountFactor(long atTick) {
        return Math.exp(-discountFactor * (atTick) * 0.08);
    }

    protected double getDefaultProb(long atTick, double riskPercent, long ticksPerStep) {
        if (counterPartySurvives.get(atTick) == null) {
            calculateCntptySurvives(atTick, riskPercent, ticksPerStep);
        }
        if (atTick == (long) 0) {
            return 0;
        }
        return counterPartySurvives.get(atTick - 1) - counterPartySurvives.get(atTick);

    }

    protected void calculateCntptySurvives(long atTick, double riskPercent, long ticksPerStep) {
        if (atTick == 0) {
            counterPartySurvives.put(atTick, 1.00);
        } else if (counterPartySurvives.get(atTick) == null) {
            counterPartySurvives.put(atTick, counterPartySurvives.get(atTick - ticksPerStep) * Math.exp(-(ticksPerStep * 0.08) * riskPercent));
        }
    }

    abstract double price();

}
