"use strict";
/* identifiers for cystoscape display panels */
const MAIN_ID = 0;
const SIDE_ID = 1;

// this are the main and side graphs - as Cytoscape objects
var vizNet = [null, null];
// this are also the Graphs - using Network structure
var toxyNet = [null, null];

/** ------------------------- FORM HANDLERS ------------------------------ **/
/**
 * Adjust interface controls whenever the user changes the active visualization
 * panel
 */
$(document).on("change", "#panelSelect", function(){
  // Determine the selected active panel
  var id = $("#panelSelect").val();
  // Display the appropriate layout
  if( toxyNet[id].layout !== "null" )
    $("#layoutSelect").val(toxyNet[id].layout.options.name);
  else
    $("#layoutSelect").val("null");
  // Display the correct value for hiding of unconnected nodes
  $("#hideNodesCheckbox").prop("checked", toxyNet[id].unconnected !== null);
});

/**
 * Changes the layout of the nodes in the network according to the user's
 * selection.
 */
$(document).on("change", "#layoutSelect", function (){
  var id = $("#panelSelect").val();
  // nothing to do if there is no network
  if( vizNet[id] === null ) return;
  // get the option selected by the user
  var opt = $("#layoutSelect").find(":selected").val();

  // update the layout accordingly
  // $("#waitModal").attr("style", "display:block");
  window.addPendingRequest();
  toxyNet[id].layout = vizNet[id].makeLayout(vizNet[id].updateLayout(opt));
  setTimeout(function(){
    toxyNet[id].layout.run();
    // $("#waitModal").attr("style", "display:none");
    window.removePendingRequest();

  });
});

/**
 * Hide (or not) from the visualiation all nodes that are unconnected, that is,
 * nodes that are not linked to any other node within the network.
 */
$(document).on("change", "#hideNodesCheckbox", function(){
  // Determine on which panel the changes should be applied
  var id = $("#panelSelect").val();
  // If no network is on display, we can safely return
  if( vizNet[id] === null ) return;
  // When checked, we hide the un-connected nodes of the network
  if( $("#hideNodesCheckbox").is(":checked") ){
    toxyNet[id].unconnected = vizNet[id].hideUnconnected();
  }
  else{
    vizNet[id].showUnconnected(toxyNet[id].unconnected); // show unconnected
    toxyNet[id].unconnected = null; // clear the list of hidden nodes
  }

  // Since the network has been modified, the corresponding layout needs to be
  // re-run in order to account for these changes
  var opt = $("#layoutSelect").find(":selected").val();
  if( toxyNet[id].layout !== "preset" ){
    toxyNet[id].layout = vizNet[id].makeLayout(vizNet[id].updateLayout(opt));
    toxyNet[id].layout.run();
  }
});

/**
 * Show (or not) a Right-side panel for additional network visualization. The
 * user typically works only on the visualization of a single network structure,
 * but by using an extra panel it is possible to load a previously saved
 * network to work with.
 * TODO this type of interaction should be made available through the selection
 * of the network to be displayed and not only with a checkbox
 */
$(document).on("change", "#showRightCheckbox", function(){
  // See if the checkbox is checked (enable display)
  if( $("#showRightCheckbox").is(":checked") ){
    // Have the left-panel reduce its size to half of the available display
    $("#leftDisplay").addClass("with-side");
    // Define the new right-side panel, together with its elements, and add it
    // to the DOM
    $("#display")
      .append('<div id="rightDisplay" class="sub-viz"></div>')
      .ready(function(){
        var right = $("#rightDisplay");
        right.data("idx", SIDE_ID);
        vizNet[SIDE_ID] = cytoscape({
          container: right,
          styleEnabled: true,
        });
        vizNet[SIDE_ID].initStyle();        // default style for network elements
        vizNet[SIDE_ID].initContextMenu();  // default context menu
        // Here I add elements to the network display... based on the network
        // currently stored in convertedNetwork
        changeNetwork(SIDE_ID);

        vizNet[MAIN_ID].resize();
        vizNet[MAIN_ID].fit();
      });


  }
  // When the checkbox is not checked (disabled display of right-side panel)
  else{
    // we remove the panel from the DOM
    $("#rightDisplay").remove();
    // remove the information from the corresponding variables
    /* As this should be linked to the loading of a previosly saved graph, it
     doesn't make sense to keep the previous elements, even if the proper
     functionality is not yet implemented */
    vizNet[SIDE_ID] = null;
    toxyNet[SIDE_ID] = null;

    // modify the left-display, so that it takes all available display space
    $("#leftDisplay").removeClass("with-side");
    // re-layout the elements within the visualization panel
    vizNet[MAIN_ID].resize();
    vizNet[MAIN_ID].fit();

  }
});


