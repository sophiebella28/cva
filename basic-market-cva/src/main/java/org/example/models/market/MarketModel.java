package org.example.models.market;

import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Group;
import simudyne.core.annotations.Input;

public class MarketModel extends AgentBasedModel<MarketModel.Globals> {
    public static final class Globals extends GlobalState {
        @Input(name = "Recovery Rate")
        public double recoveryRate = 0.4;

        @Input(name = "Number of Institutions")
        public int nmInstitutions = 20;

        @Input(name = "Trade Rate")
        public double tradeRate = 0.5;

        @Input(name = "Hazard rate")
        public double hazardRate = 0.083;

        @Input(name = "Number of ticks per iteration")
        public long ticksPerStep = 1;
    }

    {
        registerAgentTypes(Institution.class, PriceDictator.class);
        registerLinkTypes(Links.TradeLink.class, Links.MarketLink.class);
    }

    @Override
    public void setup() {
        Group<Institution> institutionGroup = generateGroup(Institution.class, getGlobals().nmInstitutions);
        Group<PriceDictator> priceGroup = generateGroup(PriceDictator.class, 1);

        institutionGroup.fullyConnected(priceGroup, Links.MarketLink.class);
        institutionGroup.fullyConnected(institutionGroup, Links.TradeLink.class);
        priceGroup.fullyConnected(institutionGroup, Links.MarketLink.class);

        super.setup();
    }

    @Override
    public void step() {
        super.step();

        //getGlobals().informationSignal = new Random().nextGaussian() * getGlobals().volatilityInfo;

        run(Institution.sendTrades(), Institution.makeTrades(), PriceDictator.calcPrices(), Institution.calculateCva());
    }
}
