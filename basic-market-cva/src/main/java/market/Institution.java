package market;

import org.apache.commons.math3.random.RandomGenerator;
import simudyne.core.abm.Action;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

public class Institution extends InstitutionBase {
    RandomGenerator random;
    @Variable
    double tradingThresh;

    // the institution class is essentially a noise trader class - it buys and sells randomly
    private static Action<Institution> action(SerializableConsumer<Institution> consumer) {
        return Action.create(Institution.class, consumer);
    }

    @Override
    public void buyOrSell() {
            double informationSignal = getGlobals().informationSignal;
            if (Math.abs(informationSignal) > tradingThresh) {
                if (informationSignal > 0) {
                    buy();
                } else {
                    sell();
                }
            }
    }

    // updates the information signal and has a chance of updating the price change
    @Override
    void updateInfo() {
        double updateFrequency = 0.01; // it would be better if this was a global variable
        if (random.nextDouble() <= updateFrequency) {
            tradingThresh =
                    getMessageOfType(Messages.UpdateFields.class).priceChange;
        }
    }


    @Override
    public void init() {
        super.init();
        portfolio = new Portfolio();
        random = this.getPrng().generator;
        tradingThresh = random.nextGaussian();
    }

}
