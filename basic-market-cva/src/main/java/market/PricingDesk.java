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
        totalMoney = 1500;
        numberOfAssets = 100;
        portfolio = new Portfolio();
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

            List<Trader> buyerList = pricingDesk.getMessagesOfType(Messages.CallOptionBuyTrade.class).stream().map(link -> link.from).collect(Collectors.toList());
            List<Trader> sellerList = pricingDesk.getMessagesOfType(Messages.CallOptionSellTrade.class).stream().map(link -> link.from).collect(Collectors.toList());
            for (Trader inst : buyerList) {
                addCallOption(pricingDesk, inst, pricingDesk);
            }

            for (Trader inst : sellerList) {
                addCallOption(pricingDesk, pricingDesk, inst);
            }


            // todo: change this so it is fueled by the market demand
            double priceChange = pricingDesk.getPrng().generator.nextGaussian();
            pricingDesk.price = pricingDesk.bankAsset.updatePrice(priceChange);
            pricingDesk.send(Messages.UpdateFields.class, (msg) -> {
                msg.priceChange = priceChange;
                msg.price = pricingDesk.price;
            }).to(pricingDesk.getID());
            pricingDesk.getLinks(Links.MarketLink.class).send(Messages.UpdateFields.class, (msg, link) -> {
                msg.priceChange = priceChange;
                msg.price = pricingDesk.price;
            });
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

    private static void addCallOption(PricingDesk PricingDesk, Trader buyer, Trader seller) {
        long startTick = PricingDesk.getContext().getTick();
        long endTick = startTick + (Math.abs(PricingDesk.getPrng().generator.nextInt(10)) + 3);

        // for now only buying one of each asset but might change that
        // changing would involve adding an int to the message so each trader could have strategy
        CallOption callOption = new CallOption(startTick, endTick, 0.05, buyer, seller, 1, PricingDesk.bankAsset);
        buyer.addDerivativeToPortfolio(callOption);
        seller.addDerivativeToPortfolio(callOption);
    }

    public void closeTrades(long currentTick) {
        List<Derivative> derivativeList = portfolio.derivativeList;
        for (Derivative derivative : derivativeList) {
            if (derivative.endTick == currentTick) {
                if (derivative instanceof Forward) {
                    Forward forward = (Forward) derivative;
                    Trader floating = forward.floating;
                    Trader fixed = forward.fixed;
                    sendValueChanges(floating, fixed, forward.agreedValue, forward.amountOfAsset, forward.assetType);
                    // need a measure of whether or not this was actually lost idk
                }
                if (derivative instanceof CallOption) {
                    CallOption option = (CallOption) derivative;
                    Trader buyer = option.buyer;
                    Trader seller = option.seller;
                    if (option.agreedValue < option.assetType.getPrice()) {
                        sendValueChanges(buyer, seller, option.agreedValue, option.amountOfAsset, option.assetType);
                    }
                }
            }
        }
        for (CDS cds : portfolio.hedgingList) {
            if(currentTick == cds.startTick || (currentTick - cds.startTick ) % 12 == 0 ) {
                Trader trader = cds.buyer;
                CDSDesk desk = cds.desk;
                trader.send(Messages.ChangeValue.class, (msg) -> msg.valueChange = -cds.yearly * cds.notional).to(desk.getID());
                desk.send(Messages.ChangeValue.class, (msg) -> msg.valueChange = cds.yearly * cds.notional).to(trader.getID());
            }
        }
    }

    private void sendValueChanges(Trader floating, Trader fixed, double agreedValue, int amountOfAsset, AssetType assetType) {
        double valueChange = agreedValue * amountOfAsset;
        send(Messages.ChangeValue.class, (msg) -> msg.valueChange = valueChange).to(fixed.getID());
        send(Messages.ChangeValue.class, (msg) -> msg.valueChange = -valueChange).to(floating.getID());

        send(Messages.ChangeAssets.class, (msg) -> msg.noOfAssets = -amountOfAsset).to(fixed.getID());
        send(Messages.ChangeAssets.class, (msg) -> msg.noOfAssets = amountOfAsset).to(floating.getID());

        totalMoney += amountOfAsset * (agreedValue - assetType.getPrice());
    }
}
