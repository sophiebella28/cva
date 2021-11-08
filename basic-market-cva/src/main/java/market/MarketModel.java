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
        // generates given number of institutions
        Group<Institution> institutionGroup = generateGroup(Institution.class, getGlobals().nmInstitutions);

        // generates given numbers of momentum institutions
        Group<MomentumInstitution> momInstitutionGroup = generateGroup(MomentumInstitution.class, getGlobals().nmMomInstitutions);
        // generates pricing desk and cds desk
        Group<PricingDesk> priceGroup = generateGroup(PricingDesk.class, 1);
        Group<CDSDesk> cdsGroup = generateGroup(CDSDesk.class, 1);

        // connects all of the groups - all traders are connected to the desks and vice versa
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

        // logic to determine the correct hedging strategy - currently it is dictated by three different boolean switches
        // in the console. It would be better to replace this with a drop down.
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
        // generates random value for information signal
        getGlobals().informationSignal = new Random().nextGaussian() * getGlobals().volatilityInfo;
        // sends trades, then calculates new prices, then updates values in each of the traders
        Sequence makeTradesAndHedges = Sequence.create(InstitutionBase.sendTrades(), PricingDesk.calcPrices(),
                Trader.updateFields(getContext().getTick()));
        // traders make any hedges they want to according to the hedging strategy and then send a message to the cds
        // desk who creates these hedges
        Sequence makeHedges = Sequence.create(Trader.sendHedges(getContext().getTick()), CDSDesk.createHedges());

        // traders check whether they have defaulted, and if they have then the pricing desk and cds desk close their
        // trades and evalute the amount of money other counterparties have made from cds protection and distribute this
        Sequence checkDefault = Sequence.create(InstitutionBase.checkDefault(), PricingDesk.closeDefaultedTrades(), CDSDesk.evaluateCds(),Trader.cdsGains());
        // trades are closed if the end of their duration is reached and then values are updated in the traders
        Sequence closeTrades = Sequence.create(PricingDesk.closeTrades(getContext().getTick()), Split.create(Trader.updateValues(), CDSDesk.updateValues()));
        run(makeTradesAndHedges);
        run(makeHedges);
        run(checkDefault);
        run(closeTrades);

        // a time variable so that current tick doesnt need to be passed around - hasnt been fully refactored into the code
        getGlobals().time = getContext().getTick() * getGlobals().timeStep;
    }
}
