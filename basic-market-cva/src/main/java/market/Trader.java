package market;

import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

public abstract class Trader extends Agent<MarketModel.Globals> {
    public Portfolio portfolio;
    @Variable
    public double totalValue;
    @Variable
    public double numberOfAssets;

    @Variable
    public double cvaPercent;

    public void addDerivativeToPortfolio(Derivative derivative) {
        portfolio.add(derivative);
    }

    void updateCva(long currentTick, double stockPrice) {
        cvaPercent = portfolio.updateCva(currentTick,getGlobals().timeStep, getGlobals().hazardRate, getGlobals().recoveryRate, getGlobals().meanRev, getGlobals().volatility, stockPrice, getPrng().generator);
    }


    protected void sell() {
        getLinks(Links.MarketLink.class).send(Messages.ForwardFixedTrade.class, (msg, link) -> msg.from = this);


    }

    protected void buy() {
        getLinks(Links.MarketLink.class).send(Messages.ForwardFloatingTrade.class, (msg, link) -> msg.from = this);
    }
}
