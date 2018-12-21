"use strict";
/* identifiers for cystoscape display panels */
const MAIN_ID = 0; // left-side
const SIDE_ID = 1; // right-side

// this are the main and side graphs - as Cytoscape objects
var vizNet = [null, null];
// this are also the Graphs - using Network structure
var toxyNet = [null, null];

/** ------------------------- FORM HANDLERS ------------------------------ **/
/**
 * The user is able to interact, using the same interface, with the graphs shown
 * on the left and right side panel only by turns. By changing the value of this
 * component, the user chooses to which of the two, the results of the
 * interaction should be applied.
 * Adjust interface controls whenever the user changes the active visualization
 * panel
 */
$(document).on("change", "#panelSelect", function(){
  // Determine the selected active panel
  var id = $("#panelSelect").val();

  if( id == 2 ){
    $("#layoutSelect").val("null");
    $("#hideNodesCheckbox").attr("disabled", true);
    return;
  }

  // Display the appropriate layout
  if( toxyNet[id].layout !== "null" )
    $("#layoutSelect").val(toxyNet[id].layout.options.name);
  else
    $("#layoutSelect").val("null");

  // Display the correct value for hiding of unconnected nodes
  $("#hideNodesCheckbox").prop("checked", toxyNet[id].unconnected !== null);
});

/**
 * Applies the selected layout, to the currently active graph.
 */
$(document).on("change", "#layoutSelect", function (){
  // Determine which panel is currently active
  var id = $("#panelSelect").val();
  // If the selected panel has no information, then is no need to perform any
  // routine
  if( vizNet[id] === null ) return;

  // Retrieve the selected layout
  var opt = $("#layoutSelect").find(":selected").val();

  // If we need to apply a layout to the interserction of two networks
  if( id == 2){

    /* Define a collection for the intersection of both networks*/
    var intersection = vizNet[MAIN_ID].nodes().intersection(vizNet[SIDE_ID].nodes());

    /* Define collections for the remainders of both networks */
    var othersMain = vizNet[MAIN_ID].nodes().difference(intersection);
    var othersSide = vizNet[SIDE_ID].nodes().difference(intersection);

    /* layout only the intersection of both networks */
    var removedMain = vizNet[MAIN_ID].remove(othersMain);
    vizNet[MAIN_ID].layout = vizNet[MAIN_ID].makeLayout(vizNet[MAIN_ID].updateLayout(opt));

    /* once the positioning of the nodes is completed, position the nodes on
     * both networks, and arrange the remainder nodes for both networks */
    vizNet[MAIN_ID].layout.promiseOn('layoutstop').then(function(evt){

      /* position the intersected nodes in the side panel */
      vizNet[MAIN_ID].nodes().forEach(function(node){
        vizNet[SIDE_ID].$("#"+node.id())
          .position(node.position());
      });

      /* position the remainder nodes in the main panel */
      var bb = vizNet[MAIN_ID].extent();
      var startx = bb["x1"]+15;
      var starty = bb["y2"]+30;
      removedMain.nodes().forEach(function(node){
        node.position({x:startx, y:starty});
        startx += 60;
        if( startx >= bb["x2"] ){
          startx = bb["x1"]+15;
          starty += 60;
        }
      });

      /* position the remainder nodes in the side panel */
      startx = bb["x1"]+15;
      starty = bb["y2"]+30;
      othersSide.nodes().forEach(function(node){
        node.position({x:startx, y:starty});
        startx += 60;
        if( startx >= bb["x2"] ){
          startx = bb["x1"]+15;
          starty += 60;
        }
      });

      /* restore the removed elements to the main panel */
      removedMain.restore();
      /* fit the viewport of both panels to show all nodes in their updated
       * positions */
      vizNet[MAIN_ID].fit();
      vizNet[SIDE_ID].fit();
    });
    vizNet[MAIN_ID].layout.run();
  }
  else{
  // Update the layout
    toxyNet[id].layout = vizNet[id].makeLayout(vizNet[id].updateLayout(opt));
    console.log(toxyNet[id].layout.options);
    setTimeout(function(){
      toxyNet[id].layout.run();
    });
  }
});

/**
 * Hide/show from the visualiation all nodes that are unconnected, that is,
 * nodes that are not linked to any other node within the network.
 */
