package market;

public class CDS {
    CDSDesk desk; //todo: i really need to find a better solution to this
    Trader buyer;
    double notional;
    long startTick;
    public long endTick;
    double yearly;
    Trader protectionOn;

    public CDS(Trader buyer, long startTick, long endTick, double notional, double yearly, Trader protectionOn) {
        this.buyer = buyer;
        this.startTick = startTick;
        this.endTick = endTick;
        this.notional = notional;
        this.yearly = yearly;
        this.protectionOn = protectionOn;
    }

    public void setDesk(CDSDesk desk) {
        this.desk = desk;
    }
}
