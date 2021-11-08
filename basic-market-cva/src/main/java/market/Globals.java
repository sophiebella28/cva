package market;

import simudyne.core.abm.GlobalState;
import simudyne.core.annotations.Input;

import java.util.Random;

public class Globals extends GlobalState {
    // the average percentage of money that is recovered when a company defaults
    @Input(name = "Recovery Rate")
    public double recoveryRate = 0.4;

    // number of noise institutions
    @Input(name = "Number of Institutions")
    public int nmInstitutions = 5;

    // number of momentum institutions
    @Input(name = "Number of Momentum Institutions")
    public int nmMomInstitutions = 1;

    // hazard rate - should be calculated instead of taking as an input
    @Input(name = "Hazard rate")
    public double hazardRate = 0.083;

    // fraction of a year per tick - this also means that the time on the simudyne console isnt accurate
    @Input(name = "Fraction of a year per tick")
    public double timeStep = 0.25;

    // mean of the stock prices
    public double mean = 0.33 * timeStep;

    // volatility of the stock prices
    public double volatility = Math.sqrt(Math.pow(0.323,2) * timeStep);

    // volatility of the information signal
    @Input(name = "Volatility of Information Signal")
    public double volatilityInfo = 0.001;

    // lambda used in price change calculations
    @Input(name = "Lambda")
    public double lambda = 8;

    // random number for the information signal
    public double informationSignal = new Random().nextGaussian() * volatilityInfo;

    // time period for short term average
    @Input(name = "Momentum: Short Term Average")
    public long shortTermAverage = 7;

    // time period for long term average
    @Input(name = "Momentum: Long Term Average")
    public long longTermAverage = 21;

    // rate at which traders are active
    @Input(name = "Custom Trader Activity")
    public double traderActivity = 0.1;

    // variable that keeps track of time
    public double time = 0;

    // percentile at which to sample var
    @Input(name = "Level of Var")
    public double varLevel = 0.99;

    // three bools to determine hedging strategy
    // to pick a strategy, make sure the other two are false
    @Input(name = "Add On CDS Hedging Strategy")
    public boolean addOnHedge = false;

    @Input(name = "Every Tick Hedging Strategy (default)")
    public boolean everyTickHedge = true;

    @Input(name = "Run Out Hedging Strategy")
    public boolean runOutHedge = false;

    // amount of money the traders start with
    @Input(name = "Starting Money")
    public double startingMoney = 200;

    // amount of assets the pricing desk starts with
    @Input(name = "Central Starting Assets")
    public double centralStartingAssets = 100;

    // amount of assets the institutions start with
    @Input(name = "Institution Starting Assets")
    public double instStartingAssets = 10;

    public HedgingStrategy hedgingStrategy = HedgingStrategy.EVERY;


}

