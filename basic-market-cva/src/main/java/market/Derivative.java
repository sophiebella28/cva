package market;

import org.apache.commons.math3.random.RandomGenerator;

import java.util.HashMap;

public abstract class Derivative {
    double startTime;
    public double endTime;
    double discountFactor;

    HashMap<Double, Double> counterPartySurvives;

    public Derivative(double startTime, double endTime, double discountFactor) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.discountFactor = discountFactor;
        counterPartySurvives = new HashMap<>();
        counterPartySurvives.put(0.0, 1.00);

    }

    private double getPrevCounterParty(double atTime, double timeStep) {
        return counterPartySurvives.get((double) Math.round((atTime - timeStep) * 100) / 100.0);
    }

    // returns the expected exposure of this derivative at the given time as a percentage
    protected double getExpectedExposure(double atTime) {
        return Math.sqrt(atTime) * 0.01;
    }

    protected double getDiscountFactor(double atTime) {
        return Math.exp(-discountFactor * (atTime));
    }

    protected double getDefaultProb(double atTime, double riskPercent, double timeStep) {
        if (counterPartySurvives.get(atTime) == null) {
            calculateCntptySurvives(atTime, riskPercent, timeStep);
        }
        if (atTime == 0.0) {
            return 0;
        }
        return getPrevCounterParty(atTime, timeStep) - counterPartySurvives.get(atTime);

    }

    protected void calculateCntptySurvives(double atTime, double riskPercent, double timeStep) {
        if (atTime == 0.0) {
            counterPartySurvives.put(atTime, 1.00);
        } else if (counterPartySurvives.get(atTime) == null) {
            counterPartySurvives.put(atTime, getPrevCounterParty(atTime, timeStep) * Math.exp(-(timeStep) * riskPercent));
        }
    }

    protected abstract void calculateStartingValue();

    public abstract void calculateExpectedExposure(double duration, double timeStep, double interestRate, double meanRev, double equilibrium, double volatility, double swapRate, RandomGenerator generator);

}
