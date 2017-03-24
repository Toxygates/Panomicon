package t.viewer.shared.intermine;

import java.io.Serializable;

/**
 * An instance of an Intermine system, such as TargetMine or HumanMine.
 */
@SuppressWarnings("serial")
public class IntermineInstance implements Serializable {

  private String title, appName, apiKey, userURL;
  
  //GWT constructor
  public IntermineInstance() {}
  
  public IntermineInstance(String title, String appName,
      String apiKey, String userURL) {
    this.title = title;
    this.appName = appName;
    this.apiKey = apiKey;
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
   * API key for login-free shared account
   * @return
   */
  public String apiKey() {
    return apiKey;
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
 
}
