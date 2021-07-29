package market;

import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

public abstract class Trader extends Agent<MarketModel.Globals> {
    public Portfolio portfolio = new Portfolio();
    @Variable
    public double totalValue;
    @Variable
    public double numberOfAssets;

    @Variable
    public double cvaPercent;

    public void addDerivativeToPortfolio(Derivative derivative) {
        portfolio.add(derivative);
    }

    void updateCva(double currentTime) {
        cvaPercent = portfolio.updateCva(currentTime,getGlobals().timeStep, getGlobals().hazardRate, getGlobals().recoveryRate);
    }


}
