package org.example.models.market;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.functions.SerializableConsumer;
import simudyne.core.graph.Message;

public class PriceDictator extends Agent<MarketModel.Globals> {

    private static Action<PriceDictator> action(SerializableConsumer<PriceDictator> consumer) {
        return Action.create(PriceDictator.class, consumer);
    }

    public static Action<PriceDictator> calcPrices() {
        return action(priceDictator -> {
            // if this doesn't work i might have to call getMessages
            // also realistically at some point ill have to have a measure of changing the price
            // which will be passed
            // so idk
            // i actually can't really think of anything to put here at all oops
            // im going to leave it out then i guess
            priceDictator.getLinks(Links.MarketLink.class).send(Messages.CvaUpdate.class);
        });
    }
}
