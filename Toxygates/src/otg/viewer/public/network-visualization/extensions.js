/**
 * Initialize style for network components
 * Set the default style for each type of element within a cytoscape network,
 * i.e. nodes and edges. Also set up a special display to appy to currently
 * selected nodes.
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
      'border-color': 'data(borderColor)',
      'border-width': '1px',
      'display': 'element',
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
    .selector('node.hidden')
    .style({
      'display': 'none',
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
 * Display a node's Pop-up on hover
 * Handle the display of a pop-up div element, to be shown whenever the user
 * hovers over a node on the network.
 *
 * @type {Event}
 * @param {Event} event The mouseover event triggered when the user hovers over
 * a node on the display.
 */
function onNodeEnter(event){
  /* retrieve the node that has been entered by the user */
  let node = event.target;

  /* update the display content for the pop-up DOM, based on the node's data */
  $('#nodePopper').css('display', 'block');
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
 * Hide a node's pop-up on leave
 * Whenever the user leaves a node or the visualization area, whatever pop-up
 * that was active needs to be hidden from the display
 *
 * @type {Event}
 * @param {Event} event The mouseout event triggered when the user moves the
 * pointer outside of a given node or outside the visualization area
 */
function onNodeExit(event){
  /* retrieve the element that triggered the event */
  let node = event.target;
  /* remove the listeners, both on node position and on viewport interaction */
  node.removeListener('position');
  event.cy.removeListener('viewport');
  /* hide the pop-up */
  $('#nodePopper').css('display', 'none');
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
 * Load elements into the graph
 * Internal toxygates data is provided through a JSON style dictionary, including
 * interactions and nodes that need to be loaded to the cytoscape instance in
 * order to be displayed
 *
 * @type {JSONNetwork}
 * @param {JSONNetwork} network The network received from the table display in
 * toxygates, as stored in the variable 'convertedNetwork'
 *
 * @return true if the nodes in the given network had a predefined position, or
 * false otherwise
 */
function loadElements(network){
  /* nodes and interactions are added as Elements to cytoscape */
  let eles = [];
  /* handle the nodes from the provided network */
  network.nodes.forEach(function(e){
    /* define all the node's properties */
    let id = e.id;
    let type = nodeType[e.type];
    let weight = e.weight;
    let label = (e.symbol.length > 0) ? e.symbol[0] : id;
    let color = e.color !== undefined ? e.color : nodeColor[nodeType[e.type]];
    let shape = e.shape !== undefined ? e.shape : nodeShape[nodeType[e.type]] ;
    /* use the properties to create a new cytoscape node element */
    let node = {
      group: 'nodes',
      classes: type,
      data:{
        id: id,
        label: label,
        color: color,
        borderColor: color,
        type: type,
        shape: shape,
        weight: weight,
        display: 'element',
      },
      position: {
        x: e.x,
        y: e.y,
      },
    };
    /* add the node to the list of elements */
    eles.push( node );
  });

  /* handle the list of interactions from the provided network */
  network.interactions.forEach(function(e){
    eles.push({
      group: 'edges',
      data:{
        id: e.from+e.to,
        source: e.from,
        target: e.to,
        color: edgeColor.REGULAR,
      }
    });
  });
  /* add the whole list of elements (nodes and interactions) to the current
   * cytoscape graph */
  this.add(eles);

  /* return true if there was a position associated to the first node */
  let positioned = network.nodes[0].x !== undefined;
  if( positioned )
    this.options().layout.name = 'custom';
  return positioned;
}

/**
 * Apply a composite layout to both panels simultaneously.
 * Two different layout options are applied to both networks, the first layout,
 * identified by inLyt is applied only to elements that are part of the
 * intersection of both networks. The second layout, identified by outLyt, is
 * applied to all remainder elements on both networks.
 * @param {graph} cy The second cytoscape element, where we will look for
 * intersecting elements
 * @param {string} inLyt Identifier for the layout used on the intersection
 * @param {string} outLyt Identifier for the layout used on remainder elements
 */
function dualLayout(cy, inLyt, outLyt="grid"){
  /* get the intersection between this network and cy, resulting elements are
   * part of cy (the second collection) */
  let intersection = this.elements(':visible').intersection(cy.elements(':visible'));
  /* define the layout for the intersecting elements */
  let innerlay = intersection.updateLayout(inLyt);
  /* since running the layout will only change the position of the nodes in the
   * second graph, we need to assign the same position to the nodes in the first
   * graph */
  innerlay.promiseOn('layoutstop').then(function(){
    intersection.forEach(function(ele,i,eles){
      if (ele.cy().container().id === 'leftDisplay'){
        cy.$('#'+ele.id())[0]
        .position(ele.position())
        ;
      }
      else{
        this.$('#'+ele.id())[0]
          .position(ele.position())
        ;
      }
    },this);
  }.bind(this));
  /* run the layout for intersecting nodes */
  innerlay.run();

  /* we select the elements within the current graph that are not part of the
   * intersection, so we can apply a second layout to them */
  let thisDif = this.elements(':visible').difference(intersection);
  /* define the layout for these elements */
  let thisDifLay = thisDif.updateLayout(outLyt);
  /* once the layout is finished, shift the position of the nodes down by the
   * size of the bounding box of the intersecting nodes and fit all the elements
   * to the viewport */
  thisDifLay.promiseOn('layoutstop').then(function(){
    thisDif.shift('y', intersection.boundingBox().y2);
    this.fit();
  }.bind(this));
  /* run the layout */
  thisDifLay.run();

  /* We do the same for the nodes of the other graph that are not part of the
   * intersection */
  let cyDif = cy.elements(':visible').difference(intersection);
  let cyDifLay = cyDif.updateLayout(outLyt);
  /* shift and fit the graph after the layout has finished its positioning */
  cyDifLay.promiseOn('layoutstop').then(function(){
    cyDif.shift('y', intersection.boundingBox().y2);
    cy.fit();

  });
  /* run the layout */
  cyDifLay.run();
}

/**
 * Set default hidden nodes.
 * In any given network it is possible to have nodes that do not interact with
 * any other element. This unconnected nodes are, by default, hidden from the
 * display.
 * On network load, this method adds the class 'hidden' to all unconnected nodes,
 * used to determing whether they should be displayed or not.
 */
function hideUnconnected(){
  let unconnected = this.nodes().filter(function(ele){
    return ele.degree(false) === 0;
  });
  unconnected.forEach(function(ele){
    ele.data('hidden', true);
    ele.toggleClass('hidden', true);
  });
}

/**
 * Merge the current collection with the collection of elements provided.
 * The merge is performed in such a way as to keep a single copy of intersecting
 * elements, and defining a dual layout that positions the intersecting elements
 * at the center of the viewport, with elements coming from the left panel at
 * the left of the intersection, and elements coming from the right panel at the
 * right of the intesection.
 * @param {collection} eles The elements we are to merge with the current graph.
 * @param {string} innerLyt Layout used for intersecting elements.
 * @param {string} outerLyt Layout used for non-intersecting elements, both at
 * the left and right sides of the display
 */
function mergeWith(eles, innerLyt="concentric", outerLyt="grid"){
  /* Define the collections that represent the three components of the new graph,
   * intersecting nodes, left-side nodes and right-side nodes */
  let inter = this.elements().intersection(eles);
  let left = inter.absoluteComplement();
  let right = eles.difference(inter);
  /* capture the dimensions of the viewport, to use them as constrains for the
   * layout positioning algorithms */
  let w = this.container().parentElement.clientWidth/3;
  let h = this.container().parentElement.clientHeight;
  /* define the layout algorithm for each part of the merged network */
  let interLyt = inter.updateLayout(innerLyt, {x1:w,y1:0,w:w,h:h} );
  let leftLyt = left.updateLayout(outerLyt, {x1:0,y1:0,w:w,h:h} );
  let rightLyt = right.updateLayout(outerLyt, {x1:2*w,y1:0,w:w,h:h} );

  /* set a promise to run the layout of the 2nd element of the merged network */
  leftLyt.promiseOn('layoutstop').then(function(){
    interLyt.run();
  });
  /* set a promise to run the layout of the 3rd element of the merged network */
  interLyt.promiseOn('layoutstop').then(function(){
    rightLyt.run();
  });
  /* once each part of the merged graph has been positioned, place them together
   * in a single cytoscape instance, and use it to replace the contents of the
   * current structure */
  rightLyt.promiseOn('layoutstop').then(function(cy){
    /* Define the merged collection of elements */
    let mrg = cytoscape();
    mrg.add(inter);
    mrg.add(left);
    mrg.add(right);
    /* Update the contents of the current graph with the ones computed */
    cy.elements().remove();
    cy.add(mrg.elements());
    cy.fit();
  }.bind(null, this)); // this is bounded to the inner function as the cy parameter

  /* run the layout on the left (first) section of the merged network */
  leftLyt.run();
}

/**
 * Returns elements in the collection to their default style, in terms of color
 * and shape of its components
 */
function setDefaultStyle(){//eles){
  this.forEach(function(ele){
    if ( ele.isEdge() ){
      ele.data("color", edgeColor.REGULAR);
      return;
    }
    let color = ele.data('type') === nodeType.mRNA ? nodeColor.MSG_RNA : nodeColor.MICRO_RNA;
    let shp = ele.data('type') === nodeType.mRNA ? nodeShape.MSG_RNA : nodeShape.MICRO_RNA;

    ele.data("color", color)
    ele.data('borderColor', color)
    ele.data("shape", shp);
  });
}


/**
 * Hide/Show nodes
 * @param {boolean} show Boolean value that indicates whether the hidden nodes
 * should be displayed or not. If they are to be displayed, then their 'hidden'
 * needs to be set to OFF (false), otherwise, it turned ON (true).
 */
  function toggleHiddenNodes(show){
    this.options().layout['showHidden'] = show;
    /* find all nodes in the graph that have a data element hidden set to true */
    let hiddenNodes = this.nodes('[?hidden]');
    /* for each element turn the class 'hidden' ON or OFF depending on the value
     * of show */
    hiddenNodes.forEach(function(ele){
      this.$('#'+ele.id()).toggleClass('hidden', !show);
    }, this);
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
 */
function toggleIntersectionHighlight(other, toggle){
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
cytoscape('collection', 'setDefaultStyle', setDefaultStyle);
cytoscape("collection", "updateLayout", updateLayout);

cytoscape("core", "hideUnconnected", hideUnconnected);
cytoscape("core", "mergeWith", mergeWith);
cytoscape("core", "toggleIntersectionHighlight", toggleIntersectionHighlight);
cytoscape("core", "initStyle", initStyle);
cytoscape("core", "updateLayout", updateLayout);
cytoscape('core', 'loadElements', loadElements);
cytoscape("core", "dualLayout", dualLayout);
cytoscape("core", "initContextMenu", initContextMenu);
cytoscape("core", "toggleHiddenNodes", toggleHiddenNodes);
cytoscape("core", "getToxyNodes", getToxyNodes);
cytoscape("core", "getToxyInteractions", getToxyInteractions);
