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
    public double totalMoney;
    @Variable
    public double numberOfAssets;

    @Variable
    public double totalValue;

    @Variable
    public double cvaPercent;

    private static Action<Trader> action(SerializableConsumer<Trader> consumer) {
        return Action.create(Trader.class, consumer);
    }
    public void addDerivativeToPortfolio(Derivative derivative) {
        portfolio.add(derivative);
    }

    public double calculatePortfolioValue() {
        double total = 0.0;
        for (Derivative derivative : portfolio.derivativeList) {
            total += derivative.getCurrentValue(getContext().getTick(),getGlobals().timeStep,0.05,getGlobals().volatility);
        }
        return total;
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
                    derivative.calculateExpectedExposure(derivative.endTick - currentTick,  stockPrice, generator, this, getGlobals());
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
        return action(
                trader -> {
                    trader.updateCva(currentTick,trader.getMessageOfType(Messages.UpdateFields.class).price);
                    double totalValueChange = trader.getMessagesOfType(Messages.ChangeValue.class).stream().map(link -> link.valueChange).reduce(0.0, Double::sum);
                    int totalAssetChange = trader.getMessagesOfType(Messages.ChangeAssets.class).stream().map(link -> link.noOfAssets).reduce(0, Integer::sum);
                    trader.totalMoney += totalValueChange;
                    trader.numberOfAssets += totalAssetChange;
                    if (trader instanceof InstitutionBase) {
                         ((InstitutionBase) trader).updateInfo();
                    }
                    trader.totalValue = trader.calculatePortfolioValue();
                });
    }


}
