package market;

import java.util.HashMap;

public class CDS {
    CDSDesk desk;
    Trader buyer;
    double faceValue;
    long startTick;
    public long endTick;
    double interestRate;

    public CDS(Trader buyer, long startTick, long endTick, double faceValue, double interestRate, CDSDesk desk) {

        this.buyer = buyer;
        this.startTick = startTick;
        this.endTick = endTick;
        this.faceValue = faceValue;
        this.interestRate = interestRate;
        this.desk = desk;
    }
}
