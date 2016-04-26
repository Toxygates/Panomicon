/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health
 * and Nutrition (NIBIOHN), Japan.
 * 
 * This file is part of Toxygates.
 * 
 * Toxygates is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Toxygates is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Toxygates. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package t.viewer.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import t.common.shared.Dataset;
import t.common.shared.Platform;
import t.common.shared.StringList;
import t.common.shared.clustering.ProbeClustering;
import t.common.shared.sample.Group;

/**
 * Container for various client side application parameters.
 * 
 * TODO some of this is OTG-specific
 */
@SuppressWarnings("serial")
public class AppInfo implements Serializable {

  private String instanceName, pathologyTermsURL, targetmineURL, applicationName;

  private Platform[] platforms = new Platform[0];
  private Group[] predefGroups = new Group[0];
  private Dataset[] datasets = new Dataset[0];
  private List<StringList> predefProbeLists = new ArrayList<StringList>();
  private List<ProbeClustering> probeClusterings = new ArrayList<ProbeClustering>();

  private String[] annotationTitles;
  private String[] annotationComments;
  
  private String userKey;
  
  public AppInfo() {}

  public AppInfo(String instanceName_, String pathologyTermsURL_, String targetmineURL_,
      String applicationName_, String userKey_) {
    instanceName = instanceName_;
    pathologyTermsURL = pathologyTermsURL_;
    targetmineURL = targetmineURL_;
    applicationName = applicationName_;
    userKey = userKey_;
    // NB does not set datasets or platforms
  }

  public AppInfo(String instanceName_, Dataset[] datasets, Platform[] platforms,
      List<StringList> probeLists, String appName, String userKey) {
    this(instanceName_, "http://toxico.nibiohn.go.jp/open-tggates/doc/pathology_parameter.pdf",
        "http://targetmine.mizuguchilab.org", appName, userKey);
    this.datasets = datasets;
    this.platforms = platforms;
    this.predefProbeLists = probeLists;
  }

  public AppInfo(String instanceName_, Dataset[] datasets, Platform[] platforms,
      List<StringList> probeLists, List<ProbeClustering> probeClusterings, String appName,
      String userKey,
      String[][] annotationInfo) {
    this(instanceName_, datasets, platforms, probeLists, appName, userKey);
    this.probeClusterings = probeClusterings;
    this.annotationTitles = annotationInfo[0];
    this.annotationComments = annotationInfo[1];
  }

  public String welcomeHtmlURL() {
    return "../shared/" + instanceName() + "/welcome.html";
  }

  public String applicationName() { return applicationName; }

  public String instanceName() {
    return instanceName;
  }

  public String targetmineURL() {
    return targetmineURL;
  }

  public String pathologyTermsURL() {
    return pathologyTermsURL;
  }

  public String userGuideURL() {
    return "toxygatesManual.pdf";
  }

  public Group[] predefinedSampleGroups() {
    return predefGroups;
  }

  public void setPredefinedGroups(Group[] gs) {
    predefGroups = gs;
  }

  public Dataset[] datasets() {
    return datasets;
  }
  
  public void setDatasets(Dataset[] ds) {
    this.datasets = ds;
  }
  
  public Platform[] platforms() {
    return platforms;
  }

  public Collection<StringList> predefinedProbeLists() {
    return predefProbeLists;
  }

  public Collection<ProbeClustering> probeClusterings() {
    return probeClusterings;
  }
  
  public String[] getAnnotationTitles() {
    return annotationTitles;
  }
  
  public String[] getAnnotationComments() {
    return annotationComments;
  }
  
  public String getUserKey() {
    return userKey;
  }
}
