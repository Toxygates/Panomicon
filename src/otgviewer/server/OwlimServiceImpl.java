package otgviewer.server;

import java.util.Collection;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import otg.B2RAffy;
import otg.B2RKegg;
import otg.BCode;
import otg.CHEMBL;
import otg.DrugBank;
import otg.OTGOwlim;
import otg.OTGQueries;
import otg.Species;
import otgviewer.client.OwlimService;
import otgviewer.shared.Barcode;
import otgviewer.shared.DataFilter;
import otgviewer.shared.SampleTimes;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class OwlimServiceImpl extends RemoteServiceServlet implements
		OwlimService {

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		OTGOwlim.connect();
		B2RAffy.connect();
	}
	
	public void destroy() {
		B2RAffy.close();
		OTGOwlim.close();
		super.destroy();		
	}
	
	private otg.Filter toScala(DataFilter filter) {
		return new otg.Filter(filter.cellType.toString(), filter.organ.toString(),
				filter.repeatType.toString(), filter.organism.toString());
	}
	
	public String[] compounds(DataFilter filter) {		
		return OTGOwlim.compounds(toScala(filter));		
	}

	public String[] organs(DataFilter filter, String compound) {		
		return OTGOwlim.organs(toScala(filter), compound);		
	}

	public String[] doseLevels(DataFilter filter, String compound) {		
		return OTGOwlim.doseLevels(toScala(filter), compound);		
	}

	public Barcode[] barcodes(DataFilter filter, String compound,
			String doseLevel, String time) {

		BCode[] codes = OTGOwlim.barcodes4J(toScala(filter), compound, doseLevel, time);
		Barcode[] r = new Barcode[codes.length];
		int i = 0;
		for (BCode code : codes) {
			r[i] = new Barcode(code.code(), code.individual(), code.dose(),
					code.time(), compound);
			i += 1;
		}
		return r;
	}
	
	public String[] times(DataFilter filter, String compound) {
		String[] r =  OTGOwlim.times(toScala(filter), compound);
		SampleTimes.sortTimes(r);
		return r;
	}
	
	public String probeTitle(String probe) {		
		return B2RAffy.title(probe);					
	}
	
	public String[] probes(DataFilter filter) {		
		return OTGQueries.probeIds(Utils.speciesFromFilter(filter));
	}
	
	public String[] pathways(DataFilter filter, String pattern) {
		try {
			B2RKegg.connect();
			Species s = Utils.speciesFromFilter(filter);
			return B2RKegg.pathways(pattern, s);
		} finally {
			B2RKegg.close();
		}		
	}
	
	public String[][] geneSymsForProbes(String[] probes) {
		return B2RAffy.geneSyms4J(probes).toArray(new String[0][0]);
	}
	
	public String[] probesForPathway(DataFilter filter, String pathway) {
		try {
			B2RKegg.connect();						
			Species s = Utils.speciesFromFilter(filter);
			String[] geneIds = B2RKegg.geneIds(pathway, s);
			System.out.println("Probes for " + geneIds.length + " genes");
			String [] probes = OTGOwlim.probesForEntrezGenes4J(geneIds);
			return OTGQueries.filterProbes(probes, s);			
		} finally {
			B2RKegg.close();			
		}		
	}
	
	public String[] probesTargetedByCompound(DataFilter filter, String compound, String service) {
			
			Species s = Utils.speciesFromFilter(filter);
			String[] prots;
			if (service.equals("CHEMBL")) {
				try {
					CHEMBL.connect();			
					prots = CHEMBL.targetProtsForCompound(compound, s);
				} finally {					
					CHEMBL.close();			
				}
			} else if (service.equals("DrugBank")) {
				try {
					DrugBank.connect();
					prots = DrugBank.targetProtsForDrug(compound);
				} finally {
					DrugBank.close();
				}
				
			} else {
				//TODO
				throw new RuntimeException("Unexpected service request: " + service);
			}
			
			System.out.println("Probes for " + prots.length + " genes");
			String [] probes = OTGOwlim.probesForUniprot4J(prots);
			return OTGQueries.filterProbes(probes, s);					
	}
	
	public String[] goTerms(String pattern) {	
		return OTGOwlim.goTerms(pattern);		
	}
	
	public String[] probesForGoTerm(DataFilter filter, String goTerm) {
		Species s = Utils.speciesFromFilter(filter);
		return OTGQueries.filterProbes(OTGOwlim.probesForGoTerm(goTerm), s);
	}

}