/** ---------------------- UPDATE NODE MODAL ---------------------------- **/
/**
 * Handle updates made on a node through the corresponding modal. Once the user
 * selects to update, we check each of the node's properties, and whenever we
 * find any change, we register them on the corresponding instance in the graph
 */
$(document).on("click", "#updateNodeModal #updateNode", function(event){
  var id = $("#updateNodeModal").data("idx");
   /* reference to the node currently selected on the visualization */
   var node = vizNet[id].nodes().getElementById($("#nodeID").val());
   /* nodeID is not to be changed by the user */
   /* nodeLabel */
   var label = $("#updateNodeModal #nodeLabel").val();
   node.data("label", label);

   /* nodeType */
   var type = $("#updateNodeModal #nodeType").val()
   node.data("type", type);

   /* nodeWeights */
   var selection = $("#updateNodeModal #nodeWeights").val();
   if( selection !== null ){
     var w = node.data("weight");
     w[selection] = $("#updateNodeModal #weightValue").val();
     node.data("weight", w);
   }

   /* nodeColor */
   var color = $("#updateNodeModal #nodeColor").val();
   node.data("color", color);
   node.style("background-color", color);

   /* nodeShape */
   var shape = $("#updateNodeModal #nodeShape").val();
   node.style("shape", shape);

   /* hide the modal */
   var modal = $(event.target).data().modal;
   $("#"+modal).hide();
 });

 /**
  * Whenever the user changes the type of a node, default values for color and
  * shape are displayed.
  */
$(document).on("change", "#updateNodeModal #nodeType", function(evt){
  // get the text from the currently selected option
  var type = $("#nodeType :selected").text();
  // update default color for this type of node
  $("#nodeColor").val(nodeColor[type]);
  // update default shape for this type of node
  $("#nodeShape").val(nodeShape[type]);
});

 /**
  * Handle the display of the correct value when the user chooses a different
  * weight from available list.
  */
$(document).on("change", "#updateNodeModal #nodeWeights", function(evt){
  var node = vizNet.nodes().getElementById($("#nodeID").val());
  var selection = $("#nodeWeights").val();
  if( selection !== null )
    $("#weightValue").val(node.data()["weight"][selection]);
  else
    $("#weightValue").val("");
});

