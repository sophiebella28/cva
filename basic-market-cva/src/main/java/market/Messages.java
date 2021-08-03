package market;

import simudyne.core.graph.Message;

public class Messages {
  public static class ForwardFloatingTrade extends Message {
    public Trader from;
  }

  public static class ForwardFixedTrade extends Message {
    public Trader from;
  }

  public static class MarketPriceChange extends Message {
  }

  public static class UpdateFields extends Message {
    public double price;
    public double priceChange;
  }
}
