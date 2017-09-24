/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health
 * and Nutrition (NIBIOHN), Japan.
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

package t.viewer.client.table;

import java.util.*;

import otgviewer.client.components.Screen;
import t.common.shared.*;
import t.viewer.client.rpc.SparqlServiceAsync;
import t.viewer.shared.Association;
import t.viewer.shared.table.SortKey;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * A RichTable that can display association columns.
 * @author johan
 *
 */
abstract public class AssociationTable<T> extends RichTable<T> {
	protected final SparqlServiceAsync sparqlService;
	protected Map<AType, Association> associations = new HashMap<AType, Association>();
	private boolean waitingForAssociations = true;
	
	public AssociationTable(Screen screen) {
		super(screen.schema());
		sparqlService = screen.manager().sparqlService();
	}
	
	protected List<HideableColumn<T, ?>> initHideableColumns(DataSchema schema) {
		SafeHtmlCell shc = new SafeHtmlCell();
		List<HideableColumn<T, ?>> r = new ArrayList<HideableColumn<T, ?>>();
		for (AType at: schema.associations()) {
			//TODO fill in matrixColumn for sortable associations
			AssociationColumn ac = new AssociationColumn(shc, at);			
			r.add(ac);
		}
		return r;
	}
	
	private AType[] visibleAssociations() {
		List<AType> r = new ArrayList<AType>();
		for (HideableColumn<T, ?> ac: hideableColumns) {
			if (ac instanceof AssociationTable<?>.AssociationColumn) {				
				if (ac.visible()) {
					r.add(((AssociationTable<?>.AssociationColumn)ac).assoc);
				}
			}
		}
		return r.toArray(new AType[0]);
	}

	protected void getAssociations() {
		waitingForAssociations = true;					
		AType[] vas = visibleAssociations();
		if (vas.length > 0) {
			AsyncCallback<Association[]> assocCallback = new AsyncCallback<Association[]>() {
				public void onFailure(Throwable caught) {
					Window.alert("Unable to get associations: " + caught.getMessage());
				}

				public void onSuccess(Association[] result) {
					associations.clear();
					waitingForAssociations = false;
					for (Association a: result) {
						associations.put(a.type(), a);	
					};				
					grid.redraw();
				}
			};

			logger.info("Get associations for " + chosenSampleClass.toString());
			sparqlService.associations(chosenSampleClass, vas,
					displayedAtomicProbes(), 
					assocCallback);
		}
	}
	
	/**
	 * Get the atomic probes currently being displayed (keys for associations)
	 * @return
	 */
	abstract protected String[] displayedAtomicProbes();
	abstract protected String probeForRow(T row);
	abstract protected String[] atomicProbesForRow(T row);
	abstract protected String[] geneIdsForRow(T row);
	
	public static abstract class LinkingColumn<T> extends HTMLHideableColumn<T> {		
		public LinkingColumn(SafeHtmlCell c, String name, 
				boolean initState, String width) {
			super(c, name, initState, width);			
		}
		
		protected List<String> makeLinks(Collection<Pair<String, String>> values) {
			List<String> r = new ArrayList<String>();
			for (Pair<String, String> v: values) {
				String l = formLink(v.second());
				if (l != null) {
					r.add("<div class=\"associationValue\"><a target=\"_TGassoc\" href=\"" +
							l + "\">" + v.first() + "</a></div>");
				} else {
					r.add("<div class=\"associationValue\">" + v.first() + "</div>"); //no link
				}				
			}
			return r;
		}
		
		@Override
		protected String getHtml(T er) {
			return SharedUtils.mkString(makeLinks(getLinkableValues(er)), "");
		}
				
		protected Collection<Pair<String, String>> getLinkableValues(T er) {
			return new ArrayList<Pair<String, String>>();
		}
		
		protected abstract String formLink(String value);
		
	}
	
	public class AssociationColumn extends LinkingColumn<T> implements MatrixSortable {
		private AType assoc;				
		
		/**
		 * @param tc
		 * @param association
		 * @param matrixIndex Underlying data index for a corresponding
		 * hidden sorting column. Only meaningful if this association is 
		 * sortable.
		 */
		public AssociationColumn(SafeHtmlCell tc, AType association) {
			super(tc, association.title(), false, "15em");
			this.assoc = association;			
			this._columnInfo = new ColumnInfo(_name, _width, association.canSort());
		}
		
		public SortKey sortKey() {
			return new SortKey.Association(assoc);
		}
		
		@Override
		public void setVisibility(boolean v) {
			super.setVisibility(v);
			if (v) {
				getAssociations();
			}
		}
		
		protected String formLink(String value) { return assoc.formLink(value); }
		
		protected Collection<Pair<String, String>> getLinkableValues(T er) {
			Association a = associations.get(assoc);
			Set<Pair<String, String>> all = new HashSet<Pair<String, String>>();
			for (String at : atomicProbesForRow(er)) {
				if (a.data().containsKey(at)) {
					all.addAll(a.data().get(at));					
				} 
			}
			for (String gi : geneIdsForRow(er)) {
				if (a.data().containsKey(gi)) {
					all.addAll(a.data().get(gi));
				}
			}
			return all;
		}
		
		@Override
		protected String getHtml(T er) {					
			if (waitingForAssociations) {
				return("(Waiting for data...)");				
			} else if (associations.containsKey(assoc)) {
					return super.getHtml(er);									
			}		
			return("(Data unavailable)");
		}		
	}
}
