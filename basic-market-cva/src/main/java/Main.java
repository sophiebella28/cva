import market.MarketModel;
import simudyne.nexus.Server;

public class Main {
  public static void main(String[] args) {
    Server.register("cva initial", MarketModel.class);
    Server.run();
  }
}
