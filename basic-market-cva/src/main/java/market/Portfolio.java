package market;

import org.apache.commons.math3.random.RandomGenerator;
import simudyne.core.abm.AgentBasedModel;
import simudyne.core.annotations.Variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Portfolio {

    List<Derivative> derivativeList;

    List<CDS> hedgingList;

    Trader owner;

    @Variable
    public double cvaPercent;

    @Variable
    public double totalValue;

    public Portfolio(Trader owner) {
        derivativeList = new ArrayList<>();
        hedgingList = new ArrayList<>();
        this.owner = owner;
    }


    public boolean derivativeIsEmpty() {
        return derivativeList.isEmpty();
    }

    public boolean hedgingIsEmpty() {
        return hedgingList.isEmpty();
    }

    public void add(Derivative derivative) {
        derivativeList.add(derivative);
    }

    public void add(CDS cds) {
        hedgingList.add(cds);
    }

    public double updateCva(long currentTick, double timeStep, double hazardRate, double recoveryRate, double meanRev, double volatility, double stockPrice, RandomGenerator generator) {
        // find the longest time in the portfolio
        if (derivativeIsEmpty()) {
            cvaPercent = 0;
        } else {
            long last = Collections.max(derivativeList, Comparator.comparingLong(derivative -> derivative.endTick)).endTick;
            double cvaSum = 0;
            for (Derivative derivative : derivativeList) {
                if (derivative.endTick >= currentTick) {
                    derivative.calculateExpectedExposure(derivative.endTick - currentTick, timeStep, stockPrice, generator, owner);
                    for (long i = 0; i < last - currentTick; i++) {

                        double expectedExposure = derivative.getExpectedExposure(i, timeStep);

                        double defaultProb = derivative.getDefaultProb(i, hazardRate, timeStep);

                        double discountFactor = derivative.getDiscountFactor(i, timeStep);

                        double cvaIndividual = expectedExposure * defaultProb * discountFactor;

                        cvaSum += cvaIndividual;

                    }
                }

            }
            cvaPercent = (1 - recoveryRate) * cvaSum;
        }
        return cvaPercent;
    }

    public void closeTrades(long currentTick) {

        for (Derivative derivative : derivativeList) {
            if (derivative.endTick == currentTick) {
                if (derivative instanceof Forward) {
                    Forward forward = (Forward) derivative;
                    Trader floating = forward.floating;
                    Trader fixed = forward.fixed;
                    double valueChange = forward.agreedValue * forward.amountOfAsset;
                    owner.send(Messages.ChangeValue.class, (msg) -> msg.valueChange = valueChange).to(fixed.getID());
                    owner.send(Messages.ChangeValue.class, (msg) -> msg.valueChange = -valueChange).to(floating.getID());

                    owner.send(Messages.ChangeAssets.class, (msg) -> msg.noOfAssets = -forward.amountOfAsset).to(fixed.getID());
                    owner.send(Messages.ChangeAssets.class, (msg) -> msg.noOfAssets = forward.amountOfAsset).to(floating.getID());

                    totalValue += forward.amountOfAsset * (forward.agreedValue - forward.assetType.getPrice());
                    // need a measure of whether or not this was actually lost idk
                }

            }
        }

        for (CDS cds : hedgingList) {
            if(currentTick == cds.startTick || (currentTick - cds.startTick ) % 12 == 0 ) {
                Trader trader = cds.buyer;
                CDSDesk desk = cds.desk;
                trader.send(Messages.ChangeValue.class, (msg) -> msg.valueChange = -cds.interestRate * cds.faceValue).to(desk.getID());
                desk.send(Messages.ChangeValue.class, (msg) -> msg.valueChange = cds.interestRate * cds.faceValue).to(trader.getID());
            }
        }

    }
}
