package market;

import org.apache.commons.math3.random.RandomGenerator;
import org.json4s.JsonUtil;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public abstract class Trader extends Agent<Globals> {
    public Portfolio portfolio;

    @Variable
    public double totalMoney;
    @Variable
    public double numberOfAssets;

    @Variable
    public double totalValue;

    @Variable
    public double cvaPercent;

    @Variable
    public double valueAtRisk = 0;


    private static Action<Trader> action(SerializableConsumer<Trader> consumer) {
        return Action.create(Trader.class, consumer);
    }


    public void addDerivativeToPortfolio(Derivative derivative) {
        portfolio.add(derivative);
    }

    public double calculatePortfolioValue() {
        double total = 0.0;
        for (Derivative derivative : portfolio.derivativeList) {
            if (getContext().getTick() <= derivative.endTick) {
                total += derivative.getCurrentValue(getContext().getTick(), getGlobals().timeStep, 0.05, getGlobals().volatility, this);
            }
        }
        return total; //I think this is how it works but i should double check this
    }

    void updateCva(long currentTick, double stockPrice) {
        double timeStep = getGlobals().timeStep;
        double hazardRate = getGlobals().hazardRate;
        double recoveryRate = getGlobals().recoveryRate;
        RandomGenerator generator = getPrng().generator;
        List<Derivative> derivativeList = portfolio.derivativeList;

        if (portfolio.derivativeIsEmpty()) {
            cvaPercent = 0;
        } else {
            double cvaSum = 0;
            for (Derivative derivative : derivativeList) {
                if (derivative.endTick >= currentTick) {
                    derivative.calculateExpectedExposure(derivative.endTick - currentTick, stockPrice, generator, this, getGlobals());
                    long duration = derivative.endTick - currentTick;
                    double totalExpectedExposure = 0.0;
                    for (long i = 0; i < duration; i++) {

                        double expectedExposure = derivative.getExpectedExposure(i, timeStep);
                        totalExpectedExposure += expectedExposure;

                        double defaultProb = derivative.getDefaultProb(i, hazardRate, timeStep);
                        // look i just think perhaps the default probability shouldn't be tied to the derivative and should
                        // instead be calculated based on the institution
                        // but i dont really want to solve this problem

                        double discountFactor = derivative.getDiscountFactor(i, timeStep);

                        double cvaIndividual = expectedExposure * defaultProb * discountFactor;

                        cvaSum += cvaIndividual;

                    }

                    
                    derivative.setHedgingNotional(totalExpectedExposure,currentTick);



                }
            }
            cvaPercent = (1 - recoveryRate) * cvaSum;
        }
    }

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
            long cdsLengthInTicks =  Math.round(cdsLengthInYears/getGlobals().timeStep);
            purchaseCDS(currentTick, cdsLengthInTicks,notional, counterparty);
        }
    }

    private void addOnHedging(long currentTick) {
        Map<Trader, Double> hedgeMap = new HashMap<>();
        for (Derivative derivative : portfolio.derivativeList) {
            Trader counterparty = derivative.getCounterparty(this);
            if (derivative.hedgingNotional > 0 ) {
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
            double currentProtection = portfolio.hedgingList.stream().filter(cds -> cds.protectionOn == counterparty && cds.endTick >= currentTick).map(cds -> cds.notional).reduce(0.0,Double::sum);
            if(notional > currentProtection) {
                int cdsLengthInYears = getPrng().generator.nextInt(10);
                long cdsLengthInTicks =  Math.round(cdsLengthInYears/getGlobals().timeStep);
                purchaseCDS(currentTick,cdsLengthInTicks,notional - currentProtection, counterparty);
            }
        }
    }

    private void everyTickHedging(long currentTick) {
        for (Derivative derivative : portfolio.derivativeList) {
            Trader counterparty = derivative.getCounterparty(this);
            if (derivative.hedgingNotional > 0 ) {
                purchaseCDS(currentTick, 1,derivative.hedgingNotional, counterparty);
            }
        }
    }

    private void purchaseCDS(long currentTick, long cdsLength, Double notional, Trader counterparty) {
        CDS cds = new CDS(this, currentTick, currentTick + cdsLength, notional, 0.01, counterparty);
        getLinks(Links.HedgingLink.class).send(Messages.BuyCDS.class, (msg, link) -> {
            msg.tobuy = cds;
        });
        portfolio.add(cds);
    }

    public static Action<Trader> sendHedges(long currentTick) {
        return action(
                trader -> {
                    //System.out.println(trader.getGlobals().hedgingStrategy);
                    switch(trader.getGlobals().hedgingStrategy) {
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

    void calculateVaR(long currentTick, double stockPrice) {
        List<Derivative> derivativeList = portfolio.derivativeList;
        int timePeriodOfVar = 1;
        valueAtRisk = 0;
        double[][] prices = monteCarlo(timePeriodOfVar, stockPrice, getPrng().generator, getGlobals(), 1000);
        if (portfolio.derivativeIsEmpty()) {
            valueAtRisk = 0;
        } else {
            Map<Trader, List<Derivative>> counterToDerivativeMap = new HashMap<>();
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
                List<CDS> counterpartyHedges = hedgingList.stream().filter(cds -> cds.protectionOn == counterparty && cds.endTick >= currentTick).collect(Collectors.toList());
                double totalProtected = counterpartyHedges.stream().map(cds -> cds.notional * (1 - getGlobals().recoveryRate)).reduce(0.0, Double::sum);
                //System.out.println("Counterparty is " + counterparty.getID());
                //System.out.println("All derivatives are " + counterPartyDerivatives);
                //System.out.println("All hedges are " + counterpartyHedges);
                //System.out.println("Total protected is " + totalProtected);
                double counter = 0;
                for (int j = 0; j < prices[0].length; j++) {
                    int finalJ = j;
                    double totalExposure = counterPartyDerivatives.stream().map(der -> der.uniqueExposureCalculation(prices[finalTick][finalJ], this)).filter(val -> val > 0).reduce(0.0, Double::sum);
                    //System.out.println("Total exposure: " + totalExposure);
                    counter += totalExposure;
                    totalExposure = (totalExposure - totalProtected > 0) ? totalExposure - totalProtected : 0;
                    finalTimeStep.add(totalExposure);
                }
                //System.out.println("Exposure Counter In VaR:" + counter);
                finalTimeStep.sort(Double::compareTo);
                //todo: check that this sorts in the right direction
                //System.out.println("99th percentile " + finalTimeStep.get((int) Math.floor(finalTimeStep.size() * getGlobals().varLevel) - 1));
                //System.out.println("0th percentile " + finalTimeStep.get(0));
                valueAtRisk += finalTimeStep.get((int) Math.floor(finalTimeStep.size() * getGlobals().varLevel));
                //todo: this should be a percentage but i cannot figure out what to divide by
            }

        }
    }

    protected double[][] monteCarlo(int duration, double stockPrice, RandomGenerator generator, Globals globals, int noOfTrials) {
        double[][] stockPrices = new double[duration][noOfTrials];
        //System.out.println("duration " + duration);
        //System.out.println("no of trials " + noOfTrials);
        //System.out.println("current price " + stockPrice);
        double timeStep = globals.timeStep;
        double mu = globals.mean;
        double sigma = globals.volatility;
        for (int i = 0; i < noOfTrials; i++) {
            // todo: consider taking an uneven sample of time points
            double sampleStockPrice = stockPrice;
            //System.out.println("next trial");
            for (int j = 0; j < duration; j++) {
                double stockChange = timeStep * mu * sampleStockPrice + sigma * Math.sqrt(timeStep) * sampleStockPrice * generator.nextGaussian();

                sampleStockPrice += stockChange;
                stockPrices[j][i] = sampleStockPrice;
                //System.out.println("generated price " + sampleStockPrice);
            }
        }
        return stockPrices;
    }

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

    public static Action<Trader> updateValues() {
        return action(
                trader -> {
                    double totalValueChange = trader.getMessagesOfType(Messages.ChangeValue.class).stream().map(link -> link.valueChange).reduce(0.0, Double::sum);
                    int totalAssetChange = trader.getMessagesOfType(Messages.ChangeAssets.class).stream().map(link -> link.noOfAssets).reduce(0, Integer::sum);
                    trader.totalMoney += totalValueChange;
                    trader.numberOfAssets += totalAssetChange;

                    trader.totalValue = trader.calculatePortfolioValue();
                    // System.out.println("total value is " + trader.totalValue);
                    // am a bit concerned about negatives in the portfolio value
                    // also here calculate the VaR of the portfolio
                });
    }

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