// /** ---------------------- UPDATE GRAPH COLORING ------------------------ **/
// /**
//  * Apply the user defined color scale to all nodes of a particular type.
//  */
// $(document).on("click", "#graphColorModal #colorGraph", function(evt){
//   // type of node we should color, and weight used for the linear scaling
//   var type = $("#graphColorModal #graphColorTo").val();
//   var w = $("#graphColorModal #graphColorBy").val();
//   // we only apply a color scale if both values have been selected
//   if ( type !== null && w !== null ){
//     // get a collection of nodes to color
//     var trg = vizNet.nodes("[type = '"+type+"']");
//     // get min and max values for the color scale, together with the value
//     // associated with white
//     var min = Number($("#graphColorModal #minRange").val());
//     var max = Number($("#graphColorModal #maxRange").val());
//     var white = Number($("#graphColorModal #whiteRange").val());
//     // apply the color change to all nodes within the collection
//     trg.forEach(function(ele){
//       var d = ele.data("weight");
//       var color = valueToColor(d[w], min, max, white,
//         $("#graphColorModal #minColor").val(),
//         $("#graphColorModal #maxColor").val());
//       if( color !== null ){
//         ele.data("color", color);
//         ele.style('background-color', color);
//       }
//     });
//   }
//
//   /* hide the modal after color has been applied to nodes */
//   var modal = $(event.target).data().modal;
//   $("#"+modal).hide();
//
// });
//
// /**
//  * Color scales are applied to ALL nodes of a particular type.
//  * Each time a user select the type of node on which to apply color, we reload
//  * the list of weights that can be used to generate the color scale.
//  */
// $(document).on("change", "#graphColorModal #graphColorTo", function(evt){
//   // the type of node we will be coloring
//   var type = ($("#graphColorModal #graphColorTo").val());
//   // get a sample node from the given type
//   var trg = vizNet.nodes("[type = '"+type+"']")[0];
//   var weights = Object.keys(trg.data()["weight"]);
//   if( weights !== null && weights !== undefined ){
//     $("#graphColorModal #graphColorBy").empty();
//     $("#graphColorModal #graphColorBy").append(new Option("Select...",null));
//     for(var i=0; i<weights.length; ++i){
//       $("#graphColorModal #graphColorBy").append(new Option(weights[i], weights[i]));
//     }
//   }
//   // reset values for the color scale
//   $("#graphColorModal #minRange").val("");
//   $("#graphColorModal #maxRange").val("");
//   $("#graphColorModal #colorRange").val(50);
//   $("#graphColorModal #whiteRange").val("");
// });
//
// /**
//  * Handle the initialization of color scale parameters whenever the user select
//  * a weight.
//  */
// $(document).on("change", "#graphColorModal #graphColorBy", function(evt){
//   //  get the collection of nodes to which the color scale is applied
//   var type = $("#graphColorModal #graphColorTo").val();
//   var trg = vizNet.nodes("[type = '"+type+"']");
//   // get the weight used as base for the color scale
//   var w = $("#graphColorModal #graphColorBy").val();
//   if( w !== null ){
//     // calculate the minimum value for the given weight
//     var min = trg.min(function(ele){
//       var d = ele.data("weight");
//       return d[w];
//     });
//     // calculate the maximum value for the given weight
//     var max = trg.max(function(ele){
//       var d = ele.data("weight");
//       return d[w];
//     });
//     // update minimum and maximim values
//     $("#graphColorModal #minRange").val(min.value.toFixed(2));
//     $("#graphColorModal #minRange").attr("max", max.value.toFixed(2));
//
//     $("#graphColorModal #maxRange").val(max.value.toFixed(2));
//     $("#graphColorModal #maxRange").attr("min", min.value.toFixed(2));
//     // update white level range
//     var threshold = ((max.value+min.value)/2);
//     $("#graphColorModal #colorRange").val(50);
//     $("#graphColorModal #whiteRange").val(threshold.toFixed(2));
//   }
// });
//
// /**
//  * Handle the interaction of the user with the slider that determines the
//  * position of the white level in the coloring scale.
//  */
// $(document).on("input change", "#graphColorModal #colorRange", function(evt){
//   var min = Number($("#graphColorModal #minRange").val());
//   var max = Number($("#graphColorModal #maxRange").val());
//   var threshold = min + $("#graphColorModal #colorRange").val()*(max-min)/100;
//   $("#graphColorModal #whiteRange").val(threshold.toFixed(2));
// });
//
// /**
//  * Handle the update of white level whenever the user changes the color scale by
//  * modifying the minimum value to use.
//  * Also make sure that minimum value selected by the user is kept within a valid
//  * range.
//  */
// $(document).on("change", "#graphColorModal #minRange", function(evt){
//   // get the value input by the user
//   var min = $("#graphColorModal #minRange").val();
//   // check that the value is not empty
//   if( min === "" ){
//     min = $("#graphColorModal #maxRange").attr("min");
//     $("#graphColorModal #minRange").val(min);
//   }
//   min = Number(min);
//   var max = Number($("#graphColorModal #maxRange").val());
//   // check that the value is within a valid range and if not, make the
//   // corresponding adjustments
//   if( min > max ){
//     min = max;
//     $("#graphColorModal #minRange").val(min);
//   }
//   // the white level mantains its relative distance to minimum and maximum
//   // values, but its actual value is updated accordingly
//   var threshold = min + $("#graphColorModal #colorRange").val()*(max-min)/100;
//   $("#graphColorModal #whiteRange").val(threshold.toFixed(2));
// });
//
// /**
//  * Handle the update of white level whenever the user changes the color scale by
//  * modifying the minimum value to use.
//  * Also make sure that the maximum value selected by the user is kept within a
//  * valid range
//  */
// $(document).on("change", "#graphColorModal #maxRange", function(evt){
//   // get the value input by the user
//   var max = $("#graphColorModal #maxRange").val();
//   // check that the value is not empty
//   if( max === "" ){
//     max = $("#graphColorModal #minRange").attr("max");
//     $("#graphColorModal #maxRange").val(max);
//   }
//   max = Number(max);
//   var min = Number($("#graphColorModal #minRange").val());
//   // check that the value is within a valid range and if not, make the
//   // corresponding adjustments
//   if( max < min ){
//     max = min;
//     $("#graphColorModal #maxRange").val(max);
//   }
//   // the white level mantains its relative distance to minimum and maximum
//   // values, but its actual value is updated accordingly
//   var threshold = min + $("#graphColorModal #colorRange").val()*(max-min)/100;
//   $("#graphColorModal #whiteRange").val(threshold.toFixed(2));
// });
//
// /** ---------------------------- FILTER GRAPH ---------------------------- **/
// /**
//  * Handle the application of filters to the visualization.
//  * All filters currently active (listed) will be applied to the visualization.
//  */
// $(document).on("click", "#filterModal #filterNodes", function(evt){
//   /* since the application of filters could translate in several drawing
//    * iterations, we batch them to optimize rendering */
//   vizNet.batch(function(){
//     // make all elements in the graph visible again, as elements that should not
//     // be filtered by the current list of filters could remain invisible due to
//     // previous filtering rules
//     vizNet.nodes().style("display", "element");
//
//     // get the current list of filters to be applied
//     var list = $("#filterModal #filterList")[0];
//     for(var i=0; i<list.childElementCount; ++i ){
//       // get the details of each filter included within the list
//       var data = list.children[i].dataset;
//       var filtered;
//       if(data["type"] === "degFilter"){
//         // select the group of nodes that should be filtered out from the
//         // visualization
//         filtered = vizNet.nodes().filter(function(ele){
//           return ele.degree(false) < parseInt(data["degree"]);
//         });
//       }
//       // by setting their display to "none", we effectively prevent nodes to be
//       // show, without permanently removing them from the graph
//       filtered.style('display', 'none');
//     }
//   });
//
//   /* onde all filters have beeb applied, hide the modal */
//   var modal = $(event.target).data().modal;
//   $("#"+modal).hide();
// });
//
// /**
//  * Handle the addition of filter rules to the corresponding list. Later, and
//  * upon user selection, filters are applied to the network visualization
//  */
// $(document).on("click", ".modal-add", function(event){
//   /* list of filters currently defined */
//   var list = $("#filterModal #filterList")[0];
//
//   /* create a new filter element */
//   var type = $(event.target).data().type;
//   var node = document.createElement("li");
//   node.setAttribute("data-type", type);
//
//   /* define the filter element */
//   var html = "";
//   if( type === "degFilter"){
//     var deg = Math.trunc($("#nodeDegree").val());
//     deg = deg >= 0 ? deg : 0;
//     node.setAttribute("data-degree", deg);
//     html += ("Degree at least "+deg);
//     html +=  "<span class='modal-remove'>&times;</span>";
//
//   }
//   node.innerHTML = html;
//
//   /* add the new filter to the list of filters */
//   list.appendChild(node);
// });
//
// /**
//  * Handle the removal of a specific filter from the corresponding list.
//  */
// $(document).on("click", ".modal-remove", function(event){
//   /* the list of filters */
//   var list = $("#filterModal #filterList")[0]
//   /* removal of the element selected by the user */
//   list.removeChild(event.target.parentElement);
// })

