"use strinct";
var hola
// this is the Graph - a Cytoscape object
var vizNet = null;

// this is also the Graph - using Network structure
var toxyNet = null;

/** ----------------------- FORM HANDLERS ---------------------------- **/

/**
 * Changes the layout of the nodes in the network according to the user's
 * selection. Interface to already implemented algorithms for node positioning
 * included with Cytoscape
 */
$(document).on("click", "#layoutSelect", function (){
  // get the option selected by the user
  var opt = $("#layoutSelect").find(":selected").val();
  // update the layout accordingly
  vizNet.updateLayout(opt);
});

/** ---------------------- UPDATE NODE MODAL ---------------------------- **/

/**
 * Handle updates made on a node through the corresponding modal. Once the user
 * selects to update, we check each of the node's properties, and whenever we
 * find any change, we register them on the corresponding instance in the graph
 */
 $(document).on("click", "#updateNode", function(event){
   var node = vizNet.nodes().getElementById($("#nodeID").val());
   /* nodeID is not to be changed by the user */
   /* nodeLabel */
   var label = $("#nodeLabel").val();
   node.data("label", label);

   /* nodeType */
   node.data("type", $("#nodeType").val());

   /* nodeWeights */
   var selection = $("#nodeWeights").val();
   if( selection !== null ){
     var w = node.data("weight");
     w[selection] = $("#weightValue").val();
     node.data("weight", w);
   }

   /* nodeColor */
   var color = $("#nodeColor").val();
   node.data("color", color);
   node.style("background-color", color);

   /* nodeShape */
   var shape = $("#nodeShape").val();
   node.style("shape", shape);

   /* hide the modal */
   var modal = $(event.target).data().modal;
   $("#"+modal).hide();
 });

 /**
  * Before applying changes, whenever the user changes the current type of the
  * node, the default properties for the selected type are shown in the
  * corresponding fields
  */
$(document).on("change", "#nodeType", function(evt){
  // get the text from the currently selected option
  var type = $("#nodeType :selected").text();
  // update default color for this type of node
  $("#nodeColor").val(nodeColor[type]);
  // update default shape for this type of node
  $("#nodeShape").val(nodeShape[type]);
});

 /**
  * When looking at a modal with node properties, this function handles the
  * display of the correct value when the user chooses a different weight from
  * available list
  */
$(document).on("change", "#nodeWeights", function(evt){
  var node = vizNet.nodes().getElementById($("#nodeID").val());
  var selection = $("#nodeWeights").val();
  if( selection !== null )
    $("#weightValue").val(node.data()["weight"][selection]);
  else
    $("#weightValue").val("");
});

/** ---------------------- UPDATE GRAPH COLORING ------------------------ **/

/**
 * Apply the user defined color scale to the type of nodes selected by the user.
 */
$(document).on("click", "#colorGraph", function(evt){
  // type of node we should color, and weight used for the linear scaling
  var type = $("#graphColorTo").val();
  var w = $("#graphColorBy").val();
  // we only apply a color scale if both values have been selected
  if ( type !== null && w !== null ){
    // get a collection of nodes to color
    var trg = vizNet.nodes("[type = '"+type+"']");
    // get min and max values for the color scale, together with the value
    // associated with white
    var min = Number($("#minRange").val());
    var max = Number($("#maxRange").val());
    var white = Number($("#whiteRange").val());
    // apply the color change to all nodes within the collection
    trg.forEach(function(ele){
      var d = ele.data("weight");
      ele.style('background-color', valueToColor(d[w], min, max, white,
        $("#minColor").val(),
        $("#maxColor").val()));
    });
  }

  /* hide the modal after color has been applied to nodes */
  var modal = $(event.target).data().modal;
  $("#"+modal).hide();

});

/**
 * Color scales can be applied to a whole type of nodes within a graph. In order
 * to apply such a color scale, we first need to determine on which type of node
 * the coloring will be applied. Once the type of node is selected, we need to
 * dynamically change the options, before we can actually apply the color scale.
 */
$(document).on("change", "#graphColorTo", function(evt){
  // the type of node we will be coloring
  var type = ($("#graphColorTo").val());
  var trg = vizNet.nodes("[type = '"+type+"']")[0];
  var weights = Object.keys(trg.data()["weight"]);
  if( weights !== null && weights !== undefined ){
    $("#graphColorBy").empty();
    $("#graphColorBy").append(new Option("Select...",null));
    for(var i=0; i<weights.length; ++i)
      $("#graphColorBy").append(new Option(weights[i], weights[i]));
  }
});

/**
 * Each time the user select a particular weight within the set of nodes, the
 * parameters for the color scale need to be re-defined and shown, so that an
 * appropriate selection can be made
 */
$(document).on("change", "#graphColorBy", function(evt){
  //  get a collection of nodes to which apply the color scale
  var type = $("#graphColorTo").val();
  var trg = vizNet.nodes("[type = '"+type+"']");

  var w = $("#graphColorBy").val();
  if( w !== null ){
    var min = trg.min(function(ele){
      var d = ele.data("weight");
      return d[w];
    });
    var max = trg.max(function(ele){
      var d = ele.data("weight");
      return d[w];
    });
    // update minimum and maximim values
    $("#minRange").val(min.value.toFixed(2));
    $("#maxRange").val(max.value.toFixed(2));
    var threshold = ((max.value+min.value)/2);
    $("#colorRange").val(50);
    $("#whiteRange").val(threshold .toFixed(2));
  }
});

/**
 * Handle the interaction of the user with the slider that determines the
 * position of the white level in the coloring scale.
 */
