package market;

import simudyne.core.abm.Action;
import simudyne.core.abm.messages.PoisonPill;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

import java.util.HashMap;
import java.util.Map;

public class MomentumInstitution extends InstitutionBase {

    // a trader that implements the momentum strategy of buying and selling derivatives
    // this strategy could be improved because its calculating the current averages but is purchasing derivatives that
    // it wont actually get for a certain period of time
    public double longTermMovingAvg;

    public double shortTermMovingAvg;

    public Map<Long, Double> historicalPrices = new HashMap<>();

    private static Action<MomentumInstitution> action(SerializableConsumer<MomentumInstitution> consumer) {
        return Action.create(MomentumInstitution.class, consumer);
    }

    // if short term moving average is bigger than long term moving average, buy, otherwise, sell
    @Override
    public void buyOrSell() {
        if (getContext().getTick() > getGlobals().longTermAverage) {
            longTermMovingAvg = getTermMovingAvg(getGlobals().longTermAverage);
            shortTermMovingAvg = getTermMovingAvg(getGlobals().shortTermAverage);
            double probToBuy = getPrng().uniform(0, 1).sample();

            if (shortTermMovingAvg > longTermMovingAvg && probToBuy < getGlobals().traderActivity) {
                buy();
            } else if ((shortTermMovingAvg < longTermMovingAvg && probToBuy < getGlobals().traderActivity)) {
                sell();
            }
        }
    }

    // calculates the short and long term moving averages
    public double getTermMovingAvg(long nbDays) {
        double totalPrice = historicalPrices.entrySet().stream().filter(a -> a.getKey() > getContext().getTick() - nbDays).mapToDouble(Map.Entry::getValue).sum();
        return totalPrice / nbDays;
    }

    @Override
    public void init() {
        super.init();
        portfolio = new Portfolio();
    }

    // keeps track of the historical prices for the averages
    @Override
    void updateInfo() {
        historicalPrices.put(getContext().getTick(), getMessageOfType(Messages.UpdateFields.class).price);
    }
}
