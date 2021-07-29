package market;

import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Group;
import simudyne.core.annotations.Input;


//@ModelSettings(timeUnit = "DAYS")
public class MarketModel extends AgentBasedModel<MarketModel.Globals> {
    public static final class Globals extends GlobalState {
        @Input(name = "Recovery Rate")
        public double recoveryRate = 0.4;

        @Input(name = "Number of Institutions")
        public int nmInstitutions = 5;

        @Input(name = "Trade Rate")
        public double tradeRate = 0.5;

        @Input(name = "Hazard rate")
        public double hazardRate = 0.083;
        // todo calculate this instead of taking it as an input

        @Input(name = "Fraction of a year per tick")
        public double timeStep = 0.08;

        @Input(name = "Mean Reversion")
        public double meanRev = 0.1;

        @Input(name = "Equilibrium")
        public double equilibrium = 0.05;

        @Input(name = "Volatility")
        public double volatility = 0.01;

        @Input(name = "Swap rate")
        public double swapRate = 0.05;


        public double time = 0;
    }

    {
        registerAgentTypes(Institution.class, PricingDesk.class);
        registerLinkTypes(Links.MarketLink.class);
    }

    @Override
    public void setup() {
        Group<Institution> institutionGroup = generateGroup(Institution.class, getGlobals().nmInstitutions);
        Group<PricingDesk> priceGroup = generateGroup(PricingDesk.class, 1);

        institutionGroup.fullyConnected(priceGroup, Links.MarketLink.class);
        priceGroup.fullyConnected(institutionGroup, Links.MarketLink.class);

        super.setup();
    }

    @Override
    public void step() {
        super.step();
        run(Institution.sendTrades(), PricingDesk.calcPrices(), Institution.calculateCva(getGlobals().time));
        getGlobals().time += getGlobals().timeStep;
        getGlobals().time = Math.round(getGlobals().time * 100)/ 100.0;
    }
}
