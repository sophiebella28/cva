package market;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

import java.util.List;
import java.util.stream.Collectors;

public class CDSDesk extends Agent<Globals> {

    public Portfolio portfolio = new Portfolio();
    @Variable
    public double totalValue;

    public static Action<CDSDesk> createHedges() {
        return action( cdsDesk -> {
                    List<CDS> hedgingList = cdsDesk.getMessagesOfType(Messages.BuyCDS.class).stream().map(link -> link.tobuy).collect(Collectors.toList());
                    cdsDesk.portfolio.add(hedgingList);
                    //don't think theres anything else that needs to be done here
                }

        );
    }

    @Override
    public void init() {
        super.init();
    }
    private static Action<CDSDesk> action(SerializableConsumer<CDSDesk> consumer) {
        return Action.create(CDSDesk.class, consumer);
    }
}
