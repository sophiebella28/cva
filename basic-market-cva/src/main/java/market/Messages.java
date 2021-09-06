package market;

import simudyne.core.graph.Message;

import java.util.List;

public class Messages {
  public static class ForwardFloatingTrade extends Message {
    public Trader from;
  }

  public static class ForwardFixedTrade extends Message {
    public Trader from;
  }

  public static class CallOptionSellTrade extends Message {
    public Trader from;
  }

  public static class CallOptionBuyTrade extends Message {
    public Trader from;
  }

  public static class ChangeAssets extends Message {
    public int noOfAssets;
  }

  public static class ChangeValue extends Message {
    public double valueChange;
  }

  public static class UpdateFields extends Message {
    public double price;
    public double priceChange;
  }

  public static class BuyCDS extends Message {
    public CDS tobuy;
  }

  public static class DefaultNotification extends Message {
    public InstitutionBase defaulted;
  }

  public static class DefaultList extends Message {
    public List<Trader> defaulted;
  }


}
