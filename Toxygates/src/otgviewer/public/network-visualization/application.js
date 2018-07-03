"use strict";

/** Convenience variable used to store a network, using Toxygates" general structure */
var toxyNet = new Network();

/** Convenience variable used to store a network using the format required by
 * each visualization library. It's content changes every time the user changes
 * the visualiation library */
var vizNet = null;

/**
 * Handles the sequence of operations that needs to be performed everytime the
 * user changes the visualization library used for the depiction of the network.<br>
 * TO-DO (split) Also for convenience used when the user loads a file, although
 * this has to be changed, as to allow the user to continuously load information.
 * HAVE TO WRITE A DIFFERENT FUNCTION FOR THAT
 * @param {String} library the string indicating the visualization library
 * selected by the user for the drawing of the network.
 */
function changeVisualizationLibrary(library=document.getElementById("library").value){
  var pos = [];
  // check if network is empty, and if not, store current node positions, so
  // we can transfere them to the new visualization
  if( vizNet !== null ){
    // to store node positions we use ad-hoc functions for each visualization
    // library
    if( library === "Cyto" ){ // previous was vis.js
      console.log("found Cyto");
      var p = vizNet.getPositions();
      for(var k in p){
        // pos.push([k, p[k].x, p[k].y] );
        toxyNet.updateNodePosition(k, p[k].x, p[k].y);
      }
    }
    else{ // previous was cytoscape.js
      console.log("found Vis");
      // vizNet.nodes().forEach(function(ele){
      //   this.push([ele.id(), ele.position().x, ele.position().y]);
      // },pos);
      vizNet.nodes().forEach(function(ele){
        this.updateNodePosition(ele.id(), ele.position().x, ele.position().y);
      },toxyNet);
    }
  }
  console.log("changeVisualizationLibrary");

  // enable the corresponding layout for the visualization
  switch (library) {
    case "Cyto":
      setCytoLayoutOptions();
      setCytoDisplay();
      break;
    case "Vis":
      setVisLayoutOptions();
      setVisDisplay();
      break;
  }


}


/**
 * Updates the available options for layout selection, based on what is ready
 * available for Cytoscape.js library.
 */
function setCytoLayoutOptions(){
  var layoutSelect = document.getElementById("layout");
  // clear previously available options
  for(var i = 0; i<layoutSelect.options.length; ++i)
    layoutSelect.remove(i);
  // add cytoscape.js options
  layoutSelect.options[0] = new Option("None", "none");
  layoutSelect.options[0].selected = "selected";
  layoutSelect.options[1] = new Option("Force Directed", "cose");
  layoutSelect.options[2] = new Option("Random/None", "random");
  layoutSelect.options[3] = new Option("Grid", "grid");
  enable(layoutSelect);
}

/**
* Updates the available options for layout selection, based on what is ready
* available for Vis.js library.
* In the case of Vis.js, we have to be careful, as whenever we modify the network,
* the modification automatically triggers the reorganization of the nodes using
* a force-directed layout.
*/
function setVisLayoutOptions(){
  var layoutSelect = document.getElementById("layout");
  // clear previously available options
  for(var i = 0; i<layoutSelect.options.length; ++i)
    layoutSelect.remove(i);
  // add vis.js options
  layoutSelect.options[0] = new Option("None", "none");
  layoutSelect.options[0].selected = "selected"
  layoutSelect.options[1] = new Option("Force Directed", "cose");

  enable(layoutSelect);
}

/**
 * export
 */
function exportFile(){
  var save = document.getElementById("export");



  save.addEventListener("click", exportJSON(toxyNet));


}

/**
 * Based on the selections made by the user through, it updates layout properties
 * and applies them to the visuialization of the network. <br>
 * By applying a layout algorithm, the user effectively reset all previous
 * modifications made to the network visualization.
 * @param {String} library the library used for display, if not specified, the
 * value is recovered directly from the website
 * @param {String} layout the layout option selected that identifies the
 * algorithm used the positioning of the nodes in the network
 * @return the layout object to be used in the display of the network
 */
function updateLayout(library = document.getElementById("library").value,
                      layout = document.getElementById("layout").value){

  // layout settings depend on the visualization library used, thus we have to
  // check what is the currently selected library, and modify accordingly
  if( library === "Cyto"){
    var lyt = {"name": layout}
    switch(layout){
      case "random":
      case "grid":
        break;
      case "none":
        lyt["name"] = "preset";
        break;
      case "cose":
        lyt["nodeRepulsion"] = 1000;
        lyt["gravity"] = 0.1;
        break;
      }
    vizNet.layout( lyt ).run();
  } // cytoscape library
  // library === "Vis"
  else{
    var opts = vizNet["options"];
    switch(layout){
      case "none":
        opts["physics"] = {
          "enabled": false
        };
        break;
      case "cose":
        opts["physics"] = {
          "enabled": true
        };
        break;
    }
    vizNet.setOptions(opts);
  }
}

