package market;

import java.util.ArrayList;
import java.util.List;

public class Portfolio {

    List<Derivative> derivativeList;

    List<CDS> hedgingList;

    // stores a list of derivatives and a list of hedges on those derivatives
    // currently there is one portfolio per counterparty but it could be interesting to add multiple for one trader
    public Portfolio() {
        derivativeList = new ArrayList<>();
        hedgingList = new ArrayList<>();
    }


    public boolean derivativeIsEmpty() {
        return derivativeList.isEmpty();
    }

    public void add(Derivative derivative) {
        derivativeList.add(derivative);
    }

    public void add(CDS cds) {
        hedgingList.add(cds);
    }

    public void add(List<CDS> cdss) {
        hedgingList.addAll(cdss);
    }



}
