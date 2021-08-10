package market;

import org.apache.commons.math3.random.RandomGenerator;
import simudyne.core.abm.Agent;

public class callOption extends Derivative {
    public callOption(long startTick, long endTick, double discountFactor) {
        super(startTick, endTick, discountFactor);
    }

    @Override
    protected void calculateStartingValue(double timeStep) {
        
    }

    @Override
    public void calculateExpectedExposure(long duration, double timeStep, double stockPrice, RandomGenerator generator, Agent<Globals> owner) {

    }


}
