package t.clustering.shared;

import java.io.Serializable;

public class WGCNAResults implements Serializable {

    //GWT constructor
    public WGCNAResults() {}

    public WGCNAResults(String sampleClusteringImage,
                        String modulesImage,
                        String dendrogramTraitsImage,
                        String softThresholdImage,
                        String[] clusterTitles,
                        String[][] clusters) {
        this.dendrogramTraitsImage = dendrogramTraitsImage;
        this.modulesImage = modulesImage;
        this.softThresholdImage = softThresholdImage;
        this.sampleClusteringImage = sampleClusteringImage;
        this.clusters = clusters;
        this.clusterTitles = clusterTitles;
    }

    private String dendrogramTraitsImage, modulesImage, softThresholdImage, sampleClusteringImage;

    private String[] clusterTitles;

    private String[][] clusters;

    public String getDendrogramTraitsImage() { return dendrogramTraitsImage; }

    public String getModulesImage() { return modulesImage; }

    public String getSoftThresholdImage() { return softThresholdImage; }

    public String getSampleClusteringImage() { return sampleClusteringImage; }

    public String[][] getClusters() { return clusters; }

    public String[] getClusterTitles() { return clusterTitles; }

}