/** ------------------------------------------------------------------ **/
/**                    Other MODAL Functions                           **/
/** ------------------------------------------------------------------ **/

/**
* Hide the corresponding modal when the close option is selected.
* No changes are applied and whatever information the user added to the modal
* components is lost.
*/
$(document).on("click", ".modal-close", function(event){
  var modal = $(event.target).data().modal;
  $("#"+modal).hide();
});

/**
* Hide the corresponding modal when the cancel option is selected.
* No changes are applied and whatever information the user added to the modal
* components is lost.
*/
$(document).on("click", ".modal-cancel", function(event){
  var modal = $(event.target).data().modal;
  $("#"+modal).hide();
});

/** ------------------------------------------------------------------ **/
/**          Required methods for toxygates integration                **/
/** ------------------------------------------------------------------ **/

/**
 * Called by Toxygates once the user interface HTML has been loaded and all
 * scripts have been injected, it initialize the main and side visualization
 * so that they later can be used for network display.
 */
function onReadyForVisualization(){
  // mainDisplay initialization - the one currently being used by the user to
  // work, and currently linked with the background options of toxygates
  $("#display")
    .append('<div id="leftDisplay" class="sub-viz"></div>')
    .ready(function(){
      var left = $("#leftDisplay");
      left.data("idx", MAIN_ID);

      vizNet[MAIN_ID] = cytoscape({
        container: left,
        styleEnabled: true,
      });
      vizNet[MAIN_ID].initStyle();        // default style for network elements
      vizNet[MAIN_ID].initContextMenu();  // default context menu

      // vizNet[MAIN_ID].on("select", "node", onNodeSelection(MAIN_ID));
      changeNetwork(MAIN_ID);
    });

  /* Move the Cytoscape context menu into the modal GWT network visualiaztion
   * dialog, because otherwise input to it will be intercepted */
  $(".cy-context-menus-cxt-menu").appendTo($(".gwt-DialogBox"));
  $(".cy-context-menus-cxt-menu").hide();

}

