package market;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import simudyne.core.abm.Agent;

import java.util.Arrays;
import java.util.HashMap;

public class CallOption extends Derivative {
    AssetType assetType;

    Trader buyer;
    Trader seller;
    int amountOfAsset = 0;
    double agreedValue = 0;


    public CallOption(long startTick, long endTick, double discountFactor, Trader buyer, Trader seller, int amountOfAsset, AssetType assetType) {
        super(startTick, endTick, discountFactor);
        this.buyer = buyer;
        this.seller = seller;
        this.amountOfAsset = amountOfAsset;
        this.assetType = assetType;
        calculateStartingValue(assetType.getPrice());
    }

    @Override
    protected void calculateStartingValue(double stockPrice) {
        agreedValue = stockPrice;
    }


    // as the purchaser can decide not to exercise the call option, it can never have positive exposure to them
    // so only ever has exposure to the buyer
    @Override
    protected double uniqueExposureCalculation(double price, Trader trader) {
        if (trader == buyer) {
            return price - agreedValue;
        }
        return 0.0;
    }

    // uses option pricing formulae to find the current values
    @Override
    public double getCurrentValue(double currentTick, double timeStep, double interestRate, double stockVolatility, Trader owner) {
        double stockPrice = assetType.getPrice();
        double currentTime = (currentTick - startTick) * timeStep;
        double sigmaRootT = stockVolatility * Math.sqrt(currentTime);
        double d1 = (Math.log(stockPrice / agreedValue) + (interestRate + Math.pow(stockVolatility, 2) / 2) * currentTime) / (sigmaRootT);
        double d2 = d1 - sigmaRootT;
        NormalDistribution normal = new NormalDistribution();
        double c = stockPrice * normal.cumulativeProbability(d1) - agreedValue * Math.exp(-interestRate * currentTime) * normal.cumulativeProbability(d2);
        return (owner == seller) ? c * amountOfAsset : -c * amountOfAsset;
    }

    // returns expected exposure at the given time by reading it from the map
    @Override
    protected double getExpectedExposure(long atTick, double timeStep) {
        return expectedExposure.getOrDefault(atTick * timeStep, 0.0);
    }

    @Override
    protected Trader getCounterparty(Trader current) {
        if (current == buyer) {
            return seller;
        }
        return buyer;
    }
}
