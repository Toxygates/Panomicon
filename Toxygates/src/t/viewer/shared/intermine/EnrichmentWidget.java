/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
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

public enum EnrichmentWidget {
  //TargetMine widgets
  GenePathway("gene_pathway_enrichment", "Pathway",
      new String[] { "All", "KEGG Pathway", "Reactome", "NCI Pathway Interaction Database"}),
  GeneIPC("gene_ipc_enrichment", "Integrated pathway cluster"), 
  GeneGO("gene_go_enrichment", "GO term",
      new String[] {"biological_process", "cellular_component", "molecular_function"}),
  GeneGOAGOS("gene_goagos_enrichment", "GOSlim term",
      new String[] {"biological_process", "cellular_component", "molecular_function"}),
      
  //Information on the following widgets may be looked up from the corresponding intermine
  //APIs. A useful gateway is http://iodocs.apps.intermine.org
  Publication("publication_enrichment", "Publication"),
  ProteinDomain("prot_dom_enrichment_for_gene", "Protein domains"),
  GeneGO2("go_enrichment_for_gene", "GO term",
      new String[] {"biological_process", "cellular_component", "molecular_function"}),
      
  GenePathwayMouse("pathway_enrichment_for_gene", "Pathway (Reactome)"),
  
  //From the flymine beta documentation
  GenePathwayHuman("pathway_enrichment", "Pathway",
      new String[] {"All", "KEGG pathways data set", "Reactome data set"});
  
  
  private String key, label;
  private String[] filterValues;
  
  EnrichmentWidget(String key,
      String label) {
    this(key, label, new String[] { "" });
  }
  
  EnrichmentWidget(String key,
      String label, String[] filterValues) {
    this.key = key;
    this.label = label;
    this.filterValues = filterValues;
  }
 
  @Override
  public String toString() { return label; }
  
  public String getKey() { return key; }
  public String getLabel() { return label; }
 
  public String[] filterValues() { return filterValues; }
  
  /**
   * Get appropriate widgets for the instance.
   * In the future, we may obtain these using API lookups from the instance directly.
   * @param instance
   * @return
   */
  public static EnrichmentWidget[] widgetsFor(IntermineInstance instance) {
    String title = instance.title();
    if (title.equals("TargetMine")) {
      return new EnrichmentWidget[] {
          GenePathway, GeneIPC, GeneGO, GeneGOAGOS
      };
    } else if (title.equals("RatMine")) {
      /**
       * RatMine has no enrichment widgets for genes available at the moment - see 
       * http://iodocs.apps.intermine.org/rgd/docs#/ws-available-widgets/GET/widgets
       */
      return new EnrichmentWidget[] {};
    } else if (title.equals("MouseMine")) {
      return new EnrichmentWidget[] { 
          GenePathwayMouse, GeneGO2, ProteinDomain
      };
    } else if (title.equals("HumanMine")) {
      return new EnrichmentWidget[] {
          Publication, ProteinDomain, GeneGO2, GenePathwayHuman
      };
    } else {
      return new EnrichmentWidget[] {};
    }
  }
}
