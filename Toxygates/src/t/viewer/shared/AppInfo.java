package t.viewer.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import otgviewer.shared.Group;
import t.common.shared.Dataset;
import t.common.shared.clustering.Algorithm;
import t.common.shared.clustering.ProbeClustering;

/**
 * Container for various client side application parameters.
 * 
 * TODO some of this is OTG-specific
 * @author johan
 */
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
				"http://targetmine.nibiohn.go.jp");
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
