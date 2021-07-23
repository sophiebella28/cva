package market;

import simudyne.core.abm.Action;
import simudyne.core.functions.SerializableConsumer;

import java.util.List;
import java.util.stream.Collectors;

public class PricingDesk extends Trader{

    AssetType bankAsset;


    @Override
    public void init() {
        super.init();
        bankAsset = new BankAsset();
        totalValue = 1500;
        numberOfAssets = 100;
    }

    private static Action<PricingDesk> action(SerializableConsumer<PricingDesk> consumer) {
        return Action.create(PricingDesk.class, consumer);
    }

    public static Action<PricingDesk> calcPrices() {
        return action(pricingDesk -> {

            List<Institution> fromList = pricingDesk.getMessagesOfType(Messages.ForwardTrade.class).stream().map(link -> link.from).collect(Collectors.toList());
            for (Institution inst : fromList) {
                Trader floating;
                Trader fixed;
                if (inst.random.nextBoolean() && inst.numberOfAssets > 0) {
                    floating = inst;
                    fixed = pricingDesk;
                } else {
                    floating = pricingDesk;
                    fixed = inst;
                }
                long startTick = pricingDesk.getContext().getTick();
                long endTick = startTick + (int) (pricingDesk.getPrng().generator.nextGaussian() * 60) + 60;
                // for now only buying one of each asset but might change that
                // changing would involve adding an int to the message so each trader could have strategy
                Forward forwardToAdd = new Forward(fixed,floating, startTick, endTick, 0.05, pricingDesk.bankAsset, 1);
                floating.addDerivativeToPortfolio(forwardToAdd);
                fixed.addDerivativeToPortfolio(forwardToAdd);
            }

            pricingDesk.updateCva(pricingDesk.getContext().getTick());

            pricingDesk.bankAsset.updatePrice(pricingDesk.getPrng().generator);
            pricingDesk.getLinks(Links.MarketLink.class).send(Messages.CvaUpdate.class);
        });
    }
}
