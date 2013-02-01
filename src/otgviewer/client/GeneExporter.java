package otgviewer.client;

import java.util.Arrays;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.EnumSelector;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.shared.SharedUtils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class GeneExporter extends Composite {
	private final KCServiceAsync kcService = (KCServiceAsync) GWT
			.create(KCService.class);
	
	private enum ExportType {
		All("All"), Limit("Limited number:");
		
		private String name;
		private ExportType(String name) { this.name = name; }		
		public String toString() { return name; }
	}
	
	private enum Template {
		Upstream("Upstream transcription factors",
				"http://targetmine.nibio.go.jp/targetmine/loadTemplate.do?name=Gene_TFSource&constraint1=Gene&op1=LOOKUP&method=results&value1="),
		Downstream("Downstream transcription target genes",
				"http://targetmine.nibio.go.jp/targetmine/loadTemplate.do?name=Gene_TFTarget&constraint1=Gene&op1=LOOKUP&method=results&value1="),
		Pathways("Pathways",
				"http://targetmine.nibio.go.jp/targetmine/loadTemplate.do?name=Gene_Pathway&constraint1=Gene&op1=LOOKUP&method=results&value1="),		
		Biogrid("Interacting genes (BioGRID)",
				"http://targetmine.nibio.go.jp/targetmine/loadTemplate.do?name=Gene_BioGrid_All&constraint1=Gene&op1=LOOKUP&method=results&value1="),
		Interactions("All interactions",
				"http://targetmine.nibio.go.jp/targetmine/loadTemplate.do?name=Gene_All_Interactions&constraint1=Gene&op1=LOOKUP&method=results&value1="),
		Disease("Diseases (GWAS)",
				"http://targetmine.nibio.go.jp/targetmine/loadTemplate.do?name=Gene_GWAS&constraint1=Gene&op1=LOOKUP&method=results&value1="),
		Enzyme("Enzymes",
				"http://targetmine.nibio.go.jp/targetmine/loadTemplate.do?name=Gene_Enzyme&constraint1=Gene&op1=LOOKUP&method=results&value1="),
		DOAnnotation("DO annotations",
				"http://targetmine.nibio.go.jp/targetmine/loadTemplate.do?name=Gene_DOAnnotation&constraint1=Gene&op1=LOOKUP&method=results&value1="),
		Homologues("Homologue genes", 
			"http://targetmine.nibio.go.jp/targetmine/loadTemplate.do?name=Homologue_Query&constraint1=Gene&op1=LOOKUP&method=results&value1="),
		GOTermsParents("GO terms with parents",
				"http://targetmine.nibio.go.jp/targetmine/loadTemplate.do?name=Gene_allGOTerms&constraint1=Gene&op1=LOOKUP&method=results&value1=");
			
		String url;		
		String name;
		private Template(String name, String url) { this.name = name; this.url = url; }
		public String toString() { return name; }
	}
	
	private EnumSelector<ExportType> etes = new EnumSelector<ExportType>() {
		protected ExportType[] values() { return ExportType.values(); }
	};
	private EnumSelector<Template> tes = new EnumSelector<Template>() {
		protected Template[] values() { return Template.values(); }
	};
	
//	private TextArea geneListText = new TextArea();
	private TextBox geneNumberText = new TextBox();
	
	public GeneExporter(final DataListenerWidget w) {	
		VerticalPanel vp = Utils.mkVerticalPanel(true);
		initWidget(vp);
		vp.setWidth("300px");
		vp.add(Utils.mkEmphLabel("TargetMine export"));
		Label l = new Label("This will export the genes you are currently viewing to the TargetMine system for further analysis.");
		l.setWordWrap(true);
		vp.add(l);
		
		geneNumberText.setEnabled(false);
	
		geneNumberText.setWidth("3em");
		vp.add(Utils.mkHorizontalPanel(true, Utils.mkEmphLabel("Probe selection"), etes, geneNumberText));		
		vp.add(Utils.mkHorizontalPanel(true, Utils.mkEmphLabel("Template"), tes));
		
		etes.listBox().addChangeHandler(new ChangeHandler() {
			
			@Override
			public void onChange(ChangeEvent event) {
				switch(etes.value()) {
				case All:
					geneNumberText.setEnabled(false);
					break;
				case Limit:
					geneNumberText.setEnabled(true);
					if (geneNumberText.getValue().equals("")) {
						geneNumberText.setValue("100");
					}
					break;
				}
				
			}
		});
		
//		vp.add(Utils.mkEmphLabel("Gene IDs for copy and paste"));
//		vp.add(geneListText);
		
//		Button getGenes = new Button("Show gene IDs");
		Button export = new Button("Export");
		export.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				int limit = -1;
				switch (etes.value()) {
				case Limit:
					limit = Integer.valueOf(geneNumberText.getValue());
					break;
				default:
					break;
				}
				
				kcService.getGenes(limit, new PendingAsyncCallback<String[]>(w) {
					public void handleSuccess(String[] genes) {
						String[] useGenes = genes;
						if (genes.length > 500) {
							Window.alert(genes.length + " genes were retrieved. Only the first 500 will be exported.");
							useGenes = new String[500];
							for (int i = 0; i < 500; ++i) {
								useGenes[i] = genes[i];
							}
						} 
						String url = tes.value().url;
						String geneString = SharedUtils.mkString(Arrays.asList(useGenes), ",");
						Window.open(url + geneString, "_blank", "");						
					}
				});

			}
		});
		
//		vp.add(Utils.mkHorizontalPanel(true, getGenes, export);
		vp.add(export);
		
	}
}
