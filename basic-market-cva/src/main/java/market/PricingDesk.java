package market;

import org.apache.commons.math3.random.RandomGenerator;
import simudyne.core.abm.Action;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

import java.util.List;
import java.util.stream.Collectors;

public class PricingDesk extends Trader {

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

            List<Trader> floatingList = pricingDesk.getMessagesOfType(Messages.ForwardFloatingTrade.class).stream().map(link -> link.from).collect(Collectors.toList());
            List<Trader> fixedList = pricingDesk.getMessagesOfType(Messages.ForwardFixedTrade.class).stream().map(link -> link.from).collect(Collectors.toList());
            for (Trader inst : floatingList) {
                addForward(pricingDesk, inst, pricingDesk);
            }

            for (Trader inst : fixedList) {
                addForward(pricingDesk, pricingDesk, inst);
            }

            pricingDesk.updateCva(pricingDesk.getContext().getTick());
            // todo: change this so it is fueled by the market demand
            double priceChange = pricingDesk.getPrng().generator.nextGaussian();
            pricingDesk.price = pricingDesk.bankAsset.updatePrice(priceChange);
            pricingDesk.getLinks(Links.MarketLink.class).send(Messages.UpdateFields.class, (msg,link) -> { msg.priceChange = priceChange; msg.price = pricingDesk.price;});
        });
    }

    private static void addForward(PricingDesk pricingDesk, Trader floating, Trader fixed) {
        long startTick = pricingDesk.getContext().getTick();
        long endTick = startTick + (Math.abs(pricingDesk.getPrng().generator.nextInt(10)));
        // for now only buying one of each asset but might change that
        // changing would involve adding an int to the message so each trader could have strategy
        Forward forwardToAdd = new Forward(fixed, floating, startTick, endTick, 0.05, pricingDesk.bankAsset, 1, pricingDesk.getGlobals().timeStep);
        floating.addDerivativeToPortfolio(forwardToAdd);
        fixed.addDerivativeToPortfolio(forwardToAdd);
        //System.out.printf("New forward added starting at %d and ending at %d with price %f \n", startTick, endTick, forwardToAdd.getAgreedValue());
    }
}
