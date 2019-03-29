// "use strict";

/** types of nodes that can be added to the visualization */
const nodeType = Object.freeze({
  mRNA: 'MSG_RNA',//: "mRNA",
  microRNA: 'MICRO_RNA', //: "microRNA",
});

/** default colours for nodes in the graph */
const nodeColor = Object.freeze({
  MSG_RNA: '#007f7f',
  MICRO_RNA: '#827f00',
  HIGHLIGHT: '#ffde4c',
});

/** list of shapes that can be used to draw a node */
const nodeShape = Object.freeze({
  MSG_RNA: "ellipse",
  MICRO_RNA: "pentagon",
});

/** default colours for lines in the graph */
const edgeColor = Object.freeze({
  REGULAR: '#989898',
  HIGHLIGHT: '#ffde4c',
});

/**
 * Display color scale modal dialog
 * A dialog window needs to be display when the user wants to appply a color
 * scale to the nodes in the network based on the values associated to nodes.
 * Upon display of the dialog, values to fill up the select fields are loaded,
 * based on the data currently on display.
 *
 * @param {event} evt The click evented captured from the user interaction with
 * the graph's context menu
 */
function showColorScaleDialog(evt){

  /* Set the position for dialog display and identify the target graph depending
   * on the panel where the interaction was triggered by the user */
  let left = '0%';
  let id = MAIN_ID;
  if( evt.cy.container().id === 'rightDisplay' ){
    left = '50%';
    id = SIDE_ID;
  }

  /* append modal dialogs to canvas */
  $('#colorScaleDialog')
    .css('visibility', 'visible')
    .css('left', left) // define the position of the dialog window
    .css('color', 'black')
    .data('id', id) // identify the target graph
    ;

  /* re-construct the list of weights available for msgRNA nodes, as this
   * changes from graph to graph */
  let msg = Object.keys(evt.cy.nodes('[type="'+nodeType.mRNA+'"]')[0].data('weight'));
  $('#msgRNAWeight').empty();
  $.each(msg, function (i, item) {
    $('#msgRNAWeight').append($('<option>', {value: item, text: item}));
  });

  /* add the list of weights available for microRNA nodes */
  let mic = Object.keys(evt.cy.nodes('[type="'+nodeType.microRNA+'"]')[0].data('weight'));
  $('#microRNAWeight').empty();
  $.each(mic, function (i, item) {
    $('#microRNAWeight').append($('<option>', {value: item, text: item}));
  });
}

/**
 * Display label change modal dialog
 * A dialog window needs to be displayed when the user wants to change the label
 * used to identify a node in the graph visualization.
 *
 * @param {event} evt The click evented captured from the user interaction with
 * the graph's context menu
 */
function showChangeLabelDialog(evt){

  /* Set the position for dialog display and identify the target graph depending
   * on the panel where the interaction was triggered by the user */
  let left = '0%';
  let id = MAIN_ID;
  if( evt.cy.container().id === 'rightDisplay' ){
    left = '50%';
    id = SIDE_ID;
  }

  /* append modal dialogs to canvas */
  $('#changeLabelDialog')
    .css('visibility', 'visible')
    .css('left', left) // define the position of the dialog window
    .css('color', 'black')
    .data('id', id) // identify the target graph
    .data('nodeid', evt.target.data('id'))
    ;

  /* capture the node's current label and use it to fill the input field */
  $('#nodeLabel').val(evt.target.data('label'));
}

/**
 * Add Pop-up div element to DOM
 * In order to display pop-up information associated to the nodes in the graph,
 * we need to make available the corresponding DOM structures. These are added
 * or modified whenever the network visualization module is called within
 * toxygates
 */
function addPopperDiv(){
  /* create the main div container for the pop-up display */
  let divPopper = $('<div />',{
    id: 'nodePopper',
    class: 'popper'
  });
  /* add contents to the pop-up display */
  let table = $('<table/>');
  table.append('<tr><td>Label</td><td id="label"></td></tr>');
  table.append('<tr><td>Type</td><td id="type"></td></tr>');
  table.append('<tr><td>Probe</td><td id="probe"></td></tr>');
  divPopper.append(table);
  /* add the structure to the body of the application */
  $('body').append(divPopper);
}

/**
 * Enable interface controls for single panel visualization
 * Controls available to users are different depending on whether they are using
 * a single or a dual panel visualization. This method makes available the
 * controls for SINGLE panel visualization
 */
