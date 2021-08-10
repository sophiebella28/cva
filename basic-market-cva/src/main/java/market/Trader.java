package market;

import org.apache.commons.math3.random.RandomGenerator;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class Trader extends Agent<Globals> {
    public Portfolio portfolio;

    @Variable
    public double totalValue;
    @Variable
    public double numberOfAssets;

    @Variable
    public double cvaPercent;

    private static Action<Trader> action(SerializableConsumer<Trader> consumer) {
        return Action.create(Trader.class, consumer);
    }
    public void addDerivativeToPortfolio(Derivative derivative) {
        portfolio.add(derivative);
    }

    void updateCva(long currentTick, double stockPrice) {
        double timeStep = getGlobals().timeStep;
        double hazardRate = getGlobals().hazardRate;
        double recoveryRate = getGlobals().recoveryRate;
        RandomGenerator generator = getPrng().generator;
        List<Derivative> derivativeList = portfolio.derivativeList;

        // find the longest time in the portfolio
        if (portfolio.derivativeIsEmpty()) {
            cvaPercent = 0;
        } else {
            long last = Collections.max(derivativeList, Comparator.comparingLong(derivative -> derivative.endTick)).endTick;
            double cvaSum = 0;
            for (Derivative derivative : derivativeList) {
                if (derivative.endTick >= currentTick) {
                    derivative.calculateExpectedExposure(derivative.endTick - currentTick, timeStep, stockPrice, generator, this);
                    for (long i = 0; i < last - currentTick; i++) {

                        double expectedExposure = derivative.getExpectedExposure(i, timeStep);

                        double defaultProb = derivative.getDefaultProb(i, hazardRate, timeStep);

                        double discountFactor = derivative.getDiscountFactor(i, timeStep);

                        double cvaIndividual = expectedExposure * defaultProb * discountFactor;

                        cvaSum += cvaIndividual;

                    }
                }

            }
            cvaPercent = (1 - recoveryRate) * cvaSum;
        }

    }

    public static Action<Trader> updateFields(long currentTick) {
        // there is going to be an issue that the pricing desk isnt being sent a message itself
        return action(
                trader -> {
                    trader.updateCva(currentTick,trader.getMessageOfType(Messages.UpdateFields.class).price);
                    double totalValueChange = trader.getMessagesOfType(Messages.ChangeValue.class).stream().map(link -> link.valueChange).reduce(0.0, Double::sum);
                    int totalAssetChange = trader.getMessagesOfType(Messages.ChangeAssets.class).stream().map(link -> link.noOfAssets).reduce(0, Integer::sum);
                    trader.totalValue += totalValueChange;
                    trader.numberOfAssets += totalAssetChange;
                    if (trader instanceof InstitutionBase) {
                         ((InstitutionBase) trader).updateInfo();
                    }
                });
    }


}
