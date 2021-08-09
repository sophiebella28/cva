package market;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

public class CDSDesk extends Agent<MarketModel.Globals> {

    //public Portfolio portfolio = new Portfolio();
    // im so tired im gonna fix this tomorrow
    @Variable
    public double totalValue;

    @Override
    public void init() {
        super.init();
    }
    private static Action<CDSDesk> action(SerializableConsumer<CDSDesk> consumer) {
        return Action.create(CDSDesk.class, consumer);
    }
}
