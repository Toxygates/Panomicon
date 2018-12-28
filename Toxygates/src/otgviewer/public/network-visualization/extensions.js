/**
 * Initialize style for network components,
 */
function initStyle(){
  this.resize();
  // define the style to use for the depiction of the network elements
  this.style()
    .selector("node")
    .style({
      "label": "data(label)",
      "text-valign": "center",
      "text-halign": "center",
      'background-color': "data(color)",
    })
    .selector("node:selected")
    .style({
      "border-width": "5px",
    })
    .update();
}

/**
 * Apply two different layouts, defined by innerName and outerName respectively,
 * to two subsets of nodes within a collection. The inner layout is applied to
 * the nodes in eles, while the outer layout is applied to the remainder of the
 * nodes in the collection.
 * @param {collection} eles The elements to which the layout defined by
 * innerName will be applied. The intersection of two networks.
 * @param {string} innerName The name of the inner layout
 * @param {function} callback A callback function to be executed once the
 * positioning of the nodes has been completed
 * @param {string} outerName The name of the outer layout, the layout applied to
 * the remainder nodes in the collection
 */
function compositeLayout(eles, innerName, callback, outerName="grid"){
  // select the nodes to which the outer layout should be applied
  let outer = eles.absoluteComplement();
  // remove outer elements from current collection
  let removed = this.remove(outer);
  // define the layout for the inner nodes
  let innerLayout = this.layout({name: innerName});
  // define the positioning of outer nodes should be done once the layout for
  // the inner nodes is completed
  innerLayout.promiseOn('layoutstop').then(function(evt){
    // restore removed (outer) nodes to the collection
    removed.restore();
    // define a layout for the outer nodes
    let outerLayout = removed.layout({name: outerName});
    // apply the layout
    outerLayout.run();
    // move the outer nodes below the inner nodes in the display
    removed.shift('y', evt.cy.height());
    // fit the viewport so that all nodes are displayed
    evt.cy.fit();

    if( callback ){
      callback();
    }
  });
  // apply layout to inner nodes
  innerLayout.run()
}



/**
 * Generate the options Object needed to define the layout for a cytoscape
 * network.
 * @param {string} name The identifier used by cytoscape to define a default
 * layout type.
 */
function updateLayout(type="null", bb=undefined){
  window.addPendingRequest();
  return this.layout({
    name: type,
    fit: true, // whether to fit to viewport
    padding: 0, // fit padding
    boundingBox: bb, // constrain layout bounds; { x1, y1, x2, y2 } or { x1, y1, w, h }
    animate: false, // whether to transition the node positions
    animationDuration: 500, // duration of animation in ms if enabled
    animationEasing: undefined, // easing of animation if enabled
    animateFilter: function ( node, i ){ return true; }, // a function that determines whether the node should be animated.  All nodes animated by default on animate enabled.  Non-animated nodes are positioned immediately when the layout starts
    ready: undefined, // callback on layoutready
    stop: function() { window.removePendingRequest(); }, // callback on layoutstop
    transform: function (node, position ){ return position; }, // transform a given node position. Useful for changing flow direction in discrete layouts
    weaver: weaver
  });
}

/**
 * Hide nodes in the network that have no edges connecting them to other
 * node
 * @return a collection of nodes that are unconnected to the remainder of the
 * network
 */
function hideUnconnected(){
  // first we find all nodes whose degree (number of edges) is zero
  var select = this.nodes().filter(function(ele){
    return ele.degree(false) === 0;
  });
  // we remove the unconnected nodes from the network, and return the collection
  return select.remove();
}

/**
 * Restore unconnected nodes to the original network, so as to include them in
 * the display and all network related calculations (such as layout)
 * @param {}eles the collection of elements to restore to the network
 * (previously removed unconnected nodes)
 * @return true if an insertion was performed, regardless of the redundance
 * produced when elements are already part of the graph, false in any other case
 */
function showUnconnected(eles){
  if( eles !== null ){
    // we need to show everything
    eles.restore();
    // this.nodes().style('display', 'element');
    return true;
  }
  return false;
}

/**
 * Return the list of nodes in the current network, using the ToxyNode data
 * structure.
 * Required functionality for the persistance of the graphic network within the
 * Toxygates system.
 * @return The list of nodes that comprise the network.
 */
