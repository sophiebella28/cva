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

    // receives a message with a cds in it and adds it to its portfolio and adds the money it receives to its total
    public static Action<CDSDesk> createHedges() {
        return action(cdsDesk -> {
                    List<CDS> hedgingList = cdsDesk.getMessagesOfType(Messages.BuyCDS.class).stream().map(link -> link.tobuy).collect(Collectors.toList());
                    hedgingList.forEach(cds -> cds.setDesk(cdsDesk));
                    cdsDesk.portfolio.add(hedgingList);
                    double totalValueChange = cdsDesk.getMessagesOfType(Messages.ChangeValue.class).stream().map(link -> link.valueChange).reduce(0.0, Double::sum);
                    cdsDesk.totalMoney += totalValueChange;
                }

        );
    }

    // adds on any money it makes from interest rates on cds
    public static Action<CDSDesk> updateValues() {
        return action(cdsDesk -> {
                    double totalValueChange = cdsDesk.getMessagesOfType(Messages.ChangeValue.class).stream().map(link -> link.valueChange).reduce(0.0, Double::sum);
                    cdsDesk.totalMoney += totalValueChange;
                }

        );
    }

    // cycles through the list of counterparties who have purchased cds protection and gives money to any who purchased
    // it on a counterparty that defaulted
    public static Action<CDSDesk> evaluateCds() {
        return action(cdsDesk -> {
                    List<Trader> defaultList = new ArrayList<>();
                    cdsDesk.getMessagesOfType(Messages.DefaultList.class).forEach(x -> defaultList.addAll(x.defaulted));
                    List<CDS> completed = new ArrayList<>();
                    for (CDS cds : cdsDesk.portfolio.hedgingList) {
                        if (defaultList.contains(cds.protectionOn) && cds.endTick <= cdsDesk.getContext().getTick()) {
                            cdsDesk.send(Messages.CDSMessage.class, (msg) -> msg.cds = cds).to(cds.buyer.getID());
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
