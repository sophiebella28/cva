package market;

import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.Group;
import simudyne.core.abm.Split;

import java.util.Random;


//@ModelSettings(timeUnit = "DAYS")
public class MarketModel extends AgentBasedModel<Globals> {
    {
        registerAgentTypes(Institution.class, PricingDesk.class, MomentumInstitution.class, CDSDesk.class);
        registerLinkTypes(Links.MarketLink.class);
    }

    @Override
    public void setup() {
        Group<Institution> institutionGroup = generateGroup(Institution.class, getGlobals().nmInstitutions);

        Group<MomentumInstitution> momInstitutionGroup = generateGroup(MomentumInstitution.class, getGlobals().nmMomInstitutions);
        Group<PricingDesk> priceGroup = generateGroup(PricingDesk.class, 1);
        Group<CDSDesk> cdsGroup = generateGroup(CDSDesk.class, 1);

        institutionGroup.fullyConnected(priceGroup, Links.MarketLink.class);
        momInstitutionGroup.fullyConnected(priceGroup, Links.MarketLink.class);
        institutionGroup.fullyConnected(cdsGroup, Links.HedgingLink.class);
        momInstitutionGroup.fullyConnected(cdsGroup, Links.HedgingLink.class);

        priceGroup.fullyConnected(momInstitutionGroup, Links.MarketLink.class);
        priceGroup.fullyConnected(institutionGroup, Links.MarketLink.class);
        cdsGroup.fullyConnected(momInstitutionGroup, Links.HedgingLink.class);
        cdsGroup.fullyConnected(institutionGroup, Links.HedgingLink.class);

        super.setup();
    }

    @Override
    public void step() {
        super.step();


        getGlobals().informationSignal = new Random().nextGaussian() * getGlobals().volatilityInfo;

        run(Institution.sendTrades(), PricingDesk.calcPrices(),
                Trader.updateFields(getContext().getTick()), CDSDesk.createHedges(), Trader.hedgeUpdates());
        getGlobals().time = getContext().getTick() * getGlobals().timeStep;
    }
}
