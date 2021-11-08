package market;

import org.apache.commons.math3.random.RandomGenerator;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

import java.util.*;
import java.util.stream.Collectors;

public abstract class Trader extends Agent<Globals> {
    public Portfolio portfolio;

    // keeps track of the total money of each trader
    @Variable
    public double totalMoney;

    // keeps track of the total number of assets of each trader
    @Variable
    public double numberOfAssets;

    // the total value of each trader as calculated by the relevant equations
    @Variable
    public double totalValue;

    // the cva of the trader's current portfolio (not as a percentage)
    @Variable
    public double cva;

    // the current value at risk of the traders portfolio (not as a percentage)
    @Variable
    public double valueAtRisk = 0;

    // helper function for returning an action
    private static Action<Trader> action(SerializableConsumer<Trader> consumer) {
        return Action.create(Trader.class, consumer);
    }


    // adds the given derivative to the portfolio
    public void addDerivativeToPortfolio(Derivative derivative) {
        portfolio.add(derivative);
    }

    // calculates the value of the portfolio by valuating each of the derivatives individually and adding together
    public double calculatePortfolioValue() {
        double total = 0.0;
        for (Derivative derivative : portfolio.derivativeList) {
            if (getContext().getTick() <= derivative.endTick) {
                total += derivative.getCurrentValue(getContext().getTick(), getGlobals().timeStep, 0.05, getGlobals().volatility, this);
            }
        }
        return total;
        // you need to be careful with this - sometimes values can't be added together like this depending on the equations used
    }


    // updates the cva field on the current trader
    void updateCva(long currentTick, double stockPrice) {
        double timeStep = getGlobals().timeStep;
        double hazardRate = getGlobals().hazardRate;
        double recoveryRate = getGlobals().recoveryRate;
        RandomGenerator generator = getPrng().generator;
        List<Derivative> derivativeList = portfolio.derivativeList;

        if (portfolio.derivativeIsEmpty()) {
            cva = 0;
        } else {
            double cvaSum = 0;
            // iterates over the derivatives in the portfolio and performs numerical integration on each of those
            // derivatives as per the cva equation
            for (Derivative derivative : derivativeList) {
                if (derivative.endTick >= currentTick) {
                    derivative.calculateExpectedExposure(derivative.endTick - currentTick, stockPrice, generator, this, getGlobals());
                    long duration = derivative.endTick - currentTick;
                    double totalExpectedExposure = 0.0;
                    for (long i = 0; i < duration; i++) {

                        double expectedExposure = derivative.getExpectedExposure(i, timeStep);
                        totalExpectedExposure += expectedExposure;

                        double defaultProb = derivative.getDefaultProb(i, hazardRate, timeStep);
                        // the model could be improved by changing ho the default probability is calculated
                        // or even having it sampled from real world data instead of this estimate

                        double discountFactor = derivative.getDiscountFactor(i, timeStep);

                        double cvaIndividual = expectedExposure * defaultProb * discountFactor;

                        cvaSum += cvaIndividual;

                    }
                    derivative.setHedgingNotional(totalExpectedExposure, currentTick);
                }
            }
            cva = (1 - recoveryRate) * cvaSum;
        }
    }

    // one of the hedging strategies that was investigated - purchases cds protection when the previous one ran out
    private void runOutHedging(long currentTick) {
        // stream hedging list and find all counterparties we have cds on
        // stream derivative list and find all counterparties we have contracts with
        // if any have run out then work out how much we want to buy
        List<Trader> protectionOnList = portfolio.hedgingList.stream().filter(cds -> cds.endTick >= currentTick).map(cds -> cds.protectionOn).collect(Collectors.toList());
        List<Trader> contractsWithList = portfolio.derivativeList.stream().filter(derivative -> derivative.endTick >= currentTick).map(derivative -> derivative.getCounterparty(this)).collect(Collectors.toList());
        List<Trader> partiesToBuyCDS = new ArrayList<>(contractsWithList);
        partiesToBuyCDS.removeAll(protectionOnList);
        Map<Trader, Double> hedgeMap = new HashMap<>();

        for (Derivative derivative : portfolio.derivativeList) {
            Trader counterparty = derivative.getCounterparty(this);
            if (derivative.hedgingNotional > 0 && partiesToBuyCDS.contains(counterparty)) {
                if (hedgeMap.containsKey(counterparty)) {
                    hedgeMap.put(counterparty, hedgeMap.get(counterparty) + derivative.hedgingNotional);
                } else {
                    hedgeMap.put(counterparty, derivative.hedgingNotional);
                }
            }
        }
        for (Map.Entry<Trader, Double> entry : hedgeMap.entrySet()) {
            Trader counterparty = entry.getKey();
            double notional = entry.getValue();
            int cdsLengthInYears = getPrng().generator.nextInt(10);
            long cdsLengthInTicks = Math.round(cdsLengthInYears / getGlobals().timeStep);
            purchaseCDS(currentTick, cdsLengthInTicks, notional, counterparty);
        }
    }

