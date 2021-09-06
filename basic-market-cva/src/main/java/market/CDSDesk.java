package market;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CDSDesk extends Agent<Globals> {

    public Portfolio portfolio = new Portfolio();
    @Variable
    public double totalMoney;

    public static Action<CDSDesk> createHedges() {
        return action(cdsDesk -> {
                    List<CDS> hedgingList = cdsDesk.getMessagesOfType(Messages.BuyCDS.class).stream().map(link -> link.tobuy).collect(Collectors.toList());
                    hedgingList.forEach(cds -> cds.setDesk(cdsDesk));
                    cdsDesk.portfolio.add(hedgingList);
                    double totalValueChange = cdsDesk.getMessagesOfType(Messages.ChangeValue.class).stream().map(link -> link.valueChange).reduce(0.0, Double::sum);
                    cdsDesk.totalMoney += totalValueChange;
                    //don't think theres anything else that needs to be done here
                }

        );
    }

    public static Action<CDSDesk> updateValues() {
        return action(cdsDesk -> {
                    double totalValueChange = cdsDesk.getMessagesOfType(Messages.ChangeValue.class).stream().map(link -> link.valueChange).reduce(0.0, Double::sum);
                    cdsDesk.totalMoney += totalValueChange;
                    //don't think theres anything else that needs to be done here
                }

        );
    }

    public static Action<CDSDesk> evaluateCds() {
        return action(cdsDesk -> {
                    List<Trader> defaultList = new ArrayList<>();
                    cdsDesk.getMessagesOfType(Messages.DefaultList.class).forEach(x -> defaultList.addAll(x.defaulted));
                    List<CDS> completed = new ArrayList<>();
                    for (CDS cds : cdsDesk.portfolio.hedgingList) {
                        if (defaultList.contains(cds.protectionOn)) {
                            cdsDesk.send(Messages.ChangeValue.class, (msg) -> msg.valueChange = (1 - cdsDesk.getGlobals().recoveryRate) * cds.notional).to(cds.buyer.getID());
                            cdsDesk.totalMoney -= (1 - cdsDesk.getGlobals().recoveryRate) * cds.notional;
                            completed.add(cds);
                        }
                    }
                    cdsDesk.portfolio.hedgingList.removeAll(completed);
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
