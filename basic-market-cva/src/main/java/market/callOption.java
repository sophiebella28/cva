package market;

import org.apache.commons.math3.random.RandomGenerator;

public class callOption extends Derivative {
    public callOption(long startTick, long endTick, double discountFactor) {
        super(startTick, endTick, discountFactor);
    }

    @Override
    protected void calculateStartingValue(double timeStep) {
        
    }

    @Override
    public void calculateExpectedExposure(long duration, double timeStep, double stockPrice, double meanRev, double volatility, RandomGenerator generator, Trader trader) {

    }


}