$(document).on("change", "#colorRange", function(evt){
  var min = Number($("#minRange").val());
  var max = Number($("#maxRange").val());
  console.log(Number($("#colorRange").val()), min, max);
  $("#whiteRange").val( min + $("#colorRange").val()*(max-min)/100 );
});

/** ---------------------- UPDATE GRAPH COLORING ------------------------ **/

/**
 * Invoqued when the user selects to apply filters to the visualization. Notice
 * that all filters currently active (listed) will be applied to the
 * visualization.
 */
$(document).on("click", "#filterNodes", function(evt){
  console.log("#filterNodes", evt);

  /* since the application of filters could translate in several drawing
   * iterations, we batch them, as to optimize rendering */
  vizNet.batch(function(){
    // make all elements in the graph visible again, as elements that should not
    // be filtered by the current list of filters could remain invisible due to
    // previous filtering rules
    vizNet.nodes().style("display", "element");

    // get the current list of filters to be applied
    var list = $("#filterList")[0];
    for(var i=0; i<list.childElementCount; ++i ){
      // get the details of each filter included within the list
      var data = list.children[i].dataset;
      var filtered;
      if(data["type"] === "degFilter"){
        // select the group of nodes that should be filtered out from the
        // visualization
        filtered = vizNet.nodes().filter(function(ele){
          return ele.degree(false) < parseInt(data["degree"]);
        });
      }
      // by setting their display to "none", we effectively prevent nodes to be
      // show, without permanently removing them from the graph
      filtered.style('display', 'none');
    }
  });

  /* onde all filters have beeb applied, hide the modal */
  var modal = $(event.target).data().modal;
  $("#"+modal).hide();
});

/** ------------------------------------------------------------------ **/
/**                          Other Functions                           **/
/** ------------------------------------------------------------------ **/

/**
 * Initialize the visualization canvas with the data currently loaded through
 * the toxyNet object from Toxygates. Once this is done, all modifications to
 * the graph will only be kept on the cytoscape object (vizNet) and will have to
 * be persisted manually via user interaction
 */
function initDisplay(){
  // container for the visualization
  var display = $("#display");
  // initialize the network, using the appropriate container, and with the list
  // of objects obtained from the toxyNet graph representation
  vizNet = cytoscape({
    container: display,
    elements: toxyNet.getCytoElements(),
  });

  // default style for network elements
  vizNet.initStyle();

  // context menu, where most user interaction options are provided
  vizNet.initContextMenu();

  // visually show selected nodes, by drawing them with a border
  vizNet.on("select", "node", function(evt){
    var source = evt.target;
    if( source.isNode() )
    source.style({
      "border-width": "5px"
    });
  });

  // remove border when elements are unselected
  vizNet.on("unselect", "node", function(evt){
    var source = evt.target;
    source.style("border-width", "0px"  );
  });
}

/**
 * Handle the hiding of a specific modal element. Modals are used to capture
 * user interactions, and ara only shown upon selection.
 * When the close option is selected, no other action apart from the hiding of
 * the modal is performed. All information already added by the user is lost.
 */
$(document).on("click", ".modal-close", function(event){
  console.log(".modal-close", event);
  var modal = $(event.target).data().modal;
  $("#"+modal).hide();
});//)

/**
 *
 */
$(document).on("change", "input[type=color]", function(evt){
  console.log(evt);
});


/**
 * Handle the addition of filter rules to the corresponding list. Later, and
 * upon user selection, filters are applied to the network visualization
 */
$(document).on("click", ".modal-add", function(event){
  /* list of filters currently defined */
  var list = $("#filterList")[0];

  /* create a new filter element */
  var type = $(event.target).data().type;
  var node = document.createElement("li");
  node.setAttribute("data-type", type);

  /* define the filter element */
  var html = "";
  if( type === "degFilter"){
    var deg = Math.trunc($("#nodeDegree").val());
    deg = deg >= 0 ? deg : 0;
    node.setAttribute("data-degree", deg);
    html += ("Degree at least "+deg);
    html +=  "<span class='modal-remove'>&times;</span>";

  }
  node.innerHTML = html;

  /* add the new filter to the list of filters */
  list.appendChild(node);
});

/**
 * Handle the removal of a specific filter from the corresponding list.
 */
$(document).on("click", ".modal-remove", function(event){
  /* the list of filters */
  var list = $("#filterList")[0]
  /* removal of the element selected by the user */
  list.removeChild(event.target.parentElement);
})

/** ------------------------------------------------------------------ **/
/**          Required methods for toxygates integration                **/
/** ------------------------------------------------------------------ **/

/**
 * Called by Toxygates once the user interface HTML has been loaded and all
 * scripts have been injected
 */
function onReadyForVisualization(){
  /* convertedNetwork is an object I get straight from toxygates... it is
   * equivalente to the result of reading a json file... it gives me a JSON
   * style string to work with */
  toxyNet = new Network(convertedNetwork["title"],
    convertedNetwork["interactions"],
    convertedNetwork["nodes"]); // Gets converted network from Toxygates
  /* once I get the network, I need to use that information within the cytoscape
   * context. The conversion to proper format and all initialization is then
   * performed by initDisplay() */
  initDisplay();
  /* Move the Cytoscape context menu into the modal GWT network visualization 
   * dialog, because otherwise input to it will be intercepted. */ 
  $(".cy-context-menus-cxt-menu").appendTo($(".gwt-DialogBox"));
}

/**
 * Called by Toxygates to get the desired height, in pixels, of the user
 * interaction div
 */
function uiHeight(){
  return 52;
}

/**
 * Test method for save functionality
 */
function saveStuff(){
  window.saveNetworkToToxygates(toxyNet);
}
