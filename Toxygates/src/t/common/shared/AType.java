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

package t.common.shared;

/**
 * All known association types. In order to add a new type, it is necessary to define it here,
 * and then add the corresponding lookup code in SparqlServiceImpl.
 * 
 * TODO split into common, otg and triti annotations
 */
public enum AType {
	KEGG("KEGG pathways") {
		//TODO construct proper link
		public String formLink(String value) { return value; }		
	},
	Chembl("CHEMBL compounds") {
		public String formLink(String value) { return "https://www.ebi.ac.uk/chembldb/compound/inspect/" + value; }		
	},
	Drugbank("DrugBank compounds") {
		public String formLink(String value) { return value; }		
	},
	Uniprot("UniProt proteins") {
		 public String formLink(String value) { return formProteinLink(value); }		 
	},
	GO("GO term") {
		public String formLink(String value) { return formGOLink(value); }
	},
	GOCC("GO Cellular component") {
		public String formLink(String value) { return formGOLink(value); }		
	},
	GOMF("GO Molecular function") {
		public String formLink(String value) { return formGOLink(value); }		
	},	
	GOBP("GO Biological process") {
		public String formLink(String value) { return formGOLink(value); }		
	},	
	Homologene("Homologene entries") {
		public String formLink(String value) { return formGeneLink(value); }		
	},
	OrthProts("eggNOG orthologous proteins") {
		 public String formLink(String value) { return formProteinLink(value); }		 
	},
	Enzymes("Kegg Enzymes") {
		public String formLink(String value) { return value; }
	},
	EnsemblOSA("O.Sativa orth. genes") {
		public String formLink(String value) { return formEnsemblPlantsLink(value); }
	},
	KEGGOSA("O.Sativa orth. pathways") {
		//TODO construct proper link
		public String formLink(String value) { return value; }
	},
	Contigs("Contigs"),
	SNPs("SNPs"),
	POPSEQ("POPSEQ distances") {
		public boolean canSort() { return true; }
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
			return "http://amigo.geneontology.org/cgi-bin/amigo/term_details?term=" + value.toUpperCase();
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
