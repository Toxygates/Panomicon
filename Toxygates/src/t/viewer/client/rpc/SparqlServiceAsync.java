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

package t.viewer.client.rpc;

import java.util.List;

import otgviewer.shared.OTGColumn;
import otgviewer.shared.OTGSample;
import otgviewer.shared.Pathology;
import t.common.shared.AType;
import t.common.shared.Dataset;
import t.common.shared.Pair;
import t.common.shared.SampleClass;
import t.common.shared.sample.Annotation;
import t.common.shared.sample.HasSamples;
import t.viewer.shared.AppInfo;
import t.viewer.shared.Association;
import t.viewer.shared.Unit;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface SparqlServiceAsync {
	
	public void appInfo(AsyncCallback<AppInfo> callback);
	
	public void chooseDatasets(Dataset[] enabled, AsyncCallback<Void> callback);
	
	@Deprecated
	public void sampleClasses(AsyncCallback<SampleClass[]> callback);
	
	public void parameterValues(SampleClass sc, String parameter, 
			AsyncCallback<String[]> callback);
	public void parameterValues(SampleClass[] scs, String parameter, 
			AsyncCallback<String[]> callback);
		
	public void samplesById(String[] ids, AsyncCallback<OTGSample[]> callback);
	public void samples(SampleClass sc, AsyncCallback<OTGSample[]> callback);	
	public void samples(SampleClass sc, String param, String[] paramValues, 
			AsyncCallback<OTGSample[]> callback);
	public void samples(SampleClass[] scs, String param, String[] paramValues, 
			AsyncCallback<OTGSample[]> callback);
	
	public void units(SampleClass sc,
			String param, String[] paramValues, 
			AsyncCallback<Pair<Unit, Unit>[]> callback);
	
	public void units(SampleClass[] sc,
			String param, String[] paramValues, 
			AsyncCallback<Pair<Unit, Unit>[]> callback);

	//TODO this is OTG-specific
	public void pathologies(OTGColumn column, AsyncCallback<Pathology[]> callback);
	
	public void annotations(HasSamples<OTGSample> column, boolean importantOnly,
			AsyncCallback<Annotation[]> callback);
	public void annotations(OTGSample barcode, AsyncCallback<Annotation> callback);
	
	public void pathways(SampleClass sc, String pattern, AsyncCallback<String[]> callback);
	@Deprecated
	public void probesForPathway(SampleClass sc, String pathway, AsyncCallback<String[]> callback);
	public void probesForPathway(SampleClass sc, String pathway, List<OTGSample> samples,
		AsyncCallback<String[]> callback);
	public void probesTargetedByCompound(SampleClass sc, String compound, String service, 
			boolean homologous, AsyncCallback<String[]> callback);
	
	public void geneSyms(String[] probes, AsyncCallback<String[][]> callback);
	public void geneSuggestions(SampleClass sc, String partialName, AsyncCallback<String[]> callback);
	
	public void goTerms(String pattern, AsyncCallback<String[]> callback);
	@Deprecated
	public void probesForGoTerm(String term, AsyncCallback<String[]> callback);
	public void probesForGoTerm(String pattern, List<OTGSample> samples, AsyncCallback<String[]> callback);
	
	public void filterProbesByGroup(String[] probes, List<OTGSample> samples, AsyncCallback<String[]> callback);
	
	public void associations(SampleClass sc, AType[] types, String[] probes, 
			AsyncCallback<Association[]> callback);

	public void keywordSuggestions(String partialName, int maxSize, AsyncCallback<Pair<String, String>[]> asyncCallback);
}