function setSinglePanelInterface(){
  /* disable right and both panel selection, and select main panel */
  $('#panelSelect option[value='+SIDE_ID+']').attr('disabled', true);
  $('#panelSelect option[value='+BOTH_ID+']').attr('disabled', true);
  $('#panelSelect option[value='+MAIN_ID+']').prop("selected", true);
  /* disable intersection controls */
  $("#showIntersectionCheckbox").prop("checked", false);
  $("#showIntersectionCheckbox").attr("disabled", true);
  $("#showIntersectionCheckbox").trigger("change");
  /* disable merging controls */
  $("#mergeNetworkButton").attr("disabled", true);
  /* disable panel closing controls */
  $("#closeRightPanelButton").attr("disabled", true);
}

/**
 * Enable interface controls for dual panel visualization
 * Controls available to users are different depending on whether they are using
 * a single or a dual panel visualization. This method makes available the
 * controls for DUAL panel visualization
 *
 * @param {int} id The id of the panel that will be selected after the controls
 * are made available
 */
function setDualPanelInterface(id=SIDE_ID){
  /* enable all options for panel selection and set the current one as selected */
  $('#panelSelect option[value='+SIDE_ID+']').attr("disabled", false);
  $('#panelSelect option[value='+BOTH_ID+']').attr("disabled", false);
  $('#panelSelect option[value='+id+']').prop("selected", true);
  /* make intersection controls available */
  $("#showIntersectionCheckbox").prop("checked", false);
  $("#showIntersectionCheckbox").attr("disabled", false);
  /* make merging controls available */
  $("#mergeNetworkButton").attr("disabled", false);
  /* make panel closing controls available */
  $("#closeRightPanelButton").attr("disabled", false);
}


/**
 * Remove the right display (DOM element) from the interface and handle the
 * deactivation of any related interace components associated with it.
 */
function removeRightDisplay(){
  // We remove the panel from the DOM
  $("#rightDisplay").remove();
  // Remove need for side-panel consideration in left panel
  $("#leftDisplay").removeClass("with-side");

  setSinglePanelInterface();
}


/**
 * Linear interpolation of color
 * For any given value, linearly interpolate the color corresponding for that
 * value assuming that a different hue is used for negative and positive values,
 * with white associated to zero values.
 * Minimum and maximum values are takes as capping values, in the sense that
 * anything below the minum cap or above the maximum, will be assigned the
 * corresponding min (or max) color.
 *
 * @param {float} val the value I want to map to a specific color value
 * @param {float} min cap for negative values, below values are assigned negColor
 * @param {float} max cap for positive values, above values are assigned posColor
 * @param {RGB} negColor the html color used for the negative side of the scale
 * @param {RGB} posColor the html color used for the positive side of the scale
 * @return a representation of the input value, as a color in RGB representation
 * or undefined if the target value is not a number (as defined by isNaN() )
 */
function valueToColor(val, min=-1, max=1, negColor='#FF0000', posColor='#0000FF'){
  /* Handle all the extreme cases first. This also handle the cases when min
   * and max have the same value */
  /* 0. the value is not a valid number */
  if (isNaN(val))
    return undefined;
  /* 1. value is at or below the min */
  if( val <= min ) return negColor;
  /* 2. value is at or above the max */
  if( val >= max ) return posColor;

  /* if val is negative, we interpolate between white and negColor, using the
   * absolute value of val and min */
  if( val < 0 ){
    val = Math.abs(val); // take the absolute value
    max = Math.abs(min);   // take the absolute value
    posColor = negColor.substring(1); // change the ending color and trim '#'
  }
  else
    posColor = posColor.substring(1); // only trim the leading hash('#') char

  // RGB components of the end color
  var re = parseInt(posColor.substring(0,2), 16);
  var ge = parseInt(posColor.substring(2,4), 16);
  var be = parseInt(posColor.substring(4), 16);

  var perc = val / max;

  var r = 255+(perc*(re-255));
  var g = 255+(perc*(ge-255));
  var b = 255+(perc*(be-255));

  r = Math.trunc(r);
  r = ("00" + r.toString(16)).slice(-2);
  g = Math.trunc(g);
  g = ("00" + g.toString(16)).slice(-2);
  b = Math.trunc(b);
  b = ("00" + b.toString(16)).slice(-2);

  return "#"+r+g+b;
}
