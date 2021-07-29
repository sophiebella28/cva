package market;

import simudyne.core.abm.Action;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

import java.util.List;
import java.util.stream.Collectors;

public class PricingDesk extends Trader{

    AssetType bankAsset;

    @Variable
    double price = 4.0;


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

                // need to add better logic here - need a concept of available assets
                if (inst.random.nextDouble() > 0.3 && inst.numberOfAssets > 5 ) {
                    System.out.printf("pricing desk is floating with %f assets\n", pricingDesk.numberOfAssets);
                    floating = pricingDesk;
                    fixed = inst;
                    addForward(pricingDesk, floating, fixed);
                } else if (pricingDesk.numberOfAssets > 5 ){
                    System.out.printf("trader is floating with %f assets\n", inst.numberOfAssets);
                    floating = inst;
                    fixed = pricingDesk;
                    addForward(pricingDesk, floating, fixed);
                }

            }

            pricingDesk.updateCva(pricingDesk.getGlobals().time);

            pricingDesk.price = pricingDesk.bankAsset.updatePrice(pricingDesk.getPrng().generator);
            pricingDesk.getLinks(Links.MarketLink.class).send(Messages.CvaUpdate.class);
        });
    }

    private static void addForward(PricingDesk pricingDesk, Trader floating, Trader fixed) {
        double startTime = pricingDesk.getGlobals().time;
        double endTime = startTime + ( Math.abs(pricingDesk.getPrng().generator.nextInt(10)) * pricingDesk.getGlobals().timeStep) ;
        // for now only buying one of each asset but might change that
        // changing would involve adding an int to the message so each trader could have strategy
        Forward forwardToAdd = new Forward(fixed,floating, startTime, endTime, 0.05, pricingDesk.bankAsset, 1);
        floating.addDerivativeToPortfolio(forwardToAdd);
        fixed.addDerivativeToPortfolio(forwardToAdd);
        System.out.printf("New forward added starting at %f and ending at %f with price %f \n", startTime, endTime, forwardToAdd.getAgreedValue());
    }
}
