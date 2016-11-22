package otgviewer.shared;

@SuppressWarnings("serial")
public class StringBioParamValue extends BioParamValue {

  protected String value;
  
  public StringBioParamValue() { }

  public StringBioParamValue(String id, String label, String value) {
    super(id, label);
    this.value = value;
  }

  @Override
  public String displayValue() {
    return value;
  }

}
