/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

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
