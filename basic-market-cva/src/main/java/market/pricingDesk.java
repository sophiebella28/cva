package market;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.functions.SerializableConsumer;

import java.util.List;
import java.util.stream.Collectors;

public class pricingDesk extends Trader{

    Portfolio bankPortfolio;
    AssetType bankAsset;

    @Override
    public void init() {
        super.init();
        bankPortfolio = new Portfolio();
        bankAsset = AssetType.ASSET1;
    }

    private static Action<pricingDesk> action(SerializableConsumer<pricingDesk> consumer) {
        return Action.create(pricingDesk.class, consumer);
    }

    public static Action<pricingDesk> calcPrices() {
        return action(pricingDesk -> {

            List<Institution> fromList = pricingDesk.getMessagesOfType(Messages.ForwardTrade.class).stream().map(link -> link.from).collect(Collectors.toList());
            for (Institution inst : fromList) {
                Trader floating;
                Trader fixed;
                if (inst.random.nextBoolean()) {
                    floating = inst;
                    fixed = pricingDesk;
                } else {
                    floating = pricingDesk;
                    fixed = inst;
                }
                long startTick = pricingDesk.getContext().getTick();
                // between 1 to 10 years - check
                long endTick = startTick + (int) (pricingDesk.getPrng().generator.nextGaussian() * 60) + 60;
                Forward forwardToAdd = new Forward(fixed,floating, startTick, endTick, 0.05, pricingDesk.bankAsset);
                floating.addDerivativeToPortfolio(forwardToAdd);
                fixed.addDerivativeToPortfolio(forwardToAdd);
            }

            pricingDesk.updateCva(pricingDesk.getContext().getTick());

            pricingDesk.getLinks(Links.MarketLink.class).send(Messages.CvaUpdate.class);
        });
    }
}
