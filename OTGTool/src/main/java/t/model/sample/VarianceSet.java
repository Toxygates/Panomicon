package t.model.sample;

import t.model.sample.Attribute;

import javax.annotation.Nullable;

/**
 * Provides distributional information for attributes, typically from a
 * control group or other set of samples.
 */
public abstract class VarianceSet {

    public abstract Double standardDeviation(Attribute attribute);
    public abstract Double mean(Attribute attribute);

    @Nullable
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

    @Nullable
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
