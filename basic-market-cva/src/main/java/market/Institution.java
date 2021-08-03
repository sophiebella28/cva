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

    public static Action<Institution> getValueChanges(long currentTick) {
        return action(
                institution -> {
                    double totalValueChange = institution.getMessagesOfType(Messages.ChangeValue.class).stream().map(link -> link.valueChange).reduce(0.0, Double::sum);
                    int totalAssetChange = institution.getMessagesOfType(Messages.ChangeAssets.class).stream().map(link -> link.noOfAssets).reduce(0, Integer::sum);
                    institution.totalValue += totalValueChange;
                    institution.numberOfAssets += totalAssetChange;
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
