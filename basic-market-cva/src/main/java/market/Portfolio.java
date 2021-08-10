package market;

import org.apache.commons.math3.random.RandomGenerator;
import simudyne.core.abm.Agent;
import simudyne.core.abm.AgentBasedModel;
import simudyne.core.annotations.Variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Portfolio {

    List<Derivative> derivativeList;

    List<CDS> hedgingList;

    
    public Portfolio() {
        derivativeList = new ArrayList<>();
        hedgingList = new ArrayList<>();
    }


    public boolean derivativeIsEmpty() {
        return derivativeList.isEmpty();
    }

    public boolean hedgingIsEmpty() {
        return hedgingList.isEmpty();
    }

    public void add(Derivative derivative) {
        derivativeList.add(derivative);
    }

    public void add(CDS cds) {
        hedgingList.add(cds);
    }




}
