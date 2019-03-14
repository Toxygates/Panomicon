/* Functions that are applied to, or are defined to handle aspects of the
 * network instances, in cytoscape's format */

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
    /* default style for node elements */
    .selector('node')
    .style({
      'label': 'data(label)',
      'text-valign': 'center',
      'text-halign': 'center',
      'shape': 'data(shape)',
      'background-color': 'data(color)',
      'border-width': '1px',
      'border-color': 'data(borderColor)',
    })
    .selector('node:selected')
    .style({
      'border-width': '5px',
      'background-color': nodeColor.HIGHLIGHT,
      'border-color': nodeColor.HIGHLIGHT,
    })
    .selector('node.highlighted')
    .style({
      'background-color': nodeColor.HIGHLIGHT,
      'border-color': nodeColor.HIGHLIGHT,
    })
    /* default style for edges */
    .selector("edge")
    .style({
      "line-color": "data(color)",
    })
    .selector('edge.highlighted')
    .style({
      'line-color': edgeColor.HIGHLIGHT,
    })
    .update();

}

/**
 * Display a node's Pop-up
 * Handle the display of a pop-up div element, to be shown whenever the user
 * hovers over a node on the network.
 *
 * @type{Event}
 * @param{Event} event The mouseover event triggered when the user hovers over a
 * node on the display.
 */
function onNodeEnter(event){
  /* retrieve the node that has been entered by the user */
  let node = event.target;

  /* update the display content for the pop-up DOM, based on the node's data */
  $('#nodePopper').css('display', 'block');
  let div = document.createElement('div');
  $('#nodePopper #label').html(node.data("label"));
  $('#nodePopper #type').html(node.data("type"));
  $('#nodePopper #probe').html(node.data("id"));

  /* create a new pop-up element and bind it to the corresponding node */
  let popup = node.popper({
    content: $('#nodePopper')[0],
    popper: { },
  });
  /* bind a listener to node's position, to keep pop-up in sync */
  node.on('position', function(){popup.scheduleUpdate();});
  /* bind a listener to viewport (zoom and pan), to keep pop-up position in sync */
  event.cy.on('viewport', function(){popup.scheduleUpdate();});
}

/**
 * Remove the display of a node's pop-up
 * Handle the removal of the pop-up for a node
 *
 * @type{Event}
 * @param{Event} event The mouse out event triggered when the user moves the
 * pointer outside of a given node
 */
function onNodeExit(event){
  let node = event.target;

  node.removeListener('position');
  event.cy.removeListener('viewport');

  $('#nodePopper').css('display', 'none');
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
    },
    { /* return the color of a single node or the graph to its default */
      id: 'default-color'+id,
      content: "Apply default color",
      tooltip: "Use the default color for a node or the whole graph",
      selector: 'node',
      coreAsWell: true,
      onClickFunction: function(evt){
        if( evt.cy === evt.target ) // click on the background
          evt.cy.elements().setDefaultStyle();
        else
          evt.target.setDefaultStyle();
      },
    },
    { /* change the label of a single node */
      id: 'change-label'+id,
      content: "Change label",
      tooltip: "Change the label of a single node",
      selector: 'node',
      coreAsWell: false,
      onClickFunction: showChangeLabelDialog,
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
 */
function setDefaultStyle(){//eles){
  // this.startBatch();
  // eles.forEach(function(ele){
  this.forEach(function(ele){
    if ( ele.isEdge() ){
      // this.$("#"+ele.id())
      ele.data("color", edgeColor.REGULAR);
      return;
    }
    let color = ele.data('type') === "mRNA"? nodeColor.MSG_RNA : nodeColor.MICRO_RNA;
    let shp = ele.data('type') === "mRNA"? nodeShape.MSG_RNA : nodeShape.MICRO_RNA;

    // this.$("#"+ele.id())
    ele.data("color", color)
    ele.data('borderColor', color)
    ele.data("shape", shp);
  });//,this);
  // this.endBatch();
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

// add functions to cytoscape prototype
cytoscape("core", "hideUnconnected", hideUnconnected);
cytoscape("core", "mergeWith", mergeWith);
// cytoscape("core", "setDefaultStyle", setDefaultStyle);
cytoscape('collection', 'setDefaultStyle', setDefaultStyle);
cytoscape("core", "toogleIntersectionHighlight", toogleIntersectionHighlight);

cytoscape("core", "initStyle", initStyle);
cytoscape("core", "updateLayout", updateLayout);
cytoscape("collection", "updateLayout", updateLayout);

cytoscape("core", "dualLayout", dualLayout);
cytoscape("core", "initContextMenu", initContextMenu);
cytoscape("core", "showHiddenNodes", showHiddenNodes);
cytoscape("core", "getToxyNodes", getToxyNodes);
cytoscape("core", "getToxyInteractions", getToxyInteractions);
