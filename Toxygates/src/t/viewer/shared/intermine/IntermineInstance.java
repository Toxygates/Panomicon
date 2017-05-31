package t.viewer.shared.intermine;

import java.io.Serializable;

/**
 * An instance of an Intermine system, such as TargetMine or HumanMine.
 */
@SuppressWarnings("serial")
public class IntermineInstance implements Serializable {

  private String title, appName, userURL;
  
  //GWT constructor
  public IntermineInstance() {}
  
  public IntermineInstance(String title, String appName,
      String userURL) {
    this.title = title;
    this.appName = appName;
    this.userURL = userURL;
  }

  /**
   * For internal API use
   * @return
   */
  public String appName() { 
    return appName;
  }

  /**
   * Application title displayed to the user.
   * @return
   */
  public String title() {
    return title;
  }
 
  /**
   * An URL that the user can visit in a web browser.
   */
  public String webURL() {     
    return userURL;    
  }
  
  public String serviceURL() {
    //TODO is this correct?
    return userURL + "/" + appName + "/service";
  }
  
  @Override
  public boolean equals(Object other) {
    if (other != null && other instanceof IntermineInstance) {
      return userURL.equals(((IntermineInstance) other).webURL());
    }
    return false;
  }
  
  @Override
  public int hashCode() {
    return userURL.hashCode();
  }
 
}
