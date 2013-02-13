package otgviewer.shared;

/**
 * Types for associations.
 * @author johan
 *
 */
public enum AType {
	KEGG("KEGG pathways"),
	Chembl("CHEMBL compounds"), Drugbank("DrugBank compounds"), 
	Uniprot("UniProt proteins") {
		 public String formLink(String value) {
			 return formProteinLink(value);
		 }
	}, GOCC("GO Cellular component"),
	GOMF("GO Molecular function"), GOBP("GO Biological process"), 
	Homologene("Homologene entries") {
		public String formLink(String value) {
			return formGeneLink(value);
		}
	},
	KOProts("KO orthologous proteins") {
		 public String formLink(String value) {
			 return formProteinLink(value);
		 }
	};

	private String _title;

	private AType(String name) {
		this._title = name;
	}	
	
	public String title() { 
		return _title;
	}
	
	public String formLink(String value) {
		return null;
	}
	
	public static String formGeneLink(String value) {
		return "http://www.ncbi.nlm.nih.gov/gene/" + value;
	}
	
	public static String formProteinLink(String value) {
		return "http://www.uniprot.org/uniprot/" + value;
	}
}
