package t.model.sample;

import t.model.sample.Attribute;

/**
 * Provides distributional information for attributes, typically from a
 * control group or other set of samples.
 */
public abstract class VarianceSet {

    public abstract Double standardDeviation(Attribute attribute);
    public abstract Double mean(Attribute attribute);

    public Double lowerBound(Attribute attribute, int zTestSampleSize) {
        Double standardDeviation = standardDeviation(attribute);
        if (standardDeviation == null) {
            return null;
        }
        Double mean = mean(attribute);
        if (mean == null) {
            return null;
        }
        return mean - 2 / Math.sqrt(zTestSampleSize) * standardDeviation;
    }

    public Double upperBound(Attribute attribute, int zTestSampleSize) {
        Double standardDeviation = standardDeviation(attribute);
        if (standardDeviation == null) {
            return null;
        }
        Double mean = mean(attribute);
        if (mean == null) {
            return null;
        }
        return mean + 2 / Math.sqrt(zTestSampleSize) * standardDeviation;
    }
}
