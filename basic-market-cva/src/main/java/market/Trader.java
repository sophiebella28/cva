package market;

import org.apache.commons.math3.random.RandomGenerator;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.abm.Section;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

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

    @Variable
    double hedgingNotional = 0.0;

    private static Action<Trader> action(SerializableConsumer<Trader> consumer) {
        return Action.create(Trader.class, consumer);
    }

    public static Action<Trader> hedgeUpdates() {
        return action(trader -> {

        });
    }

    public void addDerivativeToPortfolio(Derivative derivative) {
        portfolio.add(derivative);
    }

    public double calculatePortfolioValue() {
        double total = 0.0;
        for (Derivative derivative : portfolio.derivativeList) {
            total += derivative.getCurrentValue(getContext().getTick(), getGlobals().timeStep, 0.05, getGlobals().volatility, this);
        }
        return total - cvaPercent; //I think this is how it works but i should double check this
    }

    void updateCva(long currentTick, double stockPrice) {
        double timeStep = getGlobals().timeStep;
        double hazardRate = getGlobals().hazardRate;
        double recoveryRate = getGlobals().recoveryRate;
        RandomGenerator generator = getPrng().generator;
        List<Derivative> derivativeList = portfolio.derivativeList;
        hedgingNotional = 0;

        if (portfolio.derivativeIsEmpty()) {
            cvaPercent = 0;
        } else {
            double cvaSum = 0;
            for (Derivative derivative : derivativeList) {
                if (derivative.endTick > currentTick) {
                    derivative.calculateExpectedExposure(derivative.endTick - currentTick, stockPrice, generator, this, getGlobals());
                    long duration = derivative.endTick - currentTick;
                    double totalExpectedExposure = 0.0;
                    for (long i = 0; i < duration; i++) {

                        double expectedExposure = derivative.getExpectedExposure(i, timeStep);
                        totalExpectedExposure += expectedExposure;

                        double defaultProb = derivative.getDefaultProb(i, hazardRate, timeStep);
                        // look i just think perhaps the default probability shouldn't be tied to the derivative and should
                        // instead be calculated based on the institution
                        // but i dont really want to solve this problem

                        double discountFactor = derivative.getDiscountFactor(i, timeStep);

                        double cvaIndividual = expectedExposure * defaultProb * discountFactor;

                        cvaSum += cvaIndividual;

                    }
                    hedgingNotional += totalExpectedExposure / duration;
                }
            }
            cvaPercent = (1 - recoveryRate) * cvaSum;
        }
    }

    public static Action<Trader> updateFields(long currentTick) {
        return action(
                trader -> {
                    trader.updateCva(currentTick, trader.getMessageOfType(Messages.UpdateFields.class).price);
                    CDS cds = new CDS(trader,currentTick,currentTick+1,trader.hedgingNotional, 0.01 );
                    trader.getLinks(Links.HedgingLink.class).send(Messages.BuyCDS.class,(msg, link) -> {msg.tobuy = cds;});
                    trader.portfolio.add(cds);
                    double totalValueChange = trader.getMessagesOfType(Messages.ChangeValue.class).stream().map(link -> link.valueChange).reduce(0.0, Double::sum);
                    int totalAssetChange = trader.getMessagesOfType(Messages.ChangeAssets.class).stream().map(link -> link.noOfAssets).reduce(0, Integer::sum);
                    trader.totalMoney += totalValueChange;
                    trader.numberOfAssets += totalAssetChange;
                    if (trader instanceof InstitutionBase) {
                        ((InstitutionBase) trader).updateInfo();
                    }
                    trader.totalValue = trader.calculatePortfolioValue();
                    // am a bit concerned about negatives in the portfolio value
                });
    }


}
