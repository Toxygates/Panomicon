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