/**
 * Initializes the canvas to be used with the Cytoscape.js library
 * @param {Component} display the site's element that will serve as canvas for
 * the visualization of the newtwork
 */
function setCytoDisplay(positions=[], display=document.getElementById("display")){

  console.log("toxynet", toxyNet);

  // initialize the network
  vizNet = cytoscape({
    container: display,
    elements: toxyNet.getCytoscapeElements(),
    layout:{
      name: "preset"
    }
  });

  console.log(vizNet);
  // UNCOMMENT if the positions are not stored in toxygates network structure
  // positions.forEach(function(ele){
  //   vizNet.$('#'+ele[0]).position({x: ele[1], y: ele[2]});
  // });

  // vizNet.layout({name: "preset"}).run();

  // update the node layout based on the selected option
  // updateLayout();
  // define the style to use for the depiction of the network elements
  vizNet.style()
    .selector("node")
    .style({
      "label": "data(label)",
      "text-valign": "center",
      "text-halign": "center",
      "background-color": "data(color)"
    })
    .update();

  // assign function to respond to selection events
  vizNet.on("select", function(evt){ updateSelectionText(evt.target); });
  vizNet.on("unselect", function(evt){
    document.getElementById("selectionList").innerText = "Current Selection (only-Cyto):";//
  });
}

/**
 * Initializes the canvas to be used with the Cytoscape.js library
 * @param {Array} positions the array that holds the positions of the node in the
 * network, required in order to avoid the repositioning of the nodes when switching
 * between libraries, as this information is not dinamically stored on the
 * network structure.
 * @param {Component} display the site's element that will serve as canvas for
 * the visualization of the newtwork
 */
function setVisDisplay(positions=[], display=document.getElementById("display")){
  // load the network
  var nodes = new vis.DataSet(toxyNet.getVisNodes());
  // UNCOMMENT if positions are not stored in toxygates network structure
  // positions.forEach( function(ele){
  //   nodes.update({id: ele[0], x: ele[1], y: ele[2]});
  // });
  var edges = new vis.DataSet(toxyNet.getVisEdges());

  var data = {
    nodes: nodes,
    edges: edges
  };
  // set visualization options, incluying layout
  var options = {
    layout:{
      improvedLayout: false
    },
    nodes:{
      heightConstraint: 30,
      widthConstraint: 30,
    },
    edges:{
      smooth: false
    },
    physics:{
      enabled: false
    },
    manipulation:{
      enabled: true
    }
  };

  console.log("opts ",options);
  vizNet = new vis.Network(display, data, options);

}

/**
 *
 */
function updateInteraction(option=document.getElementById("mouse").value,
  library=document.getElementById("library").value){

    if(library === "Cyto"){
      switch(option){
        case "selection":
          vizNet.off("click", addCytoNode);
          break;
        case "addNode":
          vizNet.on("click", addCytoNode);
          break;
        case "addInteraction":
          vizNet.off("click", addCytoNode);
          break;
      }

    }
    // library === "Vis"
    else{
      switch(option){
        case "selection":
          vizNet.setOptions({interaction: {dragNodes: true, dragView: true}});
          vizNet.off("click", addVisNode);
          vizNet.off("dragStart", addVisInteractionStart);
          vizNet.off("dragEnd", addVisInteractionEnd);
          break;
        case "addNode":
          // vizNet.addNodeMode();
          vizNet.on("click", addVisNode );
          vizNet.setOptions({interaction: {dragNodes: false, dragView: true}});
          vizNet.off("dragStart", addVisInteractionStart);
          vizNet.off("dragEnd", addVisInteractionEnd);
          break;
        case "addInteraction":
          window.alert("Drag the mouse from node to node to add an interaction");
          vizNet.setOptions({interaction: {dragNodes: false, dragView: false}});
          vizNet.off("click", addVisNode);
          vizNet.on("dragStart", addVisInteractionStart);
          vizNet.on("dragEnd", addVisInteractionEnd);
          break;
      }
    }
}

var addCytoNode = function(event){
  console.log(event);
  var id = window.prompt("Compound ID:", "unique string");
  if( id !== null && id !== "" ){
    var node = new GraphicNode(id, "msgRNA", id, event.position.x, event.position.y )
    toxyNet.addNode(node);

    // var data = {
    //   nodes: toxyNet.getVisNodes(),
    //   edges: toxyNet.getVisEdges()
    // };
    vizNet.add({
      group: "nodes",
      classes: "msgRNA",
      data:{
        id: id,
        label: id,
        color: "#007f7f"
      },
      position: event.position
    });
  }
}

