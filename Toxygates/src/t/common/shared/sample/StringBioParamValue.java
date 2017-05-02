package t.common.shared.sample;

@SuppressWarnings("serial")
public class StringBioParamValue extends BioParamValue {

  protected String value;
  
  public StringBioParamValue() { }

  public StringBioParamValue(String id, String label, String section, String value) {
    super(id, label, section);
    this.value = value;
  }

  @Override
  public String displayValue() {
    return value;
  }

}
