"use strict";

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
  let msg = Object.keys(evt.cy.nodes('[type="'+nodeType.MSG_RNA+'"]')[0].data('weight'));
  $('#msgRNAWeight').empty();
  $.each(msg, function (i, item) {
    $('#msgRNAWeight').append($('<option>', {value: item, text: item}));
  });

  /* add the list of weights available for microRNA nodes */
  let mic = Object.keys(evt.cy.nodes('[type="'+nodeType.MICRO_RNA+'"]')[0].data('weight'));
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
 * Enable/Disable interface elements depending on whether we are using a single
 * or double panel visualization.
 * @param {number} panels The numer of panels that are currently being available
 * within the visualization 1 - single panel visualization; 2 - double panel
 * visualization
 */
function updateInterfaceControls(panels=1){
  // single display visualization
  if( panels === 1 ){
    // only left panel is available for selection
    $("#panelSelect option[value=0]").prop("selected", true);
    $("#panelSelect option[value=0]").attr("disabled", false);
    $("#panelSelect option[value=1]").attr("disabled", true);
    $("#panelSelect option[value=2]").attr("disabled", true);
    // Intersection highlighting
    $("#showIntersectionCheckbox").prop("checked", false);
    $("#showIntersectionCheckbox").attr("disabled", true);
    $("#showIntersectionCheckbox").trigger("change");
    // Merging networks
    $("#mergeNetworkButton").attr("disabled", true);
    // Close right panel
    $("#closeRightPanelButton").attr("disabled", true);
  }
  // double display visualization
  else{
    // left, right and intersection options are available for selection
    $("#panelSelect option[value=0]").attr("disabled", false);
    $("#panelSelect option[value=1]").prop("selected", true);
    $("#panelSelect option[value=1]").attr("disabled", false);
    $("#panelSelect option[value=2]").attr("disabled", false);
    // Intersection highlighting
    $("#showIntersectionCheckbox").prop("checked", false);
    $("#showIntersectionCheckbox").attr("disabled", false);
    $("#showIntersectionCheckbox").trigger("change");
    // Merging networks
    $("#mergeNetworkButton").attr("disabled", false);
    // Close right panel
    $("#closeRightPanelButton").attr("disabled", false);
  }
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

  updateInterfaceControls(1);
}


/**
 * If we consider that is is possible to get a linear interpolation that goes
 * both between a minimum and maximum values, and a base and end color, then it
 * is possible to find, given a value within the linear interpolant, a
 * corresponding color value.
 * To calculate the color, we simply
 * @param {float} value the value I want to map to a specific color value
 * @param {float} min the value associated to the base color
 * @param {float} max the value associated to the end color
 * @param threshold the value where white is located
 * @param {RGB} baseColor a string representation of the RGB components of the
 * base color
 * @param {RGB} endColor a string representation (or array of values) of the RGB components of the
 * end color
 * @return a representation of the input value, as a color in RGB representation
 */
function valueToColor(value, min, max, threshold, baseColor, endColor){
  /* can't assign a color if there is no range */
  if( min ===  max )
    return null;
  /* if both colors are equal, then all nodes should have the same color, as
   * there is no real mapping */
  if( baseColor === endColor )
    return baseColor;

  /* if the value is to the left of white, then the assigned color will be
   * interpolated linearly between [base - white] */
  if( value <= threshold){
    if( value <= min )
      return baseColor;
    endColor = "#FFFFFF";
    max = threshold;
  }
  /* if the value is to the right of white, then the assigned color will be
   * interpolated linearly between [white - end] */
  else{
    if( value >= max )
      return endColor;
    baseColor = "#FFFFFF";
    min = threshold;
  }

  baseColor = baseColor.substring(1); // remove #
  endColor = endColor.substring(1); // remove #
  // RGB components of the base color
  var rb = parseInt(baseColor.substring(0,2), 16);
  var gb = parseInt(baseColor.substring(2,4), 16);
  var bb = parseInt(baseColor.substring(4), 16);

  // RGB components of the end color
  var re = parseInt(endColor.substring(0,2), 16);
  var ge = parseInt(endColor.substring(2,4), 16);
  var be = parseInt(endColor.substring(4), 16);

  var perc = (value - min) / (max - min);

  var r = rb+(perc*(re-rb));
  var g = gb+(perc*(ge-gb));
  var b = bb+(perc*(be-bb));

  r = Math.trunc(r);
  r = ("00" + r.toString(16)).slice(-2);
  g = Math.trunc(g);
  g = ("00" + g.toString(16)).slice(-2);
  b = Math.trunc(b);
  b = ("00" + b.toString(16)).slice(-2);

  return "#"+r+g+b;
}

/**
 * @param {string} hex - a color expressed as an hex value with format <#RRGGBB>
 * where each pair RR, GG, and BB are an hex number between 0 and FF (as used)
 * by browsers
 */
function hex2v(hex){
  hex = hex.substring(1);
  var r = parseInt(hex.substring(0,2), 16);
  var g = parseInt(hex.substring(2,4), 16);
  var b = parseInt(hex.substring(4), 16);

  var cmax = Math.max(r, g, b);
  var cmin = Math.min(r, g, b);

  var d = cmax - cmin;
  if( d === 0 )
    return 0;

  switch( cmax ){
    case r:
      return 60*( ((g-b)/d)%6 );
    case g:
      return 60*( ((b-r)/d)+2 );
    case b:
      return 60*( ((r-g)/d)+4 );
  }

}

/**
 * Given a color, specified in the HSV color model, it returns the corresponding
 * color as an html RGB tuple
 * @param {float} h - hue component, in the range [0,360]
 * @param {float} s - saturation component, in the range [0,1]
 * @param {float} v - value component, in the range [0,1]
 */
function hsv2rgb(h, s, v){

  if( s > 1 )
    s = 1;
  if( v > 1 )
    v = 1;

    var r,g,b;
    r = g = b = 0.0;
    var f, p, q, t;
    var k;
    if (s == 0.0) {    // achromatic case
       r = g = b = v;
    }
    else {    // chromatic case
      if (h == 360.0)
        h=0.0;
      h = h/60.0;
      k = Math.round(h);
      f = h - (k*1.0);

      p = v * (1.0 - s);
      q = v * (1.0 - (f*s));
      t = v * (1.0 - ((1.0 - f)*s));

      switch (k) {
        case 0:
          r = v;  g = t;  b = p;
          break;
        case 1:
          r = q;  g = v;  b =  p;
          break;
        case 2:
          r = p;  g = v;  b =  t;
          break;
        case 3:
          r = p;  g = q;  b =  v;
          break;
        case 4:
          r = t;  g = p;  b =  v;
          break;
        case 5:
          r = v;  g = p;  b =  q;
          break;
      }
    }
    r = Math.trunc(r*255);
    r = ("00" + r.toString(16)).slice(-2);
    g = Math.trunc(g*255);
    g = ("00" + g.toString(16)).slice(-2);
    b = Math.trunc(b*255);
    b = ("00" + b.toString(16)).slice(-2);
    var rgb = [r, g, b];
    return "#"+r+g+b;
  }
