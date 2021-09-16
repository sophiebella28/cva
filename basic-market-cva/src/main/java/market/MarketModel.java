package market;

import scala.collection.SeqExtractors;
import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.Group;
import simudyne.core.abm.Sequence;
import simudyne.core.abm.Split;

import java.util.Random;


public class MarketModel extends AgentBasedModel<Globals> {
    {
        registerAgentTypes(Institution.class, PricingDesk.class, MomentumInstitution.class, CDSDesk.class);
        registerLinkTypes(Links.MarketLink.class, Links.HedgingLink.class);
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

        priceGroup.fullyConnected(cdsGroup, Links.HedgingLink.class);
        cdsGroup.fullyConnected(momInstitutionGroup, Links.HedgingLink.class);
        cdsGroup.fullyConnected(institutionGroup, Links.HedgingLink.class);

        cdsGroup.fullyConnected(priceGroup, Links.HedgingLink.class);

        if(getGlobals().addOnHedge) {
            getGlobals().hedgingStrategy = HedgingStrategy.ADDON;
        } else if (getGlobals().runOutHedge) {
            getGlobals().hedgingStrategy = HedgingStrategy.RUNOUT;
        } else {
            getGlobals().hedgingStrategy = HedgingStrategy.EVERY;
        }

        super.setup();
    }

    @Override
    public void step() {
        super.step();

        getGlobals().informationSignal = new Random().nextGaussian() * getGlobals().volatilityInfo;

        Sequence makeTradesAndHedges = Sequence.create(InstitutionBase.sendTrades(), PricingDesk.calcPrices(),
                Split.create(Trader.updateFields(getContext().getTick()), CDSDesk.updateValues()));
        //todo: update the desk values better

        Sequence makeHedges = Sequence.create(Trader.sendHedges(getContext().getTick()), CDSDesk.createHedges());

        Sequence checkDefault = Sequence.create(InstitutionBase.checkDefault(), PricingDesk.closeDefaultedTrades(), CDSDesk.evaluateCds(),Trader.cdsGains());

        run(makeTradesAndHedges);
        run(makeHedges);
        run(checkDefault);

        getGlobals().time = getContext().getTick() * getGlobals().timeStep;
    }
}
