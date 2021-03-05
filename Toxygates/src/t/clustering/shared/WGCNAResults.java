package t.clustering.shared;

import java.io.Serializable;

public class WGCNAResults implements Serializable {

    //GWT constructor
    public WGCNAResults() {}

    public WGCNAResults(String dendrogramImage,
                        String modulesImage,
                        String[][] clusters) {
        this.dendrogramImage = dendrogramImage;
        this.modulesImage = modulesImage;
        this.clusters = clusters;
    }

    private String dendrogramImage, modulesImage;

    private String[][] clusters;

    public String getDendrogramImage() { return dendrogramImage; }

    public String getModulesImage() { return modulesImage; }

    public String[][] getClusters() { return clusters; }

}
