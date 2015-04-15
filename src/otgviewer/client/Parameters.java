package otgviewer.client;

/**
 * Container for various client side application parameters.
 * @author johan
 */
public class Parameters {

	private final String instanceName, pathologyTermsURL, targetmineURL;
	
	public Parameters(String instanceName_, String pathologyTermsURL_,
			String targetmineURL_) {
		instanceName = instanceName_;
		pathologyTermsURL = pathologyTermsURL_;
		targetmineURL = targetmineURL_;				
	}

	public Parameters(String instanceName_) {
		this(instanceName_, 
				"http://toxico.nibiohn.go.jp/open-tggates/doc/pathology_parameter.pdf", 
				"http://targetmine.nibiohn.go.jp");
	}
	
	
	public String welcomeHtmlURL() {
		return "../shared/" + instanceName() + "/welcome.html";
	}
	
	public String instanceName() { return instanceName; }
	
	public String targetmineURL() { return targetmineURL; }
	
	public String pathologyTermsURL() { return pathologyTermsURL; }

}
