package otgviewer.shared.targetmine;

public enum EnrichmentWidget {
  GenePathway("gene_pathway_enrichment", "Pathway",
      new String[] { "All", "KEGG Pathway", "Reactome", "NCI Pathway Interaction Database"}),
  GeneIPC("gene_ipc_enrichment", "Integrated pathway cluster"), 
  GeneGO("gene_go_enrichment", "GO term",
      new String[] {"biological_process", "cellular_component", "molecular_function"}),
  GeneGOAGOS("gene_goagos_enrichment", "GOSlim term",
      new String[] {"biological_process", "cellular_component", "molecular_function"});
  
  private String key, label;
  private String[] filterValues;
  
  EnrichmentWidget(String key, String label) {
    this(key, label, new String[] { "" });
  }
  
  EnrichmentWidget(String key, String label, String[] filterValues) {
    this.key = key;
    this.label = label;
    this.filterValues = filterValues;
  }
 
  @Override
  public String toString() { return label; }
  
  public String getKey() { return key; }
  public String getLabel() { return label; }
 
  public String[] filterValues() { return filterValues; }
}
