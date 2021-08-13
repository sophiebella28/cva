package market;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

public class CDSDesk extends Agent<Globals> {

    public Portfolio portfolio = new Portfolio();
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