$(document).on("change", "#hideNodesCheckbox", function(){
  // Determine the currently active panel
  var id = $("#panelSelect").val();
  // Return when there is no data to apply a layout to
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
    showNetworkOnRight();
  }
  // When the checkbox is not checked (disabled display of right-side panel)
  else{
    /* Disable interaction layout application */
    $("#panelSelect option[value=0]").prop("selected", true);
    $("#panelSelect option[value=1]").attr("disabled", true);
    $("#panelSelect option[value=2]").attr("disabled", true);

    // Unselect and disable the possibility of showing the intersection of
    // both networks
    $("#showIntersectionCheckbox").prop("checked", false);
    $("#showIntersectionCheckbox").attr("disabled", true);
    $("#showIntersectionCheckbox").trigger("change");
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

/**
 * Highlight, on both panels, the intersection (equal nodes) of both networks
 * currently on display
 */
$(document).on("change", "#showIntersectionCheckbox", function(){
  // If any of the displays is empty, there is no need to do an intersection
  if( vizNet[MAIN_ID] === null || vizNet[SIDE_ID] === null ) return;

  // Check whether the display of the intersections needs to be enabled or
  // disabled
  if( $("#showIntersectionCheckbox").is(":checked") ){
    // create a headless (unshown) copy of the left-side network, this is done
    // to prevent the redraw of intersected nodes, as they are added to the pool
    // with information regarding its parent container
    var clone = cytoscape({headless: true});
    clone.add(vizNet[MAIN_ID].nodes());
    // calculate the intersection of both networks
    var inter = clone.nodes().intersection(vizNet[SIDE_ID].nodes());
    // select each node found to be part of the intersection
    inter.forEach(function(node){

      vizNet[0].$('#'+node.id())
        .data("color", '#ffde4c')
        .style('background-color', '#ffde4c');
      vizNet[1].$('#'+node.id())
          .data("color", '#ffde4c')
          .style('background-color', '#ffde4c');
    });
  }
  // Unselect everything when canceling the display
  else{
    vizNet[0].nodes().forEach(function(n){
      var c = n.data('type')==="mRNA"? nodeColor.MSG_RNA : nodeColor.MICRO_RNA;
      n.style("background-color", c);
      n.data("color", c);
    });
    vizNet[1].nodes().forEach(function(n){
      var c = n.data('type')==="mRNA"? nodeColor.MSG_RNA : nodeColor.MICRO_RNA;
      n.style("background-color", c);
      n.data("color", c);
    });
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

/** ---------------------------- SEARCH NODE  ---------------------------- **/
/**
 * Handle the search of a particular node within the network.
 */
$(document).on("click", "#searchNodeModal #searchNodes", function(evt){
  // nothing to do if there is no network
  var id = $("#panelSelect").val();
  if( vizNet[id] === null ) return;

  // retrieve the search string
  var label = $("#searchNodeModal #nodeLabel").val();
  // select the corresponding nodes within the graph
  var selection = vizNet[id].nodes('[label*="'+label+'"]');
  selection.select();

  // once all nodes with matching labels have been selected, hide the modal
  var modal = $(event.target).data().modal;
  $("#"+modal).hide();
});

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

      vizNet[MAIN_ID].on("select", "node", onNodeSelection);
      vizNet[MAIN_ID].on("unselect", "node", onNodeUnselection);

      vizNet[MAIN_ID].on("mouseover", "node", onNodeEnter);
      vizNet[MAIN_ID].on("mouseout", "node", onNodeExit);

      // vizNet[MAIN_ID].on("select", "node", onNodeSelection(MAIN_ID));
      changeNetwork(MAIN_ID);

      /* Move the Cytoscape context menu into the modal GWT network visualiaztion
      * dialog, because otherwise input to it will be intercepted */
      $(".cy-context-menus-cxt-menu").appendTo($(".gwt-DialogBox"));
      $(".cy-context-menus-cxt-menu").hide();
    });
}

/**
 * Handle selection of nodes on the complementary display, in order to provide
 * a paired visualization.
 * @param {any} event the selection event triggered on the original display.
 * Notice that in the event of multiple selection, an event is triggered for
 * each newly selected element.
 */
function onNodeSelection(event){
  // The id of the DOM element where the selection was triggered
  var dpl = event.cy.container().id;
  // Definition of the complementary display panel
  var otherID = (dpl === "leftDisplay") ? 1 : 0;
  // If the complementary display is empty, we don't need to do anything
  if( vizNet[otherID] !== null ){
    // Target node - the node that was selected
    var n = event.target;
    // Select the corresponding node on the complementary display (element with
    // the same id). If no such node exists, the nothing will happen
    vizNet[otherID].nodes('[id="'+n.id()+'"]').select();
  }
}

/**
 * Handle the de-selection of nodes on the complementary display, in order to
 * provide a paired visualization.
 * @param {any} event the un-selection event triggered on the original display.
 */
function onNodeUnselection(event){
  // The id of the DOM element where the unselection was triggered
  var dpl = event.cy.container().id;
  // Definition of the complementary display panel
  var otherID = (dpl === "leftDisplay") ? 1 : 0;
  // If the complementary display is empty, we don't need to do anything
  if( vizNet[otherID] !== null ){
    // Target node - the node that was unselected
    var n = event.target;
    // Un-select the corresponding node on the complementary display (element
    // with the same id)
    vizNet[otherID].nodes('[id="'+n.id()+'"]').unselect();
  }
}

/**
 * Handle the definition of a pop-up div element, to be shown whenever the user
 * hovers over a node on the network.
 *
 * @param{any} event The mouseover event triggered when the user hovers over a
 * node on the display.
 */
function onNodeEnter(event){
  console.log("event", event);
  // retrieve the node element that triggered the event
  let node = event.cy.$(event.target);
  console.log("node", node);
  let popup = node.popper({
    content: ()=>{
      let div = document.createElement('div');
      div.classList.add('popper');

      let t = document.createElement('table');
      let idRow = t.insertRow();
      let cell = idRow.insertCell(0);
      cell.appendChild(document.createTextNode('Probe'))
      cell = idRow.insertCell(1);
      cell.appendChild(document.createTextNode(node.data("id")));

      idRow = t.insertRow();
      cell = idRow.insertCell(0);
      cell.appendChild(document.createTextNode('Type'))
      cell = idRow.insertCell(1);
      cell.appendChild(document.createTextNode(node.data("type")));

      idRow = t.insertRow();
      cell = idRow.insertCell(0);
      cell.appendChild(document.createTextNode('Symbol'))
      cell = idRow.insertCell(1);
      // cell.appendChild(document.createTextNode(node.data("symbol")[0]));

      div.appendChild(t);
      document.body.appendChild(div);
      return div;
    },
    popper: {},
  });

  node.on('position', function(){popup.scheduleUpdate();});
}

function onNodeExit(event){
  let node = event.target;
  node.removeListener('position');
  $(".popper").remove(); }

/**
 * Enable a dual panel visualization, by adding an extra DOM component. The
 * extra component is only added once, so we need to double check that the panel
 * is not already there, before creating it.
 */
function showNetworkOnRight() {
  // Enable the display of intersection of both networks
  $("#showIntersectionCheckbox").attr("disabled", false);
  /* Enable layout application for side and both networks */
  $("#panelSelect option[value=1]").attr("disabled", false);
  $("#panelSelect option[value=2]").attr("disabled", false);


  // Check if there is already a right panel
  var right = $("#rightDisplay");
  $("#showRightCheckbox").prop('checked', true);

  // If already a display has been displayed, we have to make sure the interface
  // is up-to date with this, and only change the contents of the internal
  // variables, without adding any new DOM elements
  if( right.length !== 0 ){

    vizNet[SIDE_ID] = cytoscape({
      container: right,
      styleEnabled: true,
    });
    vizNet[SIDE_ID].initStyle();        // default style for network elements
    // vizNet[SIDE_ID].initContextMenu();  // default context menu
    // Here I add elements to the network display... based on the network
    // currently stored in convertedNetwork
    vizNet[SIDE_ID].on("mouseover", "node", onNodeEnter);
    vizNet[SIDE_ID].on("mouseout", "node", onNodeExit);

    changeNetwork(SIDE_ID);

    vizNet[MAIN_ID].resize();
    vizNet[MAIN_ID].fit();

    return;
  }

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
      // vizNet[SIDE_ID].initContextMenu();  // default context menu
      // Here I add elements to the network display... based on the network
      // currently stored in convertedNetwork
      vizNet[SIDE_ID].on("mouseover", "node", onNodeEnter);
      vizNet[SIDE_ID].on("mouseout", "node", onNodeExit);


      changeNetwork(SIDE_ID);

      vizNet[MAIN_ID].resize();
      vizNet[MAIN_ID].fit();
    });
}

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