var addVisNode = function (element){

  var id = window.prompt("Compound ID:", "unique string");
  console.log({id: id, label: id, type: "msgRNA", shape: "circle", x: element.pointer.canvas.x, y: element.pointer.canvas.y});
  if( id !== null && id !== "" ){
    var node = new GraphicNode(id, "msgRNA", id, element.pointer.canvas.x, element.pointer.canvas.y )
    toxyNet.addNode(node);

    var data = {
      nodes: toxyNet.getVisNodes(),
      edges: toxyNet.getVisEdges()
    };
    vizNet.setData(data);
  }
}

var fromId = null;
var addVisInteractionStart = function(element){
  console.log("started dragging");
  var f = vizNet.getNodeAt(element.pointer.DOM);
  if( f !== undefined )
    fromId = f;
  console.log("from:", fromId);

}

var addVisInteractionEnd = function(element){
  console.log("finished dragging");
  var t = vizNet.getNodeAt(element.pointer.DOM);
  // console.log("fromto:", fromId, vizNet.getNodeAt(element.pointer.DOM));
  if( fromId !== null && t !== undefined ){
    var inter = new Interaction(fromId, t);
    toxyNet.addInteraction(inter);

    var data = {
      nodes: toxyNet.getVisNodes(),
      edges: toxyNet.getVisEdges()
    };
    vizNet.setData(data);
  }
  fromId = null;
}

/**
 * In charge of actually displaying the network on the canvas.
 * @param {String} library a string representing the library used to display the
 * network, linked to the select component available for user interaction on the
 * main site.
 */
function repaint(library=document.getElementById("library").value){

    var pos = [];
    // check if network is empty, and if not, store current node positions, so
    // we can transfere them to the new visualization
    if( vizNet !== null ){
      // to store node positions we use ad-hoc functions for each visualization
      // library
      if( library === "Cyto" ){ // previous was vis.js
        console.log("found Cyto");
        vizNet.nodes().forEach(function(ele){
          this.updateNodePosition(ele.id(), ele.position().x, ele.position().y);
        },toxyNet);
      }
      else{ // previous was cytoscape.js
        console.log("found Vis");
        var p = vizNet.getPositions();
        for(var k in p){
          // pos.push([k, p[k].x, p[k].y] );
          toxyNet.updateNodePosition(k, p[k].x, p[k].y);
        }

      }
    }


    // enable the corresponding layout for the visualization
    switch (library) {
      case "Cyto":
        setCytoLayoutOptions();
        setCytoDisplay();
        break;
      case "Vis":
        setVisLayoutOptions();
        setVisDisplay();
        break;
    }


}

/**
 * UpdateSelectionText
 * To help the user, we constantly provide a text list of the objects on which
 * any particular change in the visualization could be applied. i.e. what is the
 * current selection of elements
 */
function updateSelectionText(source){
  var txt = document.getElementById("selectionList").innerText;
  // if( network === undefined ){
  //   return;
  // }
  // when its a cytoscape visualization
  // var selected = network.collection(':selected');
  // console.log(selected.length);
  if( source.isNode() )
    txt += ("\n"+source.data("id"));
  document.getElementById("selectionList").innerText = txt;
  // console.log(txt);
}


/**
 *
 */
function updateColor(field){
  if (field == "None" ){
    console.log("sin color");
    return true;
  }

  network.batch( function() {

    var min = 99999;
    var max = 0;

    // calculate the scale required for colouring
    network.nodes(".mRNA").forEach( function(ele){
      var value = parseFloat(ele.data(field));
      if( value != null ){
        if(value > max){
          max = value;
        }
        if(value <min){
          min = value;
        }
      }
    });
    console.log(min, max);

    // first, we update the 'color' property of the nodes
    network.nodes(".mRNA").forEach( function(ele) {
      var value = parseFloat(ele.data(field));
      if( value != null ){
        // console.log(hsv2rgb(0,1,value));
        ele.data("color", hsv2rgb(0,(value-min)/(max-min),1));
      }
    });

  });

  console.log(network.nodes());

}


/**
*
*/
function initListeners(){

  var colorBy = document.getElementById("colorBy");
  colorBy.addEventListener("change", function(e){
    updateColor(colorBy.value);
  });

  // var exportBtn = document.getElementById('export');
  // exportBtn.addEventListener("click", function(e){
  //   net.exportJSON();
  // });

}

/**
 * Called by Toxygates to get the desired height, in pixels, of the user interaction div.  
 */
function uiHeight(){
  return 235;
}


/**
 * Called by Toxygates once the user interface HTML has been loaded and all 
 * scripts have been injected.  
 */
function onReadyForVisualization(){
  toxyNet = convertedNetwork; // Gets converted network from Toxygates
  repaint();
}
