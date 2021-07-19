package market;

public class TestForward extends Derivative {
    AssetType fixed;
    AssetType floating;
    double[] expectedExposure = {0, 0.000442362,
            0.000887097,
            0.00133259,
            0.001782321,
            0.002186876,
            0.002576927,
            0.00294142,
            0.003268674,
            0.003575898,
            0.003883797,
            0.004172228,
            0.004428329,
            0.004661285,
            0.0048755,
            0.005102192,
            0.005298187,
            0.00546938,
            0.0056104,
            0.005723982,
            0.005851551,
            0.005963814,
            0.006055151,
            0.006152814,
            0.006226428,
            0.006305369,
            0.006403824,
            0.006487927,
            0.006579945,
            0.006670714,
            0.006744675,
            0.00682092,
            0.006923945,
            0.007007093,
            0.007055737,
            0.007079111,
            0.007065438,
            0.007030129,
            0.006960285,
            0.006866579,
            0.006750006,
            0.00659278,
            0.006424959,
            0.00628187,
            0.006137424,
            0.006003639,
            0.005865807,
            0.005698803,
            0.00546355,
            0.005180551,
            0.004917385,
            0.00461607,
            0.004272367,
            0.003895103,
            0.003510019,
            0.003113342,
            0.002681086,
            0.002241451,
            0.001831805,
            0.00144316,
            0.001040363,
            0.000618761,
            0.000212151
    };

    public TestForward(long startTick, long endTick, double discountFactor, AssetType fixed, AssetType floating) {
        super(startTick, endTick, discountFactor);
        this.fixed = fixed;
        this.floating = floating;
    }

    @Override
    double getExpectedExposure(long atTick) {
        return expectedExposure[(int) atTick];
    }
}
