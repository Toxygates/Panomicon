package t.clustering.shared;

import java.io.Serializable;

public class WGCNAParams implements Serializable {

    //GWT constructor
    public WGCNAParams() {}

    public WGCNAParams(int cutheight, int softPower) {
        this.cutheight = cutheight;
        this.softPower = softPower;
    }

    private int cutheight;
    public int getCutHeight() { return cutheight; }

    private int softPower;
    public int getSoftPower() { return softPower; }
}
