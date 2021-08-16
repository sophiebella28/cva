package market;

public class CDS {
    CDSDesk desk; //todo: i really need to find a better solution to this
    Trader buyer;
    double notional;
    long startTick;
    public long endTick;
    double upfront;
    double yearly;

    public CDS(Trader buyer, long startTick, long endTick, double notional, double yearly) {
        this.buyer = buyer;
        this.startTick = startTick;
        this.endTick = endTick;
        this.notional = notional;
        this.upfront = upfront;
        this.yearly = yearly;
    }

    public void setDesk(CDSDesk desk) {
        this.desk = desk;
    }
}
