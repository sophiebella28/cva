package market;

import org.apache.commons.math3.random.RandomGenerator;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Institution extends Trader{
    RandomGenerator random;




    private static Action<Institution> action(SerializableConsumer<Institution> consumer) {
        return Action.create(Institution.class, consumer);
    }

    public static Action<Institution> sendTrades() {
        return action(institution -> {
            if (institution.random.nextDouble() > institution.getGlobals().tradeRate) {
                institution.getLinks(Links.MarketLink.class).send(Messages.ForwardTrade.class, (msg,link) -> msg.from = institution);
                //sends a message to the pricing desk containing itself so the trade can be made
            }
        });
    }


    public static Action<Institution> calculateCva(long currentTick) {
        return action(institution -> {
            institution.updateCva(currentTick);
        });
    }



    @Override
    public void init() {
        super.init();
        portfolio = new Portfolio();
        random = this.getPrng().generator;
        // later on i will probably add better logic for assigning companies the assets they trade but for now
        // each company is randomly assigned an asset
    }



}
