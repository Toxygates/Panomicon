package gwttest.client;

import gwttest.shared.FieldVerifier;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Gwttest implements EntryPoint {
	/**
	 * The message displayed to the user when the server cannot be reached or
	 * returns an error.
	 */
	private static final String SERVER_ERROR = "An error occurred while "
			+ "attempting to contact the server. Please check your network "
			+ "connection and try again.";

	/**
	 * Create a remote service proxy to talk to the server-side Greeting service.
	 */
	private final GreetingServiceAsync greetingService = GWT
			.create(GreetingService.class);
	
	private OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT.create(OwlimService.class);

	private ListBox compoundList, organList, doseLevelList, barcodeList;
	
	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {


		// Add the nameField and sendButton to the RootPanel
		// Use RootPanel.get() to get the entire body element
		RootPanel rootPanel = RootPanel.get("rootPanelContainer");
		rootPanel.getElement().getStyle().setPosition(Position.RELATIVE);
		
		DockPanel dockPanel = new DockPanel();
		rootPanel.add(dockPanel, 10, 10);
		dockPanel.setSize("872px", "283px");
		
		FlowPanel flowPanel = new FlowPanel();
		dockPanel.add(flowPanel, DockPanel.CENTER);
		flowPanel.setSize("858px", "209px");
		
		compoundList = new ListBox();
		flowPanel.add(compoundList);			
		compoundList.setSize("204px", "202px");
		compoundList.setVisibleItemCount(10);
		compoundList.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent event) {
				String compound = compounds[compoundList.getSelectedIndex()];
				getOrgans(compound);
				getDoseLevels(compound, null);
				getBarcodes(compound, null, null);
			}
		});
		
		
		organList = new ListBox();
		flowPanel.add(organList);
		organList.setSize("210px", "202px");
		organList.setVisibleItemCount(10);
		organList.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent event) {
				String compound = compounds[compoundList.getSelectedIndex()];
				String organ = organs[organList.getSelectedIndex()];
				getDoseLevels(compound, organ);
				getBarcodes(compound, organ, null);
			}
		});
		
		doseLevelList = new ListBox();		
		flowPanel.add(doseLevelList);
		doseLevelList.setSize("210px", "202px");
		doseLevelList.setVisibleItemCount(10);
		doseLevelList.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent event) {
				String compound = compounds[compoundList.getSelectedIndex()];
				String organ = organs[organList.getSelectedIndex()];
				String doseLevel= doseLevels[doseLevelList.getSelectedIndex()];
				
				getBarcodes(compound, organ, doseLevel);
			}
		});
		
		barcodeList = new ListBox();
		barcodeList.setVisibleItemCount(10);
		flowPanel.add(barcodeList);
		barcodeList.setSize("210px", "202px");
		
		FlowPanel flowPanel_1 = new FlowPanel();
		dockPanel.add(flowPanel_1, DockPanel.NORTH);
		final TextBox nameField = new TextBox();
		flowPanel_1.add(nameField);
		nameField.setText("GWT User");
		
				// Focus the cursor on the name field when the app loads
				nameField.setFocus(true);
				final Label errorLabel = new Label();
				flowPanel_1.add(errorLabel);
				final Button sendButton = new Button("Send");
				flowPanel_1.add(sendButton);
				
						// We can add style names to widgets
						sendButton.addStyleName("sendButton");
						
		nameField.selectAll();
		

		// Create the popup dialog box
		final DialogBox dialogBox = new DialogBox();
		dialogBox.setText("Remote Procedure Call");
		dialogBox.setAnimationEnabled(true);
		final Button closeButton = new Button("Close");
		// We can set the id of a widget by accessing its Element
		closeButton.getElement().setId("closeButton");
		final Label textToServerLabel = new Label();
		final HTML serverResponseLabel = new HTML();
		VerticalPanel dialogVPanel = new VerticalPanel();
		dialogVPanel.addStyleName("dialogVPanel");
		dialogVPanel.add(new HTML("<b>Sending name to the server:</b>"));
		dialogVPanel.add(textToServerLabel);
		dialogVPanel.add(new HTML("<br><b>Server replies:</b>"));
		dialogVPanel.add(serverResponseLabel);
		dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
		dialogVPanel.add(closeButton);
		dialogBox.setWidget(dialogVPanel);

		// Add a handler to close the DialogBox
		closeButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();
				sendButton.setEnabled(true);
				sendButton.setFocus(true);
			}
		});
		
		//get compounds
		getCompounds();
		getOrgans(null);
		getDoseLevels(null, null);		

		// Create a handler for the sendButton and nameField
		class MyHandler implements ClickHandler, KeyUpHandler {
			/**
			 * Fired when the user clicks on the sendButton.
			 */
			public void onClick(ClickEvent event) {
				sendNameToServer();
			}

			/**
			 * Fired when the user types in the nameField.
			 */
			public void onKeyUp(KeyUpEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
					sendNameToServer();
				}
			}

			/**
			 * Send the name from the nameField to the server and wait for a response.
			 */
			private void sendNameToServer() {
				// First, we validate the input.
				errorLabel.setText("");
				String textToServer = nameField.getText();
				if (!FieldVerifier.isValidName(textToServer)) {
					errorLabel.setText("Please enter at least four characters");
					return;
				}

				// Then, we send the input to the server.
				sendButton.setEnabled(false);
				textToServerLabel.setText(textToServer);
				serverResponseLabel.setText("");
				greetingService.greetServer(textToServer,
						new AsyncCallback<String>() {
							public void onFailure(Throwable caught) {
								// Show the RPC error message to the user
								dialogBox
										.setText("Remote Procedure Call - Failure");
								serverResponseLabel
										.addStyleName("serverResponseLabelError");
								serverResponseLabel.setHTML(SERVER_ERROR);
								dialogBox.center();
								closeButton.setFocus(true);
							}

							public void onSuccess(String result) {
								dialogBox.setText("Remote Procedure Call");
								serverResponseLabel
										.removeStyleName("serverResponseLabelError");
								serverResponseLabel.setHTML(result);
								dialogBox.center();
								closeButton.setFocus(true);
							}
						});
			}
		}
		
		// Add a handler to send the name to the server
				MyHandler handler = new MyHandler();
				sendButton.addClickHandler(handler);
				nameField.addKeyUpHandler(handler);

	}
	
	private String[] compounds;
	void getCompounds() {
		compoundList.clear();
		owlimService.compounds(new AsyncCallback<String[]>() {
			public void onFailure(Throwable caught) {
				Window.alert("Unable to get compounds.");				
			}
			public void onSuccess(String[] result) {
				compounds = result;
				for (String compound: result) {					
					compoundList.addItem(compound);					
				}				
			}
		});
	}
	
	private String[] organs;
	void getOrgans(String compound) {
		organList.clear();
		owlimService.organs(compound, new AsyncCallback<String[]>() {
			public void onFailure(Throwable caught) {
				Window.alert("Unable to get organs.");				
			}
			public void onSuccess(String[] result) {
				organs = result;
				for (String organ: result) {					
					organList.addItem(organ);					
				}				
			}
		});
	}
	
	private String[] doseLevels;
	void getDoseLevels(String compound, String organ) {
		doseLevelList.clear();
		owlimService.doseLevels(null, null, new AsyncCallback<String[]>() {
			public void onFailure(Throwable caught) {
				Window.alert("Unable to get dose levels.");				
			}
			public void onSuccess(String[] result) {
				doseLevels = result;				
				for (String doseLevel: result) {					
					doseLevelList.addItem(doseLevel);					
				}				
			}
		});
	}
	
	void getBarcodes(String compound, String organ, String doseLevel) {
		barcodeList.clear();
		owlimService.barcodes(compound, organ, doseLevel, new AsyncCallback<String[]>() {
			public void onFailure(Throwable caught) {
				Window.alert("Unable to get dose levels.");				
			}
			public void onSuccess(String[] result) {								
				for (String barcode: result) {					
					barcodeList.addItem(barcode);					
				}				
			}
		});
	}
}