    // another investigated hedging strategy - purchases cds protection to the maximum amount that is needed
    private void addOnHedging(long currentTick) {
        Map<Trader, Double> hedgeMap = new HashMap<>();
        // creates the hedgemap which is a map from counterparty to the total amount that should be hedged against them
        for (Derivative derivative : portfolio.derivativeList) {
            Trader counterparty = derivative.getCounterparty(this);
            if (derivative.hedgingNotional > 0) {
                if (hedgeMap.containsKey(counterparty)) {
                    hedgeMap.put(counterparty, hedgeMap.get(counterparty) + derivative.hedgingNotional);
                } else {
                    hedgeMap.put(counterparty, derivative.hedgingNotional);
                }
            }
        }
        // cycles through the hedging map and applies the add on strategy
        for (Map.Entry<Trader, Double> entry : hedgeMap.entrySet()) {
            Trader counterparty = entry.getKey();
            double notional = entry.getValue();
            double currentProtection = portfolio.hedgingList.stream().filter(cds -> cds.protectionOn == counterparty && cds.endTick >= currentTick).map(cds -> cds.notional).reduce(0.0, Double::sum);
            if (notional > currentProtection) {
                int cdsLengthInYears = getPrng().generator.nextInt(10);
                long cdsLengthInTicks = Math.round(cdsLengthInYears / getGlobals().timeStep);
                purchaseCDS(currentTick, cdsLengthInTicks, notional - currentProtection, counterparty);
            }
        }
    }

    // a third strategy where cds protection is purchased every tick with a duration of 1 tick
    private void everyTickHedging(long currentTick) {
        for (Derivative derivative : portfolio.derivativeList) {
            Trader counterparty = derivative.getCounterparty(this);
            if (derivative.hedgingNotional > 0) {
                purchaseCDS(currentTick, 1, derivative.hedgingNotional, counterparty);
            }
        }
    }

    // a helper function for purchasing cds - sends a message to the cds desk with the cds that has been purchased
    private void purchaseCDS(long currentTick, long cdsLength, Double notional, Trader counterparty) {
        CDS cds = new CDS(this, currentTick, currentTick + cdsLength, notional, 0.01, counterparty);
        getLinks(Links.HedgingLink.class).send(Messages.BuyCDS.class, (msg, link) -> {
            msg.tobuy = cds;
        });
        portfolio.add(cds);
    }

    // picks the hedging strategy that will be used
    public static Action<Trader> sendHedges(long currentTick) {
        return action(
                trader -> {
                    //System.out.println(trader.getGlobals().hedgingStrategy);
                    switch (trader.getGlobals().hedgingStrategy) {
                        case EVERY:
                            trader.everyTickHedging(currentTick);
                            break;
                        case RUNOUT:
                            trader.runOutHedging(currentTick);
                            break;
                        case ADDON:
                            trader.addOnHedging(currentTick);
                            break;
                    }
                });
    }

