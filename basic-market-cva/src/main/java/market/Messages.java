package market;

import simudyne.core.graph.Message;

public class Messages {
  public static class ForwardTrade extends Message {
    public Institution from;
  }

  public static class MarketPriceChange extends Message {
  }

  public static class CvaUpdate extends Message {
  }
}
