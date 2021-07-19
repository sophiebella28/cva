package marketTest;


import market.*;
import org.example.models.trading.Market;
import org.junit.Before;
import org.junit.Test;
import simudyne.core.abm.testkit.TestKit;
import simudyne.core.abm.testkit.TestResult;

import static org.junit.Assert.assertEquals;

public class SimpleForwardTest {
    private TestKit<MarketModel.Globals> testKit;
    private Institution institution;
    public static final int TARGET_LINK_ID = 1;

    @Before
    public void init() {
        testKit = TestKit.create(MarketModel.Globals.class);
        testKit.registerLinkTypes(Links.TradeLink.class);
        institution = testKit.addAgent(Institution.class);

        institution.addLink(TARGET_LINK_ID, Links.TradeLink.class);
        institution.addDerivativeToPortfolio(new TestForward(0,60,0.05, AssetType.ASSET1, AssetType.ASSET2));
    }

    @Test
    public void cvaCalculationShouldMatchSpreadsheet() {
        testKit.send(Messages.CvaUpdate.class).to(institution);

        TestResult testResult = testKit.testAction(institution,Institution.calculateCva(0));

        assertEquals(0.00085, institution.cvaPercent, 0.01);
    }
}
