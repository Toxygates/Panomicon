/**
 * Initialize style for network components
 * Set the default style for each type of element within a cytoscape network,
 * i.e. nodes and edges. Also set up a special display to appy to currently
 * selected nodes.
 *
 * This function is declared as an extension to Cytoscape's core
 */
function initStyle(){
  this.resize();
  this.style()
    .selector("node")
    .style({
      "label": "data(label)",  // use the node's label
      "text-valign": "center",
      "text-halign": "center",
      'background-color': "data(color)", // use the node's color
      'shape': 'data(shape)',  // use the node's shape
      'border-width': '1px',
      'border-color': 'data(borderColor)',
    })
    .selector("edge")
    .style({
      "line-color": "data(color)",
    })
    .selector("node:selected")
    .style({
      "border-width": "5px",
    })
    .selector('node.highlighted')
    .style({
      'background-color': nodeColor.HIGHLIGHT,
      'border-color': nodeColor.HIGHLIGHT,
    })
    .selector('edge.highlighted')
    .style({
      'line-color': edgeColor.HIGHLIGHT,
    })
    .update();
}

/**
 * Initialize the visualization context menu
 * A context menu associated to each visualization panel is provided to help the
 * user interact and modify some aspects of the representation of the network.
 * The options provided depend on whether the user request the context menu
 * while on top of a graph element (node or edge) or not.
 *
 * @param {number} id Numeric identifier for the panel to which the context menu
 * is being added.
 */
function initContextMenu(id){
  /* use the default way to provide all the options to be added to the menu */
  this.contextMenus({
    /* class that helps identifying the corresponding memu items in the
     * applications DOM */
    contextMenuClasses:[
      'ctx-menu-'+id
    ],
    /* the actual contentes of the menu */
    menuItems: [
    { /* change the color of the graph according to a color scale */
      id: "color-scale-"+id,
      content: "Apply color scale",
      tooltip: "Apply color scale",
      selector: "node",
      coreAsWell: true,
      onClickFunction: showColorScaleDialog,
    }
    ] // menuItems
  });
}

/**
 * Apply two different layouts, defined by innerName and outerName respectively,
 * to two subsets of nodes within a collection. The inner layout is applied to
 * the nodes in eles, while the outer layout is applied to the remainder of the
 * nodes in the collection.
 * @param {graph} cy The cytoscape for which we are trying to find an
 * intersection. The intersection on both graphs will be given the same layout,
 * whilst other elements will be layed out separately for each graph.
 * @param {string} innerName The name of the layout used for the intersection
 * @param {string} outerName The name of the layout applied to the remainder
 * nodes in both graphs
 * EXTENSION TO CYTOSCAPE - CORE
 */
function dualLayout(cy, innerName, outerName="grid"){
  /* get the intersection between this network and cy */
  let intersection = this.elements().intersection(cy.elements());

  /* the remainder elements are the absolute complement to the intersection, we
  * remove them from the graph, in order to apply the inner layout only to the
  * intersecting nodes */
  let outer = this.remove(intersection.absoluteComplement());

  /* define the layouts for intersecting nodes on both graphs */
  let innerlay = this.updateLayout(innerName);
  /* once the positioning of the intersecting nodes in the current graph is
  * finished, we used them to also position the intersecting nodes in the cy
  * graph */
  innerlay.promiseOn('layoutstop').then(function(eles){
    /* we copy the position of each node in the current graph */
    eles.forEach(function(node){
      cy.$("#"+node.id())
      .position(node.position());
    });
  }.bind(null, this.nodes()));
  /* run the layout for intersecting nodes */
  innerlay.run();

  /* define the layout for nodes outside the intersection on the current graph */
  let outerlay = outer.updateLayout(outerName);
  /* once the layout is finished, shift the position of the nodes down by the
  * size of the bounding box of the intersecting nodes and fit all the elements
  * to the viewport */
  outerlay.promiseOn('layoutstop').then(function(graph){
    /* shift the nodes down */
    outer.shift('y', graph.elements().boundingBox().y2);
    /* restore the nodes into the graph for display */
    outer.restore();
    /* fit the whole graph to the viewport */
    graph.fit();
  }.bind(null, this));
  /* run the layout */
  outerlay.run();

  /* Define a layout for nodes of cy outside the intersection */
  let dif = cy.elements().difference(intersection);
  let diflay = dif.updateLayout(outerName);
  /* shift and fit the graph after the layout has finished its positioning */
  diflay.promiseOn('layoutstop').then(function(eles){
    dif.shift('y', eles.boundingBox().y2);//cy.height());
    cy.fit();

  }.bind(null, this.elements()));
  /* run the layout */
  diflay.run();
}

/**
 * Add the class 'hidden' to all unconnected nodes in the network.
 * EXTENSION TO CYTOSCAPE - CORE
 */
function hideUnconnected(){
  let unconnected = this.nodes().filter(function(ele){
    return ele.degree(false) === 0;
  });
  unconnected.forEach(function(ele){
    ele.addClass("hidden");
  });
}



/**
 * Merge the current collection with the collection of elements provided,
 * keeping a single copy of intersecting elements, and defining a dual layout
 * that positions the intersection at the center of the viewport, with elements
 * coming from the left panel at the left of the intersection, and elements
 * coming from the right panel at the right of the intesection.
 * @param {collection} eles The elements we are to merge with the current graph.
 * @param {string} innerName The name of the layout used for the intersection.
 * Use concentric layout as default.
 * @param {string} outerName The name of the layout used for both sets of
 * elements not in the intersection. Use grid layout as default.
 * EXTENSION TO CYTOSCAPE - CORE
 */