function getToxyNodes(){
  // create an empty list of nodes
  var toxyNodes = [];

  this.nodes().forEach(function(node){
    var data = node.data();
    // for each node, we create a new instance of ToxyNodes and initialize it
    // with the corresponding values
    var tn = new ToxyNode(data["id"], data["type"], [data["label"]]);
    tn.setWeights(data["weight"]);

    var position = node.position();
    tn.x = position["x"];
    tn.y = position["y"];

    tn.color = data["color"];
    tn.shape = node.style()["shape"];
    // once its ready, we add the node to our return list
    toxyNodes.push(tn);
  });

  return toxyNodes;
}

/**
 * Return the list of interactions, using the Interactions data structure from
 * Toxygates.
 * Required functionality for the persistance of the graphic network within the
 * Toxygates system.
 * @return The list of interactions that comprise the network.
 */
function getToxyInteractions(){
  // create an empty list of interactions
  var toxyInter = [];

  this.edges().forEach(function(edge){
    var data = edge.data();
    var te = new Interaction(data["source"], data["target"]);
    toxyInter.push(te);
  });

  return toxyInter;
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
       * Search a node.
       * The user is allowed to enter free text and search for a node within
       * the network with the given label or ID.
       */
      {
        id: "search-node",
        content: "Search Node",
        tooltipText: "Search a node by label",
        coreAsWell: true,
        onClickFunction: onSearchNode,
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
    console.log(to);
    // a new interaction is only added if the user clicks on a node different
    // from the one used as source for the interaction
    if( to !== self && typeof to.isNode === 'function' && to.isNode() ){
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
  // set the ID of the current node
  $("#updateNodeModal #nodeID").val(trg["id"]);
  // set the node's label (the text shown on the visualization)
  $("#updateNodeModal #nodeLabel").val(trg["label"]);
  // set the node's type and provide options for user to change it to any of the
  // currently available types
  var types = Object.keys(nodeType);
  $("#updateNodeModal #nodeType").empty();
  for(var i=0; i<types.length; ++i){
    // (text, value)
    $("#updateNodeModal #nodeType").append(new Option(types[i], nodeType[types[i]]));
  }
  $("#updateNodeModal #nodeType").val(trg["type"]);

  // set the available options for weights and add them to a list for display
  $("#updateNodeModal #weightValue").val("");
  var weights = Object.keys(trg["weight"]);
  if( weights !== null && weights !== undefined ){
    $("#updateNodeModal #nodeWeights").empty();
    $("#updateNodeModal #nodeWeights").append(new Option("Select...", null));
    for(var i=0; i<weights.length; ++i)
      $("#updateNodeModal #nodeWeights").append(new Option(weights[i], weights[i]));
  }

  // the node's current background color
  $("#updateNodeModal #nodeColor").val(trg["color"]);
  // select the correct shape for the current node - available options listed
  $("#updateNodeModal #nodeShape").val(event.target.style("shape"));
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

  /* add options to select the type of node on which to apply color */
  var types = Object.keys(nodeType);
  $("#graphColorModal #graphColorTo").empty();
  $("#graphColorModal #graphColorTo").append(new Option("Select...", null));
  for(var i=0; i<types.length; ++i){
    $("#graphColorModal #graphColorTo").append(new Option(types[i], nodeType[types[i]]));
  }

  /* initialize an empty color by select component */
  $("#graphColorModal #graphColorBy").empty();
  $("#graphColorModal #graphColorBy").append(new Option("Select...", null));

  /* initialize color scale values */
  $("#graphColorModal #minRange").val("");
  $("#graphColorModal #maxRange").val("");
  $("#graphColorModal #colorRange").val(50);
  $("#graphColorModal #whiteRange").val("");
}

/**
 * Define the initial set-up and options to be displayed when searching for a
 * particular node within the network.
 * @param {any} event the event triggered when the corresponding item in the
 * context menu is pressed.
 */
function onSearchNode(event){
  $("#searchNodeModal").show();

  /* initialize search text */
  $("#searchNodeModal #nodeLabel").val("");
}

// add functions to cytoscape prototype
cytoscape("core", "initStyle", initStyle);
// cytoscape("core", "mergeTo", mergeTo);
cytoscape("core", "compositeLayout", compositeLayout);
cytoscape("core", "initContextMenu", initContextMenu);
cytoscape("core", "updateLayout", updateLayout);
cytoscape("core", "hideUnconnected", hideUnconnected);
cytoscape("core", "showUnconnected", showUnconnected);
cytoscape("core", "getToxyNodes", getToxyNodes);
cytoscape("core", "getToxyInteractions", getToxyInteractions);