    // calculates the var of the entire portfolio
    void calculateVaR(long currentTick, double stockPrice) {
        List<Derivative> derivativeList = portfolio.derivativeList;
        int timePeriodOfVar = 1; // can be changed - could be put into the console
        valueAtRisk = 0;
        // calculates the predicted asset prices over the time period using monte carlo methods
        double[][] prices = monteCarlo(timePeriodOfVar, stockPrice, getPrng().generator, getGlobals(), 1000);
        if (portfolio.derivativeIsEmpty()) {
            valueAtRisk = 0;
        } else {
            Map<Trader, List<Derivative>> counterToDerivativeMap = new HashMap<>();
            // loops over the portfolio and total up the derivatives held with each counterparty
            for (Derivative derivative : derivativeList) {
                if (currentTick <= derivative.endTick) {
                    Trader counterparty = derivative.getCounterparty(this);
                    if (!counterToDerivativeMap.containsKey(counterparty)) {
                        counterToDerivativeMap.put(counterparty, new ArrayList<>(
                                Collections.singletonList(derivative)));
                    } else {
                        counterToDerivativeMap.get(counterparty).add(derivative);
                        //todo: check that this works
                    }
                }
            }

            int finalTick = prices.length - 1;
            for (Map.Entry<Trader, List<Derivative>> entry : counterToDerivativeMap.entrySet()) {
                List<Double> finalTimeStep = new ArrayList<>();
                Trader counterparty = entry.getKey();
                List<Derivative> counterPartyDerivatives = entry.getValue();
                List<CDS> hedgingList = portfolio.hedgingList;
                // makes a list of all the cds protection held against the given counterparty
                List<CDS> counterpartyHedges = hedgingList.stream().filter(cds -> cds.protectionOn == counterparty && cds.endTick >= currentTick).collect(Collectors.toList());
                // totals up the amount that would be received if default did occur
                double totalProtected = counterpartyHedges.stream().map(cds -> cds.notional * (1 - getGlobals().recoveryRate)).reduce(0.0, Double::sum);
                // loops over the predicted asset prices at all future timesteps
                for (int j = 0; j < prices[0].length; j++) {
                    int finalJ = j;
                    // calculates the exposure of all derivatives held with a certain counterparty and then adds up all of them with value > 0
                    double totalExposure = counterPartyDerivatives.stream().map(der -> der.uniqueExposureCalculation(prices[finalTick][finalJ], this)).filter(val -> val > 0).reduce(0.0, Double::sum);
                    totalExposure = (totalExposure - totalProtected > 0) ? totalExposure - totalProtected : 0;
                    finalTimeStep.add(totalExposure);
                }
                finalTimeStep.sort(Double::compareTo);
                valueAtRisk += finalTimeStep.get((int) Math.floor(finalTimeStep.size() * getGlobals().varLevel));
                //it would be easier to interpret the results if this was a percentage
            }

        }
    }

    // performs monte carlo methods for the given duration, calculating the stock prices in the future
    protected double[][] monteCarlo(int duration, double stockPrice, RandomGenerator generator, Globals globals, int noOfTrials) {
        double[][] stockPrices = new double[duration][noOfTrials];
        double timeStep = globals.timeStep;
        double mu = globals.mean;
        double sigma = globals.volatility;
        for (int i = 0; i < noOfTrials; i++) {
            // an improvement to the model can be made by taking an uneven distribution of timesteps (eg just sampling
            // at times when a derivative runs out)
            double sampleStockPrice = stockPrice;
            for (int j = 0; j < duration; j++) {
                double stockChange = timeStep * mu * sampleStockPrice + sigma * Math.sqrt(timeStep) * sampleStockPrice * generator.nextGaussian();
                sampleStockPrice += stockChange;
                stockPrices[j][i] = sampleStockPrice;
            }
        }
        return stockPrices;
    }

    // receives message to update cva and var and calls the appropriate functions
    public static Action<Trader> updateFields(long currentTick) {
        return action(
                trader -> {
                    double stockPrice = trader.getMessageOfType(Messages.UpdateFields.class).price;
                    trader.updateCva(currentTick, stockPrice);
                    trader.calculateVaR(currentTick, stockPrice);
                    if (trader instanceof InstitutionBase) {
                        ((InstitutionBase) trader).updateInfo();
                    }
                });
    }

    // receives message to update money values and asset values and does that accordingly
    public static Action<Trader> updateValues() {
        return action(
                trader -> {
                    double totalValueChange = trader.getMessagesOfType(Messages.ChangeValue.class).stream().map(link -> link.valueChange).reduce(0.0, Double::sum);
                    int totalAssetChange = trader.getMessagesOfType(Messages.ChangeAssets.class).stream().map(link -> link.noOfAssets).reduce(0, Integer::sum);
                    trader.totalMoney += totalValueChange;
                    trader.numberOfAssets += totalAssetChange;
                    trader.totalValue = trader.calculatePortfolioValue();
                });
    }

    // receives message to calculate the gains from any companies that defaulted and updates values accordingly
    public static Action<Trader> cdsGains() {
        return action(
                trader -> {
                    List<CDS> cdss = trader.getMessagesOfType(Messages.CDSMessage.class).stream().map(link -> link.cds).collect(Collectors.toList());
                    trader.portfolio.hedgingList.removeAll(cdss);
                    double totalValueChange = cdss.stream().map(cds -> cds.notional * (1 - trader.getGlobals().recoveryRate)).reduce(0.0, Double::sum);
                    trader.totalMoney += totalValueChange;
                    trader.totalValue = trader.calculatePortfolioValue();
                });
    }


}
