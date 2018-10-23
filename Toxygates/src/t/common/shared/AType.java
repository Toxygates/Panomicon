/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package t.common.shared;

import javax.annotation.Nullable;

/**
 * All known association types. In order to add a new type, it is necessary to define it here, and
 * then add the corresponding lookup code in SparqlServiceImpl.
 * 
 * Note: instead of defining everything in a single place, it may eventually
 * be better to have a registry
 * and allow different modules with specialised functionality to register their own association
 * types.
 */
public enum AType {
  KEGG("KEGG pathways") {
    public String formLink(String value) {
      return "http://www.genome.jp/dbget-bin/www_bget?path:" + value;
    }
  },
  Chembl("CHEMBL compounds") {
    public String formLink(String value) {
      return "https://www.ebi.ac.uk/chembldb/compound/inspect/" + value;
    }
  },
  Drugbank("DrugBank compounds") {
    public String formLink(String value) {
      return value;
    }
  },
  Uniprot("UniProt proteins") {
    public String formLink(String value) {
      return formProteinLink(value);
    }
  },
  GO("GO term") {
    public String formLink(String value) {
      return formGOLink(value);
    }
  },
  GOCC("GO Cellular component") {
    public String formLink(String value) {
      return formGOLink(value);
    }
  },
  GOMF("GO Molecular function") {
    public String formLink(String value) {
      return formGOLink(value);
    }
  },
  GOBP("GO Biological process") {
    public String formLink(String value) {
      return formGOLink(value);
    }
  },
  RefseqTrn("RefSeq transcript") {
    public String formLink(String value) {
      return "https://www.ncbi.nlm.nih.gov/nuccore/" + value;
    }
  },
  RefseqProt("Refseq protein") {
    public String formLink(String value) {
      return "https://www.ncbi.nlm.nih.gov/protein/" + value;
    }
  },
  Unigene("UniGene ID") {
    public String formLink(String value) {
      // Example: Rn.162549
      String[] spl = value.split("\\.");
      if (spl.length == 2) {
        return "https://www.ncbi.nlm.nih.gov/UniGene/clust.cgi?ORG=" + spl[0] + "&CID=" + spl[1];
      }
      return null;
    }
  },
  Ensembl("Ensembl gene") {
    public String formLink(String value) {
      return "http://ensembl.org/Gene/Summary?db=core;g=" + value;
    }
  },
  EC("EC enzymes") {
    public String formLink(String value) {
      //example: 5.3.9.4
      return "http://enzyme.expasy.org/EC/" + value;
    }
  },
  Homologene("Homologene entries") {
    public String formLink(String value) {
      return formGeneLink(value);
    }
  },
  OrthProts("eggNOG orthologous proteins") {
    public String formLink(String value) {
      return formProteinLink(value);
    }
  },
  //microRNA-mRNA association
  MiRNA("MicroRNA") {
    public String formLink(String value) {
      //example: MIMAT0000376
//      return "http://www.mirbase.org/cgi-bin/mature.pl?mature_acc=" + value;
      return "http://www.mirbase.org/cgi-bin/query.pl?terms=" + value;
    }
  },
  //mRNA-microRNA association
  MRNA("mRNA"),    
  // Enzymes("Kegg Enzymes") {
  // public String formLink(String value) { return value; }
  // },
  
  //Was used in Tritigate
  EnsemblOSA("O.Sativa orth. genes") {
    public String formLink(String value) {
      return formEnsemblPlantsLink(value);
    }
  },
  
  //Was used in Tritigate
  KEGGOSA("O.Sativa orth. pathways") {
    //Not used currently (Tritigate legacy)
    public String formLink(String value) {
      return value;
    }
  },
  
  //Was used in Tritigate
  Contigs("Contigs"), SNPs("SNPs"), POPSEQ("POPSEQ distances") {
    public boolean canSort() {
      return true;
    }

    public String auxSortTableKey() {
      return "popseq";
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

  public boolean canSort() {
    return false;
  }

  public boolean canFilter() {
    return false;
  }

  public @Nullable String auxSortTableKey() {
    return null;
  }

  public static String formGeneLink(String value) {
    if (value != null) {
      return "http://www.ncbi.nlm.nih.gov/gene/" + value;
    } else {
      return null;
    }
  }

  public static String formProteinLink(String value) {
    if (value != null) {
      return "http://www.uniprot.org/uniprot/" + value;
    } else {
      return null;
    }
  }

  public static String formGOLink(String value) {
    if (value != null) {
      return "http://amigo.geneontology.org/amigo/term/" + value.toUpperCase();
    } else {
      return null;
    }
  }

  public static String formEnsemblPlantsLink(String value) {
    if (value != null) {
      return "http://plants.ensembl.org/Oryza_sativa/Gene/Summary?g=" + value.toUpperCase();
    } else {
      return null;
    }
  }
}
