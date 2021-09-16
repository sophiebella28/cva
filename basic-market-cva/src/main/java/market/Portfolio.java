package market;

import java.util.ArrayList;
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

    public void add(List<CDS> cdss) {
        hedgingList.addAll(cdss);
    }



}
