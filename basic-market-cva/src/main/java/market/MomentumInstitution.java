package market;

import simudyne.core.abm.Action;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MomentumInstitution extends Trader {

    @Variable(name = "Long Term Moving Average")
    public double longTermMovingAvg;

    @Variable(name = "Short Term Moving Average")
    public double shortTermMovingAvg;

    public Map<Long, Double> historicalPrices = new HashMap<>();

    private static Action<MomentumInstitution> action(SerializableConsumer<MomentumInstitution> consumer) {
        return Action.create(MomentumInstitution.class, consumer);
    }

    public static Action<MomentumInstitution> sendTrades() {
        return action(
                trader -> {
                    if (trader.getContext().getTick() > trader.getGlobals().longTermAverage) {
                        trader.longTermMovingAvg = trader.getTermMovingAvg(trader.getGlobals().longTermAverage);
                        trader.shortTermMovingAvg = trader.getTermMovingAvg(trader.getGlobals().shortTermAverage);
                        double probToBuy = trader.getPrng().uniform(0, 1).sample();

                        if (trader.shortTermMovingAvg > trader.longTermMovingAvg && probToBuy < trader.getGlobals().traderActivity) {
                            trader.buy();
                        } else if ((trader.shortTermMovingAvg < trader.longTermMovingAvg && probToBuy < trader.getGlobals().traderActivity)) {
                            trader.sell();
                        }
                    }
                });
    }

    public static Action<MomentumInstitution> updateFields(long currentTick) {
        return action(
                institution -> {
                    institution.portfolio.closeTrades(currentTick);
                    institution.updateCva(currentTick,institution.getMessageOfType(Messages.UpdateFields.class).price);
                    institution.historicalPrices.put(institution.getContext().getTick(), institution.getMessageOfType(Messages.UpdateFields.class).price);
                });
    }

    public static Action<MomentumInstitution> getValueChanges(long currentTick) {
        return action(
                institution -> {
                    double totalValueChange = institution.getMessagesOfType(Messages.ChangeValue.class).stream().map(link -> link.valueChange).reduce(0.0, Double::sum);
                    int totalAssetChange = institution.getMessagesOfType(Messages.ChangeAssets.class).stream().map(link -> link.noOfAssets).reduce(0, Integer::sum);
                    institution.totalValue += totalValueChange;
                    institution.numberOfAssets += totalAssetChange;
                });
    }

    public double getTermMovingAvg(long nbDays) {
        double totalPrice = historicalPrices.entrySet().stream().filter(a -> a.getKey() > getContext().getTick() - nbDays).mapToDouble(Map.Entry::getValue).sum();
        return totalPrice / nbDays;
    }

    @Override
    public void init() {
        super.init();
        portfolio = new Portfolio(this);
        totalValue = 1500;
        numberOfAssets = 10;
    }
}
