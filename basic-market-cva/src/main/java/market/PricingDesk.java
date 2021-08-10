package market;

import simudyne.core.abm.Action;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

import java.util.List;
import java.util.stream.Collectors;

public class PricingDesk extends Trader {

    AssetType bankAsset;

    @Variable
    double price = 9.9;


    @Override
    public void init() {
        super.init();
        bankAsset = new BankAsset();
        totalValue = 1500;
        numberOfAssets = 100;
        portfolio = new Portfolio(this);
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


            // todo: change this so it is fueled by the market demand
            double priceChange = pricingDesk.getPrng().generator.nextGaussian();
            pricingDesk.price = pricingDesk.bankAsset.updatePrice(priceChange);
            System.out.println("Price is" + pricingDesk.price);
            pricingDesk.send(Messages.UpdateFields.class, (msg) -> { msg.priceChange = priceChange; msg.price = pricingDesk.price;}).to(pricingDesk.getID());
            pricingDesk.getLinks(Links.MarketLink.class).send(Messages.UpdateFields.class, (msg, link) -> { msg.priceChange = priceChange; msg.price = pricingDesk.price;});
            pricingDesk.closeTrades(pricingDesk.getContext().getTick());
        });
    }

    private static void addForward(PricingDesk PricingDesk, Trader floating, Trader fixed) {
        long startTick = PricingDesk.getContext().getTick();
        long endTick = startTick + (Math.abs(PricingDesk.getPrng().generator.nextInt(10)) + 3);

        // for now only buying one of each asset but might change that
        // changing would involve adding an int to the message so each trader could have strategy
        Forward forwardToAdd = new Forward(fixed, floating, startTick, endTick, 0.05, PricingDesk.bankAsset, 1, PricingDesk.getGlobals().timeStep);
        floating.addDerivativeToPortfolio(forwardToAdd);
        fixed.addDerivativeToPortfolio(forwardToAdd);
        //System.out.printf("New forward added starting at %d and ending at %d with price %f \n", startTick, endTick, forwardToAdd.getAgreedValue());
    }

    public void closeTrades(long currentTick) {
        List<Derivative> derivativeList = portfolio.derivativeList;
        for (Derivative derivative : derivativeList) {
            if (derivative.endTick == currentTick) {
                if (derivative instanceof Forward) {
                    Forward forward = (Forward) derivative;
                    Trader floating = forward.floating;
                    Trader fixed = forward.fixed;
                    double valueChange = forward.agreedValue * forward.amountOfAsset;
                    send(Messages.ChangeValue.class, (msg) -> msg.valueChange = valueChange).to(fixed.getID());
                    send(Messages.ChangeValue.class, (msg) -> msg.valueChange = -valueChange).to(floating.getID());

                    send(Messages.ChangeAssets.class, (msg) -> msg.noOfAssets = -forward.amountOfAsset).to(fixed.getID());
                    send(Messages.ChangeAssets.class, (msg) -> msg.noOfAssets = forward.amountOfAsset).to(floating.getID());

                    totalValue += forward.amountOfAsset * (forward.agreedValue - forward.assetType.getPrice());
                    // need a measure of whether or not this was actually lost idk
                }

            }
        }
        // todo: figure out at which point the cds desk should close its trades - it should probably have its own method

    }
}
