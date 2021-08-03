package market;

import org.apache.commons.math3.random.RandomGenerator;
import simudyne.core.abm.Action;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

public class Institution extends Trader {
    RandomGenerator random;
    @Variable
    double tradingThresh;


    private static Action<Institution> action(SerializableConsumer<Institution> consumer) {
        return Action.create(Institution.class, consumer);
    }

    public static Action<Institution> sendTrades() {
        return action(institution -> {
            double informationSignal = institution.getGlobals().informationSignal;
            if (Math.abs(informationSignal) > institution.tradingThresh) {
                if (informationSignal > 0) {
                    institution.buy();
                } else {
                    institution.sell();
                }
            }

        });
    }


    public static Action<Institution> updateFields(long currentTick) {
        return action(institution -> {
            institution.portfolio.closeTrades(currentTick);
            institution.updateCva(currentTick);
            double updateFrequency = 0.01; // todo: make this global
            if (institution.random.nextDouble() <= updateFrequency) {
                institution.tradingThresh =
                        institution.getMessageOfType(Messages.UpdateFields.class).priceChange;
            }
        });
    }


    @Override
    public void init() {
        super.init();
        portfolio = new Portfolio();
        random = this.getPrng().generator;
        totalValue = 1500;
        numberOfAssets = 10;
        tradingThresh = random.nextGaussian();
    }


}
