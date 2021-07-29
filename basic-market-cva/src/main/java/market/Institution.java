package market;

import org.apache.commons.math3.random.RandomGenerator;
import simudyne.core.abm.Action;
import simudyne.core.functions.SerializableConsumer;

public class Institution extends Trader {
    RandomGenerator random;


    private static Action<Institution> action(SerializableConsumer<Institution> consumer) {
        return Action.create(Institution.class, consumer);
    }

    public static Action<Institution> sendTrades() {
        return action(institution -> {
            if (institution.random.nextDouble() > institution.getGlobals().tradeRate) {
                institution.getLinks(Links.MarketLink.class).send(Messages.ForwardTrade.class, (msg, link) -> msg.from = institution);
                //sends a message to the pricing desk containing itself so the trade can be made
            }
        });
    }


    public static Action<Institution> calculateCva(double currentTime) {
        return action(institution -> {
            institution.portfolio.closeTrades(currentTime);
            institution.updateCva(currentTime);
        });
    }


    @Override
    public void init() {
        super.init();
        portfolio = new Portfolio();
        random = this.getPrng().generator;
        totalValue = 1500;
        numberOfAssets = 10;
    }


}
