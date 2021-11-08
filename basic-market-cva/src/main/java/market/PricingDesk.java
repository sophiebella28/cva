package market;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PricingDesk extends Trader {

    AssetType bankAsset;

    @Variable
    double price = 9.9;


    // initialises the pricingdesk to defaults
    @Override
    public void init() {
        super.init();
        bankAsset = new BankAsset();
        totalMoney = getGlobals().startingMoney;
        numberOfAssets = getGlobals().centralStartingAssets;
        portfolio = new Portfolio();
    }

    // default function to return actions
    private static Action<PricingDesk> action(SerializableConsumer<PricingDesk> consumer) {
        return Action.create(PricingDesk.class, consumer);
    }

    // receives the message to calculate stock prices and calculates them by using the equation detailed in simudyne docs
    // for an improvement it could be updated to the chiarella model
    public static Action<PricingDesk> calcPrices() {
        return action(pricingDesk -> {

            // totals up all the different derivates that were purchased and creates them and adds them to its own
            // portfolio
            List<Trader> floatingList = pricingDesk.getMessagesOfType(Messages.ForwardFloatingTrade.class).stream().map(link -> link.from).collect(Collectors.toList());
            List<Trader> fixedList = pricingDesk.getMessagesOfType(Messages.ForwardFixedTrade.class).stream().map(link -> link.from).collect(Collectors.toList());

            int buys = floatingList.size();
            int sells = floatingList.size();
            for (Trader inst : floatingList) {
                addForward(pricingDesk, inst, pricingDesk);
            }

            for (Trader inst : fixedList) {
                addForward(pricingDesk, pricingDesk, inst);
            }

            List<Trader> buyerList = pricingDesk.getMessagesOfType(Messages.CallOptionBuyTrade.class).stream().map(link -> link.from).collect(Collectors.toList());
            List<Trader> sellerList = pricingDesk.getMessagesOfType(Messages.CallOptionSellTrade.class).stream().map(link -> link.from).collect(Collectors.toList());

            buys += buyerList.size();
            sells += sellerList.size();
            for (Trader inst : buyerList) {
                addCallOption(pricingDesk, inst, pricingDesk);
            }

            for (Trader inst : sellerList) {
                addCallOption(pricingDesk, pricingDesk, inst);
            }

            int netDemand = buys - sells;

            double priceChange;
            if (netDemand == 0) {
                priceChange = 0.0;
            } else {
                long nbTraders = pricingDesk.getGlobals().nmInstitutions;
                double lambda = pricingDesk.getGlobals().lambda;
                priceChange = (netDemand / (double) nbTraders) / lambda;
            }
            pricingDesk.price = pricingDesk.bankAsset.updatePrice(priceChange);
            // now prices have been calculated it sends the messages to the traders
            double finalPriceChange = priceChange;
            pricingDesk.send(Messages.UpdateFields.class, (msg) -> {
                msg.priceChange = finalPriceChange;
                msg.price = pricingDesk.price;
            }).to(pricingDesk.getID());
            pricingDesk.getLinks(Links.MarketLink.class).send(Messages.UpdateFields.class, (msg, link) -> {
                msg.priceChange = finalPriceChange;
                msg.price = pricingDesk.price;
            });
        });
    }

    // creates the forward for a randomly generated amount of time
    private static void addForward(PricingDesk PricingDesk, Trader floating, Trader fixed) {
        long startTick = PricingDesk.getContext().getTick();
        long endTick = startTick + (Math.abs(PricingDesk.getPrng().generator.nextInt(10)) + 3);

        // for now only buying one of each asset but could change that
        // changing would involve adding an int to the message so each trader could have strategy
        Forward forwardToAdd = new Forward(fixed, floating, startTick, endTick, 0.05, PricingDesk.bankAsset, 1, PricingDesk.getGlobals().timeStep);
        floating.addDerivativeToPortfolio(forwardToAdd);
        fixed.addDerivativeToPortfolio(forwardToAdd);
    }

    // creates the option for a randomly generated amount of time
    private static void addCallOption(PricingDesk PricingDesk, Trader buyer, Trader seller) {
        long startTick = PricingDesk.getContext().getTick();
        long endTick = startTick + (Math.abs(PricingDesk.getPrng().generator.nextInt(10)) + 3);

        // for now only buying one of each asset but might change that
        // changing would involve adding an int to the message so each trader could have strategy
        CallOption callOption = new CallOption(startTick, endTick, 0.05, buyer, seller, 1, PricingDesk.bankAsset);
        buyer.addDerivativeToPortfolio(callOption);
        seller.addDerivativeToPortfolio(callOption);
    }

    // receives the message to check which trades need closing and updates the relevant traders
    public static Action<PricingDesk> closeTrades(long currentTick) {
        return action(desk -> {
        List<Derivative> derivativeList = desk.portfolio.derivativeList;
        for (Derivative derivative : derivativeList) {
            if (derivative.endTick == currentTick) {
                if (derivative instanceof Forward) {
                    Forward forward = (Forward) derivative;
                    Trader floating = forward.buyer;
                    Trader fixed = forward.seller;
                    desk.sendValueChanges(floating, fixed, forward.amountOfAsset, forward.agreedValue * forward.amountOfAsset);
                }
                if (derivative instanceof CallOption) {
                    CallOption option = (CallOption) derivative;
                    Trader buyer = option.buyer;
                    Trader seller = option.seller;
                    if (option.agreedValue < option.assetType.getPrice()) {
                        desk.sendValueChanges(buyer, seller, option.amountOfAsset, option.agreedValue * option.amountOfAsset);

                    }
                }
            }
        }
        for (CDS cds : desk.portfolio.hedgingList) {
            if (currentTick == cds.startTick || (currentTick - cds.startTick) % 12 == 0) {
                Trader trader = cds.buyer;
                desk.sendValueChanges(trader, desk, 0, cds.yearly * cds.notional);

            }
        }
        });
    }

    // sends messages to the relevant traders about the amount of assets/money that need to be exchanged
    private void sendValueChanges(Agent<Globals> buyer, Agent<Globals> seller, int amountOfAsset, double valueChange) {

        send(Messages.ChangeValue.class, (msg) -> msg.valueChange = valueChange).to(seller.getID());
        send(Messages.ChangeValue.class, (msg) -> msg.valueChange = -valueChange).to(buyer.getID());

        send(Messages.ChangeAssets.class, (msg) -> msg.noOfAssets = -amountOfAsset).to(seller.getID());
        send(Messages.ChangeAssets.class, (msg) -> msg.noOfAssets = amountOfAsset).to(buyer.getID());

    }

    // if a company has defaulted, this cycles through all of the trades made with that company and closes them
    // and also sends a message to all the traders about how that counterparty defaulted so they can total any cds
    // protection that they purchased
    public static Action<PricingDesk> closeDefaultedTrades() {
        return action(pricingDesk -> {
            List<Trader> defaultedList = pricingDesk.getMessagesOfType(Messages.DefaultNotification.class).stream().map(link -> link.defaulted).collect(Collectors.toList());
            List<Derivative> defaultedTrades = new ArrayList<>();
            for (Derivative derivative : pricingDesk.portfolio.derivativeList) {
                if (derivative instanceof Forward) {
                    if (defaultedList.contains(((Forward) derivative).buyer)) {
                        pricingDesk.totalMoney += pricingDesk.getGlobals().recoveryRate * ((Forward) derivative).agreedValue; //todo: is this actually what they get??? i forgot oops
                        defaultedTrades.add(derivative);
                    }
                } else if (derivative instanceof CallOption) {
                    if (defaultedList.contains(((CallOption) derivative).buyer)) {
                        pricingDesk.totalMoney += pricingDesk.getGlobals().recoveryRate * ((CallOption) derivative).agreedValue;
                        defaultedTrades.add(derivative);
                    }
                }
            }
            pricingDesk.portfolio.derivativeList.removeAll(defaultedTrades);

            pricingDesk.getLinks(Links.HedgingLink.class).send(Messages.DefaultList.class, (msg, link) -> msg.defaulted = defaultedList);
        });
    }
}
