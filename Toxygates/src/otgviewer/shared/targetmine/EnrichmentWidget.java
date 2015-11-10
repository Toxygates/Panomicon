package otgviewer.shared.targetmine;

public enum EnrichmentWidget {
  GenePathway("gene_pathway_enrichment", "Pathway"), 
  GeneIPC("gene_ipc_enrichment", "Integrated pathway cluster"), 
  GeneGO("gene_go_enrichment", "GO term"),
  GeneGOAGOS("gene_goagos_enrichment", "GOAGOS");
  
  private String key, label;
  EnrichmentWidget(String key, String label) {
    this.key = key;
    this.label = label;
  }
  
  public String getKey() { return key; }
  public String getLabel() { return label; }
  
}
