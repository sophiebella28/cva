package market;

import simudyne.core.abm.Action;
import simudyne.core.functions.SerializableConsumer;

public abstract class InstitutionBase extends Trader {

    private static Action<InstitutionBase> action(SerializableConsumer<InstitutionBase> consumer) {
        return Action.create(InstitutionBase.class, consumer);
    }

    protected void sell() {
        if( getPrng().generator.nextBoolean()) {
            getLinks(Links.MarketLink.class).send(Messages.ForwardFixedTrade.class, (msg, link) -> msg.from = this);
        } else {
            getLinks(Links.MarketLink.class).send(Messages.CallOptionSellTrade.class, (msg, link) -> msg.from = this);
        }


    }

    protected void buy() {
        if( getPrng().generator.nextBoolean()) {
            getLinks(Links.MarketLink.class).send(Messages.ForwardFloatingTrade.class, (msg, link) -> msg.from = this);
        } else {
            getLinks(Links.MarketLink.class).send(Messages.CallOptionBuyTrade.class, (msg, link) -> msg.from = this);
        }

    }

    public static Action<InstitutionBase> sendTrades() {
        return action(
                InstitutionBase::buyOrSell);
    }

    protected abstract void buyOrSell();





    abstract void updateInfo();
}