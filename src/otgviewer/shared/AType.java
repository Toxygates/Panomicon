package otgviewer.shared;

/**
 * Types for associations.
 * @author johan
 *
 */
public enum AType {
	KEGG("KEGG pathways"),
	Chembl("CHEMBL compounds"), Drugbank("DrugBank compounds"), 
	Uniprot("UniProt proteins"), GOCC("GO Cellular component"), 
			GOMF("GO Molecular function"), GOBP("GO Biological process"), 
			Homologene("Homologene entries");

	private String _title;

	private AType(String name) {
		this._title = name;
	}	
	
	public String title() { 
		return _title;
	}

}
