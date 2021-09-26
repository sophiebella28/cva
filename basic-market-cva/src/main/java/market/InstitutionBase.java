package market;

import simudyne.core.abm.Action;
import simudyne.core.functions.SerializableConsumer;

public abstract class InstitutionBase extends Trader {

    private static Action<InstitutionBase> action(SerializableConsumer<InstitutionBase> consumer) {
        return Action.create(InstitutionBase.class, consumer);
    }

    protected void sell() {
        if (getPrng().generator.nextBoolean()) {
            getLinks(Links.MarketLink.class).send(Messages.ForwardFixedTrade.class, (msg, link) -> msg.from = this);
        } else {
            getLinks(Links.MarketLink.class).send(Messages.CallOptionSellTrade.class, (msg, link) -> msg.from = this);
        }


    }

    protected void buy() {
        if (getPrng().generator.nextBoolean()) {
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

    public static Action<InstitutionBase> checkDefault() {
        return action(instBase -> {
            if (instBase.totalMoney < 0) {
                System.out.println("MADE IT");
                instBase.getLinks(Links.MarketLink.class).send(Messages.DefaultNotification.class, (msg, link) -> msg.defaulted = instBase);
                instBase.totalMoney = instBase.getGlobals().startingMoney;
                instBase.numberOfAssets = instBase.getGlobals().instStartingAssets;
            }
        });
    }

    @Override
    public void init() {
        super.init();
        totalMoney = getGlobals().startingMoney;
        numberOfAssets = getGlobals().instStartingAssets;
        portfolio = new Portfolio();
    }

    abstract void updateInfo();
}
