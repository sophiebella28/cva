package market;

import org.apache.commons.math3.random.RandomGenerator;
import simudyne.core.abm.Agent;

import java.util.Arrays;
import java.util.HashMap;

public class Forward extends Derivative {
    AssetType assetType;
    Trader floating;
    Trader fixed;
    int amountOfAsset = 0;
    double agreedValue = 0;
    // todo: research whether forwards have interest rates

    public Forward(Trader fixed, Trader floating, long startTick, long endTick, double discountFactor, AssetType assetType, int amountOfAsset, double timeStep) {
        super(startTick, endTick, discountFactor);
        this.fixed = fixed;
        this.floating = floating;
        this.assetType = assetType;
        this.amountOfAsset = amountOfAsset;
        calculateStartingValue(assetType.getPrice());
    }


    @Override
    protected double uniqueExposureCalculation(double price, Trader trader) {
        //System.out.println("Agreed value is " + agreedValue);
        //System.out.println("Price is " + price);
        if (trader == floating) {
            return agreedValue - price;

        } else {
            return price - agreedValue;
        }
    }

    @Override
    public double getCurrentValue(double currentTick, double timeStep, double interestRate, double stockVolatility, Trader owner) {
        double stockPrice = assetType.getPrice();
        double f = (stockPrice - agreedValue) * Math.exp(-interestRate * ((currentTick - startTick) * timeStep));

        return (owner == fixed) ? f * agreedValue : -f * agreedValue;
    }

    @Override
    public double getAgreedValue() {
        return agreedValue;
    }


    @Override
    protected double getExpectedExposure(long atTick, double timeStep) {
        return expectedExposure.getOrDefault(atTick * timeStep, 0.0);
    }



    @Override
    protected void calculateStartingValue(double stockPrice) {
        agreedValue = stockPrice;
    }

    @Override
    protected Trader getCounterparty(Trader current) {
        if (current == floating) {
            return fixed;
        }
        return floating;

    }
}
