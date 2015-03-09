package otgviewer.client.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import t.common.client.rpc.SparqlService;
import t.common.client.rpc.SparqlServiceAsync;
import t.common.shared.DataSchema;
import t.common.shared.Pair;
import t.common.shared.SharedUtils;
import t.viewer.shared.AType;
import t.viewer.shared.Association;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * A RichTable that can display association columns.
 * @author johan
 *
 */
abstract public class AssociationTable<T> extends RichTable<T> {
	protected final SparqlServiceAsync sparqlService = (SparqlServiceAsync) GWT.create(SparqlService.class);
	protected Map<AType, Association> associations = new HashMap<AType, Association>();
	private boolean waitingForAssociations = true;
	
	public AssociationTable(Screen screen) {
		super(screen.schema());
	}
	
	protected List<HideableColumn> initHideableColumns(DataSchema schema) {
		SafeHtmlCell shc = new SafeHtmlCell();
		List<HideableColumn> r = new ArrayList<HideableColumn>();
		for (AType at: schema.associations()) {
			AssociationColumn ac = new AssociationColumn(shc, at);			
			r.add(ac);
		}
		return r;
	}
	
	private AType[] visibleAssociations() {
		List<AType> r = new ArrayList<AType>();
		for (HideableColumn ac: hideableColumns) {
			if (ac instanceof AssociationTable.AssociationColumn) {
				AssociationColumn aac = (AssociationColumn) ac;
				if (aac.visible()) {
					r.add(aac.assoc);
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
	
	public static abstract class LinkingColumn<T> extends Column<T, SafeHtml> implements HideableColumn {
		private boolean visible;
		SafeHtmlCell c;
		String name;
		String width;
		public LinkingColumn(SafeHtmlCell c, String name, 
				boolean initState, String width) {
			super(c);
			visible = initState;
			this.name = name;
			this.width = width;
			this.c = c;
		}
				
		public String name() { return name; }
		public boolean visible() { return this.visible; }
		public void setVisibility(boolean v) { visible = v; }
		public String width() { return width; }
		
		protected List<String> makeLinks(Collection<Pair<String, String>> values) {
			List<String> r = new ArrayList<String>();
			for (Pair<String, String> v: values) {
				String l = formLink(v.second());
				if (l != null) {
					r.add("<a target=\"_TGassoc\" href=\"" + l + "\">" + v.first() + "</a>");
				} else {
					r.add(v.first()); //no link
				}				
			}
			return r;
		}
		
		public SafeHtml getValue(T er) {
			SafeHtmlBuilder build = new SafeHtmlBuilder();
			String c = SharedUtils.mkString(makeLinks(getLinkableValues(er))
							, ", ");
			build.appendHtmlConstant(c);
			return build.toSafeHtml();
		}
		
		protected Collection<Pair<String, String>> getLinkableValues(T er) {
			return new ArrayList<Pair<String, String>>();
		}
		
		protected abstract String formLink(String value);
		
	}
	
	public class AssociationColumn extends LinkingColumn<T> implements HideableColumn {
		AType assoc;		
		
		public AssociationColumn(SafeHtmlCell tc, AType association) {
			super(tc, association.title(), false, "15em");
			this.assoc = association;			
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
		
		public SafeHtml getValue(T er) {		
			SafeHtmlBuilder build = new SafeHtmlBuilder();
			if (waitingForAssociations) {
				build.appendEscaped("(Waiting for data...)");
				return build.toSafeHtml();
			} else {
				if (associations.containsKey(assoc)) {
					return super.getValue(er);					
				} else {
					build.appendEscaped("(Data unavailable)");
				}
			}
			return build.toSafeHtml();
		}		
	}
}
