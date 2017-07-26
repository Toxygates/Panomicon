package t.viewer.client.components.search;

import java.io.Serializable;

import t.common.shared.sample.HasSamples;
import t.common.shared.sample.Sample;

@SuppressWarnings("serial")
class SampleContainer implements HasSamples<Sample>, Serializable {
  private Sample[] samples;

  protected SampleContainer() {}

  public SampleContainer(Sample[] s) {
    samples = s;
  }

  @Override
  public Sample[] getSamples() {
    return samples;
  }
}