function mergeWith(eles, innerName="concentric", outerName="grid"){
  /* Define the collections that represent the three components of the new graph,
   * intersecting nodes, left-side nodes and right-side nodes */
  let inter = this.elements().intersection(eles);
  let left = inter.absoluteComplement();
  let right = eles.difference(inter);

  /* Layout each component of the new graph */
  innerName = innerName === "null" ? "concentric" : innerName;
  outerName = outerName === "null" ? "grid" : outerName;

  let w = this.container().parentElement.clientWidth/3;
  let h = this.container().parentElement.clientHeight;

  let interLyt = inter.updateLayout(innerName, {x1:w,y1:0,w:w,h:h} );
  let leftLyt = left.updateLayout(outerName, {x1:0,y1:0,w:w,h:h} );
  let rightLyt = right.updateLayout(outerName, {x1:2*w,y1:0,w:w,h:h} );

  leftLyt.promiseOn('layoutstop').then(function(){
    interLyt.run();
  });
  interLyt.promiseOn('layoutstop').then(function(){
    rightLyt.run();
  });
  rightLyt.promiseOn('layoutstop').then(function(cy){
    /* Define the merged collection of elements */
    let mrg = cytoscape();
    mrg.add(inter);
    mrg.add(left);
    mrg.add(right);

    /* Update the contentes of the current graph with the ones computed */
    cy.elements().remove();
    cy.add(mrg.elements());
    cy.fit();
  }.bind(null, this));

  leftLyt.run();
}

/**
 * Returns elements in the collection to their default style, in terms of color
 * and shape of its components
 * @param {collection} eles The elements we want to return to their original
 * style
 */
function setDefaultStyle(eles){
  this.startBatch();
  eles.forEach(function(ele){
    if ( ele.isEdge() ){
      this.$("#"+ele.id())
        .data("color", edgeColor.REGULAR);
        return;
    }
    let color = ele.data('type') === "mRNA"? nodeColor.MSG_RNA : nodeColor.MICRO_RNA;
    let shp = ele.data('type') === "mRNA"? nodeShape.MSG_RNA : nodeShape.MICRO_RNA;

    this.$("#"+ele.id())
      .data("color", color)
      .data('borderColor', color)
      .data("shape", shp);
  },this);
  this.endBatch();
}


/**
 * Hide/Show nodes that have the class "hidden" added to their names. By default
 * all unconnected nodes are flagged as hidden on load of a network.
 * @param {boolean} showHidden Whether the nodes should be shown or not as part
 * of the visualization
 * @param {collection} eles The list of currently hidden elements (nodes removed
 * from the network to prevent its display and consideration in layout
 * operations)
 * @return The updated collection of hidden nodes. Note that, even when nodes
 * might have the class "hidden" among their properties, this does not mean they
 * are hidden from the visualization.
 * When "hidden" nodes are shown, en empty collection is returned.
 * EXTENSION TO CYTOSCAPE - CORE
 */
  function showHiddenNodes(showHidden=false, eles=null){
    let hidden = this.elements(".hidden");
    if( showHidden ){
      if( eles !== null ){
        this.add(eles);//.restore();
      }
      return this.collection();
    }
    else {
      return hidden.remove();
    }
  }

/**
 * Toggle the 'highlighted' class on selected nodes.
 * A different display color is specified for nodes that have been highlighted
 * by the user. This change in appearence is handled through the toogle class
 * 'highlighted', defined in the default stylesheet used for graphs (see method
 * initStyle).
 * Here, we toogle on or off (depending on the provided parameter) the highlight
 * class for the nodes found in the intersection between the current graph and
 * the given other graph.
 *
 * @param {graph} other The graph to which the elements of the current graph are
 * to be compared with.
 * @param {boolean} toogle Indicates if the 'highlighted' class for the
 * intersecting nodes should be turned on (true) or off (false)
 *
 * EXTENSION TO CYTOSCAPE - CORE
 */
function toogleIntersectionHighlight(other, toggle){
  /* create a headless copy of the current network */
  let clone = cytoscape({headless:true});
  clone.add(this.elements());

  /* determine the intersecting elements between both collections */
  let intersection = clone.elements().intersection(other.elements());
  /* toggle the 'highlighted' class for the elements on both graphs */
  intersection.forEach(function(ele){
      this.$("#"+ele.id()).toggleClass('highlighted', toggle);
      other.$("#"+ele.id()).toggleClass('highlighted', toggle);
  },this);
}

/**
 * Generate the options Object needed to define the layout for a cytoscape
 * network.
 * @param {string} name The identifier used by cytoscape to define a layout type
 * @return The layout object generated by Cytoscape. The object needs to be run
 * for the layout to be actually applied to the network.
 */
function updateLayout(name="null", boundingBox=undefined){
  window.addPendingRequest();
  return this.layout({
    name: name,
    fit: true, // whether to fit to viewport
    padding: 0, // fit padding
    boundingBox: boundingBox, // constrain layout bounds; { x1, y1, x2, y2 } or { x1, y1, w, h }
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
cytoscape("core", "hideUnconnected", hideUnconnected);
cytoscape("core", "mergeWith", mergeWith);
cytoscape("core", "setDefaultStyle", setDefaultStyle);
cytoscape("core", "toogleIntersectionHighlight", toogleIntersectionHighlight);

cytoscape("core", "initStyle", initStyle);
cytoscape("core", "updateLayout", updateLayout);
cytoscape("collection", "updateLayout", updateLayout);

// cytoscape("core", "mergeTo", mergeTo);
cytoscape("core", "dualLayout", dualLayout);
cytoscape("core", "initContextMenu", initContextMenu);
cytoscape("core", "showHiddenNodes", showHiddenNodes);
cytoscape("core", "getToxyNodes", getToxyNodes);
cytoscape("core", "getToxyInteractions", getToxyInteractions);
