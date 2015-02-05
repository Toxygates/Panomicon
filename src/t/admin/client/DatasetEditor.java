package t.admin.client;

import java.util.Date;

import t.admin.shared.Dataset;

import com.google.gwt.user.client.ui.TextBox;

public class DatasetEditor extends ManagedItemEditor {

	private final TextBox idText, descText, commentText;
	
	public DatasetEditor() {
		super();
		idText = addLabelledTextBox("ID");
		descText = addLabelledTextBox("Description");
		commentText = addLabelledTextBox("Comment");
		
		addCommands();		
	}	

	@Override
	protected void triggerEdit() {
		Dataset d = new Dataset(idText.getValue(), 
				descText.getValue(), commentText.getValue(), new Date());
		maintenanceService.addDataset(d, editCallback());
	}	
}
