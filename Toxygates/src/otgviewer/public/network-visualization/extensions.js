/**
 * Initialize style for network components,
 */
function initStyle(){
  // define the style to use for the depiction of the network elements
  this.style()
    .selector("node")
    .style({
      "label": "data(label)",
      "text-valign": "center",
      "text-halign": "center",
      'background-color': "data(color)",
    })
    .update();
}

/**
 * Initialize the components of the context menu that will be presented to the
 * user. These options vary depending on whether there is an active selection
 * of nodes and/or edges or not (coreAsWell option).
 */
function initContextMenu(){
  // self is the viz container that is currently initializing a context menu
  var self = this; // save context to use within annonymous functions
  var contextMenu = this.contextMenus({
    menuItems: [
      //---------------------------------------------------------
      // Options shown when clicking on a selected node
      //---------------------------------------------------------
      {
        /**
         * add an interaction between the currently selected node, and a next
         * selected element
         */
        id: "add-edge",
        content: "Add Edge",
        tooltipText: "Add an edge",
        selector: "node",
        onClickFunction: onAddEdge,
      },
      {
        /**
         * update data or visual properties of a given node
         */
         id: "update-node",
         content: "Properties",
         tooltip: "View and modify node's properties",
         selector: "node",
         onClickFunction: onUpdateNode,
      },

      //---------------------------------------------------------
      // Options shown when clicking on a selected node
      //---------------------------------------------------------
      /**
       * color the whole graph based on the selection made by the user.
       * Typically this will be related to the level of expresion on messegerRNA
       * nodes
       */
      {
        id: "color-scale",
        content: "Scale coloring",
        tooltipText: "Color nodes according to a give property",
        coreAsWell: true,
        onClickFunction: onColorScale,
      },
      /**
       * filter the display of nodes to those that fullfill a certain criteria.
       * The filtering is selected by the user.
       */
       {
        id: "node-filter",
        content: "Filter Nodes",
        tooltipText: "Nodes thata do not fill the criteria will not be displayed",
        coreAsWell: true,
        onClickFunction: onNodeFiltering,
       },
    ] // menuItems
  });
}

/**
 * Adds a new interaction to the graph, if the next clicked item corresponds to
 * a different node from the originally selected
 * @param {}event the event triggered when the corresponding item in the context
 * menu is clicked
 */
function onAddEdge(event){
  // the initial node for the new interaction
  var source = event.target || event.cyTarget;
  // change cursor type to cue the user on the need to select a target node
  document.body.style.cursor = "crosshair";
  // handle the next click of the mouse
  event.cy.promiseOn("click").then(function(evt){
    var to = evt.target;
    // a new interaction is only added if the user clicks on a node different
    // from the one used as source for the interaction
    if( to !== self && to.isNode() ){
      event.cy.add({
        group: "edges",
        data: {
          id: "#"+source.id()+to.id(),
          source: source.id(),
          target: to.id()
        }
      });
    }
    else{
      window.alert("Edge not added");
    }
    // return cursor to its default value, regardless of the previous result
    document.body.style.cursor = "default";
  });
}

/**
 * Configure and display the modal used to update the properties of a single
 * node within the graph
 * @param {}event the event triggered when the corresponding item in the context
 * menu is pressed
 */
function onUpdateNode(event){
  $("#updateNodeModal").show();
  /* container for the current node's data */
  var trg = event.target.data();
  // the current node's ID
  $("#nodeID").val(trg["id"]);

  // the node's label - text shown on screen
  $("#nodeLabel").val(trg["label"]);

  // the node's type - provide options for user to change, based on available
  // types
  var types = Object.keys(nodeType);
  $("#nodeType").empty();
  for(var i=0; i<types.length; ++i)
    $("#nodeType").append(new Option(types[i], nodeType[types[i]])); // (text, value)
  $("#nodeType").val(trg["type"]);

  // set the available options for weights and add them to a list for display
  $(".modal-content #weightValue").val("");
  var weights = Object.keys(trg["weight"]);
  if( weights !== null && weights !== undefined ){
    $("#nodeWeights").empty();
    $("#nodeWeights").append(new Option("Select...", null));
    for(var i=0; i<weights.length; ++i)
      $("#nodeWeights").append(new Option(weights[i], weights[i]));
  }
  // the node's current background color
  $(".modal-content #nodeColor").val(trg["color"]);

  // select the correct shape for the current node - available options listed
  $("#nodeShape").val(event.target.style("shape"));

}

/**
 * Define the initial set-up and options for selection of coloring application
 * to entire sections of the graph.
 * @param {}event the event triggered when the corresponding item in the context
 * menu is pressed
 */
function onColorScale(event){
  /* display the corresponding color interface */
  $("#graphColorModal").show();

  /* add option to select where to apply color */
  var types = Object.keys(nodeType);
  $("#graphColorTo").empty();
  $("#graphColorTo").append(new Option("Select...", null));
  for(var i=0; i<types.length; ++i)
    $("#graphColorTo").append(new Option(types[i], nodeType[types[i]]));

  /* initialize an empty color by select component */
  $("#graphColorBy").empty();
  $("#graphColorBy").append(new Option("Select...", null));

  /* initialize color scale values */
  $("#minRange").val("");
  $("#maxRange").val("");
  $("#colorRange").val(50);
  $("#whiteRange").val("");
}

/**
 *
 */
function onNodeFiltering(event){
  $("#filterModal").show();
}

/**
 * apply a specific layout to the visual representation of the graph
 * @param {string} type the type of layout to use for the placement of the nodes
 * within the display area
 */
function updateLayout(type){
  this.layout({
    name: type,
    fit: true, // whether to fit to viewport
    padding: 0, // fit padding
    boundingBox: undefined, // constrain layout bounds; { x1, y1, x2, y2 } or { x1, y1, w, h }
    animate: false, // whether to transition the node positions
    animationDuration: 500, // duration of animation in ms if enabled
    animationEasing: undefined, // easing of animation if enabled
    animateFilter: function ( node, i ){ return true; }, // a function that determines whether the node should be animated.  All nodes animated by default on animate enabled.  Non-animated nodes are positioned immediately when the layout starts
    ready: undefined, // callback on layoutready
    stop: undefined, // callback on layoutstop
    transform: function (node, position ){ return position; } // transform a given node position. Useful for changing flow direction in discrete layouts
  })
  .run();
}

// add functions to cytoscape prototype
cytoscape("core", "initStyle", initStyle);
cytoscape("core", "initContextMenu", initContextMenu);
cytoscape("core", "updateLayout", updateLayout);
