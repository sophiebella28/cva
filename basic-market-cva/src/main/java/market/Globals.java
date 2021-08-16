package market;

import simudyne.core.abm.GlobalState;
import simudyne.core.annotations.Input;

import java.util.Random;

public class Globals extends GlobalState {
    @Input(name = "Recovery Rate")
    public double recoveryRate = 0.4;

    @Input(name = "Number of Institutions")
    public int nmInstitutions = 5;

    @Input(name = "Number of Momentum Institutions")
    public int nmMomInstitutions = 1;

    @Input(name = "Trade Rate")
    public double tradeRate = 0.5;

    @Input(name = "Hazard rate")
    public double hazardRate = 0.083;
    // todo calculate this instead of taking it as an input

    @Input(name = "Fraction of a year per tick")
    public double timeStep = 0.25;

    public double mean = 0.33 * timeStep;

    public double volatility = Math.sqrt(Math.pow(0.323,2) * timeStep);

    @Input(name = "Volatility of Information Signal")
    public double volatilityInfo = 0.001;

    public double informationSignal = new Random().nextGaussian() * volatilityInfo;

    @Input(name = "Momentum: Short Term Average")
    public long shortTermAverage = 7;

    @Input(name = "Momentum: Long Term Average")
    public long longTermAverage = 21;

    @Input(name = "Custom Trader Activity")
    public double traderActivity = 0.1;

    public double time = 0;

    @Input(name = "Interest Rate On CDS")
    public double cdsInterestRate = 0.01;
}
