package market;

import org.apache.commons.math3.random.RandomGenerator;

import java.util.HashMap;

public abstract class Derivative {
    long startTick;
    public long endTick;
    double discountFactor;

    HashMap<Double, Double> counterPartySurvives;

    public Derivative(long startTick, long endTick, double discountFactor) {
        this.startTick = startTick;
        this.endTick = endTick;
        this.discountFactor = discountFactor;
        counterPartySurvives = new HashMap<>();
        counterPartySurvives.put(0.0, 1.00);

    }

    private double getPrevCounterParty(long atTick, double timeStep) {
        //System.out.println((atTick - 1) * timeStep);
        return counterPartySurvives.get(((atTick - 1) * timeStep));
    }

    // returns the expected exposure of this derivative at the given time as a percentage
    protected double getExpectedExposure(long atTick, double timeStep) {
        return Math.sqrt(atTick * timeStep) * 0.01;
    }

    protected double getDiscountFactor(long atTick, double timeStep) {
        return Math.exp(-discountFactor * (atTick * timeStep));
    }

    protected double getDefaultProb(long atTick, double riskPercent, double timeStep) {
        if (counterPartySurvives.get(atTick * timeStep) == null) {
            calculateCntptySurvives(atTick, riskPercent, timeStep);
        }
        if (atTick == (long) 0) {
            return 0;
        }
        return getPrevCounterParty(atTick, timeStep) - counterPartySurvives.get(atTick * timeStep);

    }

    protected void calculateCntptySurvives(long atTick, double riskPercent, double timeStep) {

        //System.out.println("HELLO");
        if (atTick == (long) 1) {
            counterPartySurvives.put(atTick * timeStep, 1.00);
           // System.out.println(counterPartySurvives.get(0.0));
        } else if (counterPartySurvives.get(atTick * timeStep) == null) {
            //System.out.println(atTick * timeStep);
            counterPartySurvives.put(atTick * timeStep, getPrevCounterParty(atTick, timeStep) * Math.exp(-(timeStep) * riskPercent));
        }
    }

    protected abstract void calculateStartingValue(double timeStep);

    public abstract void calculateExpectedExposure(long duration, double timeStep, double interestRate, double meanRev, double equilibrium, double volatility, double swapRate, RandomGenerator generator);

}
