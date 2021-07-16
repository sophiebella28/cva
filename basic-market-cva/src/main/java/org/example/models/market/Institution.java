package org.example.models.market;

import org.apache.commons.math3.random.RandomGenerator;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;
import simudyne.core.graph.FilteredLinks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Institution extends Agent<MarketModel.Globals> {
    List<Derivative> portfolio;
    List<AssetType> soldTypes; // types of assets that this company sells
    RandomGenerator random;

    @Variable
    double cvaPercent;


    private static Action<Institution> action(SerializableConsumer<Institution> consumer) {
        return Action.create(Institution.class, consumer);
    }

    public static Action<Institution> sendTrades() {
        return action(institution -> {
            if (institution.random.nextDouble() > institution.getGlobals().tradeRate) {
                FilteredLinks<Links.TradeLink> links = institution.getLinks(Links.TradeLink.class);

                Links.TradeLink selected = links.get(institution.random.nextInt(links.size()));

                institution.send(Messages.ForwardTrade.class, (msg) -> msg.from = institution).to(selected.getTo());

                //sends a message to the selected institution containing itself so the selected institution can make the trade
            }
        });
    }

    public static Action<Institution> makeTrades() {
        return action(institution -> {
            List<Institution> fromList = institution.getMessagesOfType(Messages.ForwardTrade.class).stream().map(link -> link.from).collect(Collectors.toList());
            for (Institution inst : fromList) {
                Institution floating;
                Institution fixed;
                if (institution.random.nextBoolean()) {
                    floating = inst;
                    fixed = institution;
                } else {
                    floating = institution;
                    fixed = inst;
                }
                AssetType fixedAsset = fixed.getRandomTradedAsset();
                AssetType floatingAsset = floating.getRandomTradedAsset();
                long startTick = institution.getContext().getTick();
                // between 1 to 10 years - check
                long endTick = startTick + (int) (institution.random.nextGaussian() * 10) + 5;
                Forward forwardToAdd = new Forward(fixedAsset, floatingAsset, startTick, endTick, 0.05);
                institution.addDerivativeToPortfolio(forwardToAdd);
                inst.addDerivativeToPortfolio(forwardToAdd);
            }
            institution.getLinks(Links.MarketLink.class).send(Messages.MarketPriceChange.class);
        });
    }

    public static Action<Institution> calculateCva() {
        return action(institution -> {
            // find the longest time in the portfolio
            if (institution.portfolio.isEmpty()) {
                institution.cvaPercent = 0;
            } else {
                long last = Collections.max(institution.portfolio, Comparator.comparingLong(derivative -> derivative.endTick)).endTick;
                long currentTick = institution.getContext().getTick();
                long ticksPerStep = institution.getGlobals().ticksPerStep;
                double cvaSum = 0;
                for (Derivative derivative : institution.portfolio) {

                    if (derivative.endTick >= currentTick) {
                        for (long i = currentTick; i < last; i += ticksPerStep) {
                            cvaSum += derivative.getExpectedExposure(i) * derivative.getDefaultProb(i, institution.getGlobals().hazardRate, ticksPerStep) * derivative.getDiscountFactor(i);
                        }
                    }

                }
                institution.cvaPercent = (1 - institution.getGlobals().recoveryRate) * cvaSum;
            }
        });
    }

    @Override
    public void init() {
        super.init();
        portfolio = new ArrayList<>();
        random = this.getPrng().generator;
        AssetType[] assetTypes = AssetType.values();
        soldTypes = new ArrayList<>(Collections.singleton(assetTypes[random.nextInt(assetTypes.length)]));
        // later on i will probably add better logic for assigning companies the assets they trade but for now
        // each company is randomly assigned an asset


    }

    public void addDerivativeToPortfolio(Derivative derivative) {
        portfolio.add(derivative);
    }

    public AssetType getRandomTradedAsset() {
        return soldTypes.get(random.nextInt(soldTypes.size()));
    }
}
