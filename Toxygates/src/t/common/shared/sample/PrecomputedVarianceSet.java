package t.common.shared.sample;

import t.model.sample.VarianceSet;
import t.model.sample.Attribute;

import java.io.Serializable;
import java.util.HashMap;

/**
 * VarianceSet with precomputed standard deviations and means. Used for sending
 * variance information to the client.
 */
@SuppressWarnings("serial")
public class PrecomputedVarianceSet extends VarianceSet implements Serializable {
  public HashMap<Attribute, Double> standardDeviations;
  public HashMap<Attribute, Double> means;

  //GWT constructor
  public PrecomputedVarianceSet() {
  }

  public PrecomputedVarianceSet(VarianceSet source, Attribute[] attributes) {
    standardDeviations = new HashMap<Attribute, Double>();
    means = new HashMap<Attribute, Double>();
    for (Attribute attribute : attributes) {
      standardDeviations.put(attribute, source.standardDeviation(attribute));
      means.put(attribute, source.mean(attribute));
    }
  }

  public Double standardDeviation(Attribute attribute) {
    return standardDeviations.get(attribute);
  }

  public Double mean(Attribute attribute) {
    return means.get(attribute);
  }
}
