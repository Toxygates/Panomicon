/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
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

package t.viewer.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import otgviewer.shared.Group;
import t.common.shared.Dataset;
import t.common.shared.StringList;
import t.common.shared.clustering.ProbeClustering;

/**
 * Container for various client side application parameters.
 * 
 * TODO some of this is OTG-specific
 * @author johan
 */
@SuppressWarnings("serial")
public class AppInfo implements Serializable {

	private String instanceName, pathologyTermsURL, targetmineURL;
	
	private Group[] predefGroups = new Group[0];
	private Dataset[] datasets = new Dataset[0];
	private List<StringList> predefProbeLists = new ArrayList<StringList>();
	private List<ProbeClustering> probeClusterings = new ArrayList<ProbeClustering>();
	
	public AppInfo() {}
	
	public AppInfo(String instanceName_, String pathologyTermsURL_,
			String targetmineURL_) {
		instanceName = instanceName_;
		pathologyTermsURL = pathologyTermsURL_;
		targetmineURL = targetmineURL_;			
		//NB does not set datasets
	}

	public AppInfo(String instanceName_, Dataset[] datasets,
			List<StringList> probeLists) {
		this(instanceName_, 
				"http://toxico.nibiohn.go.jp/open-tggates/doc/pathology_parameter.pdf", 
				"http://targetmine.mizuguchilab.org");
		this.datasets = datasets;
		predefProbeLists = probeLists;
	}
	
	public AppInfo(String instanceName_, Dataset[] datasets,
			List<StringList> probeLists, List<ProbeClustering> probeClusterings) {
		this(instanceName_, datasets, probeLists);
		this.probeClusterings = probeClusterings;
//		probeClusterings.add(new ProbeClustering("LV_K120", new Algorithm("Hierarchical"), "K"));
	}
	
	public String welcomeHtmlURL() {
		return "../shared/" + instanceName() + "/welcome.html";
	}
	
	public String instanceName() { return instanceName; }
	
	public String targetmineURL() { return targetmineURL; }
	
	public String pathologyTermsURL() { return pathologyTermsURL; }
	
	public String userGuideURL() { return  "toxygatesManual.pdf"; }
	
	public Group[] predefinedSampleGroups() { return predefGroups; }
	
	public void setPredefinedGroups(Group[] gs) { predefGroups = gs; }
	
	public Dataset[] datasets() { return datasets; }
	
	public Collection<StringList> predefinedProbeLists() { return predefProbeLists; }
	
	//Suggested API
	public Collection<ProbeClustering> probeClusterings() { return probeClusterings; }
}
