package market;

import simudyne.core.abm.Action;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

import java.util.HashMap;
import java.util.Map;

public class MomentumInstitution extends InstitutionBase {

    @Variable(name = "Long Term Moving Average")
    public double longTermMovingAvg;

    @Variable(name = "Short Term Moving Average")
    public double shortTermMovingAvg;

    public Map<Long, Double> historicalPrices = new HashMap<>();

    private static Action<MomentumInstitution> action(SerializableConsumer<MomentumInstitution> consumer) {
        return Action.create(MomentumInstitution.class, consumer);
    }

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

    @Override
    void updateInfo() {
        historicalPrices.put(getContext().getTick(), getMessageOfType(Messages.UpdateFields.class).price);
    }
}