/**
 *
 */
// function onNodeSelection(id){
//   return function(evt){
//     console.log("entered on selection");
//     var n = evt.target;
//     var otherID = (id+1)%2;
//     if( vizNet[otherID] !== null ){
//       console.log("trying to select: "+n.id()+" on "+otherID);
//       vizNet[otherID].$([id=n.id()]).selected();
//     }
//   }
// }

/**
 * Called by Toxygates on an already running network visualization dialog when
 * it wants to switch to a different network, which has been placed in
 * window.toxyNet.
 * Method used to set the network to be displayed in either the main or side
 * display.
 * It is automatically called by Toxygates on load of a new visualization, or
 * by the user in the case of upgrading the side display only.
 * @param {int} id whether the network should be added to the main display
 * (id == 0) or to the side display (id == 1)
 */
function changeNetwork(id=MAIN_ID){
  /* convertedNetwork is the object where toxygates stores the network, using a
   * JSON style string */
  toxyNet[id] = new Network(convertedNetwork["title"],
    convertedNetwork["interactions"],
    convertedNetwork["nodes"]);

  /* add the loaded network to the corresponding display and do the necesary
   * transformations to fit the graph to the current display size */
  vizNet[id].elements().remove(); // remove all previous elements
  vizNet[id].add(toxyNet[id].getCytoElements());

  // we hide/show unconnected nodes based on user selection
  if( $("#hideNodesCheckbox").is(":checked") ){
    toxyNet[id].unconnected = vizNet[id].hideUnconnected();
  }

  /* if the nodes had no position, and the user has previously selected a layout
   * option, apply it tho the recently loaded network */
  var layout = $("#layoutSelect").val(); // UI selected layout
  if( toxyNet[id].layout === "null" && layout !== "null"){
    toxyNet[id].layout = vizNet[id].makeLayout(vizNet[id].updateLayout(layout));
    toxyNet[id].layout.run();
  }
  vizNet[id].fit();

}

/**
 * Called by Toxygates to get the desired height, in pixels, of the user
 * interaction div
 */
function uiHeight(){
  return 92;
}

/**
 * Updates toxyNet with the changes made to vizNet
 */
function updateToxyNet(){
  var title = toxyNet["title"];
  var nodes = vizNet[MAIN_ID].getToxyNodes();
  var edges = vizNet[MAIN_ID].getToxyInteractions();

  toxyNet[MAIN_ID] = new Network(title, edges, nodes);
}
