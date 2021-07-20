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

        @Input(name = "Number of ticks per iteration")
        public long ticksPerStep = 1;

        public long currentTick = 0;
    }

    {
        registerAgentTypes(Institution.class, pricingDesk.class);
        registerLinkTypes(Links.MarketLink.class);
    }

    @Override
    public void setup() {
        Group<Institution> institutionGroup = generateGroup(Institution.class, getGlobals().nmInstitutions);
        Group<pricingDesk> priceGroup = generateGroup(pricingDesk.class, 1);

        institutionGroup.fullyConnected(priceGroup, Links.MarketLink.class);
        priceGroup.fullyConnected(institutionGroup, Links.MarketLink.class);

        super.setup();
    }

    @Override
    public void step() {
        super.step();

        run(Institution.sendTrades(), pricingDesk.calcPrices(), Institution.calculateCva(getContext().getTick()));
    }
}
