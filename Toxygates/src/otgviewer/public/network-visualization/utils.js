/**
 * Convenience function used to load a file containing network data.<br>
 * The load of information should be later made mainly throught the Toxygates
 * platform.
 * @callback processFile once the file has been correctly loaded, the processFile
 * function is used to parse its contents and load the actual information
 */
function loadFile(){
  var fileInput = document.getElementById("fileSelect");
  if( fileInput.files.length != 0 ){
    // create an url object to load the file
    var url = URL.createObjectURL(fileInput.files[0]);
    // define a new XML HTTP Request
    var rqst = new XMLHttpRequest();
    // request the file to be open
    rqst.open("GET", url);
    // process the request once its ready
    rqst.onreadystatechange = function(){
      if( rqst.readyState == 4 && rqst.status == 200 ){
        // console.log('finished loading, now to process');
        var fileType = document.getElementById("fileType").value;
        // depending on the type of file we are loading, we call an ad-hoc
        // processing callback function
        switch(fileType){
          case "Edge":
            processEdgeFile(rqst.responseText);
            break;
          case "Node":
            processNodeFile(rqst.responseText);
            break;
          case "Json":
            processJsonFile(rqst.responseText);
            break;
          default:
            window.alert("Something wrong happened, aborting file loading");
        } // switch
      } // if
    } //rdy state change
    rqst.send();
  }
  else{
    window.alert("No file to load");
  }
}

/**
 * Interaction files contain information regarding the relationships that are
 * found between messengerRNA and microRNA components of the graphy.
 * @param {String} inputData a string that represents the contents of an input
 * file.
 */
function processEdgeFile(inputData){
  // clean the input string from unnecesary line jumps
  inputData = inputData.trim().split(/\r|\n/);

  // we need to decide whether or not to ignore the first line of the file, and
  // we let the user decide
  var trimFirst = window.confirm("Use first line of the data file?\n"+
    inputData[0]
  );

  // if the user confirms the data type, we proceed with loading and visualization
  var process = window.confirm("Parsing INTERACTIONS data");
  if( process ){

    // for each line in the file, we crate a new instance of the corresponding data
    // type, and add or update the corresponding information in our network
    var i;
    for( i=(trimFirst)? 1 : 0; i<inputData.length; ++i ){
      var line = inputData[i].split(/\t|\s/);
      // console.log(inputData[i]);
      // console.log(line);

      var micro = new GraphicNode(line[0], 'microRNA', line[0]);
      toxyNet.addNode(micro);

      // add nodes that represent msgRNA nodes
      var msg = new GraphicNode(line[1], 'msgRNA', line[1]);
      // UNCOMMENT WHEN GENE ID BECOMES AVAILABLE ON MICRO_RNA DATASET
      // var msn = new Node(line[???], 'msgRNA', line[1]);
      // -------------------------------------------------------------
      toxyNet.addNode(msg);

      // add interactions between microRNA and msgRNA nodes
      if( micro.id !== msg.id) {
        var int = new Interaction(micro.id, msg.id);
        toxyNet.addInteraction(int);
      }
    }
  } // for i

  // we visualize the loaded data
  repaint();
  // changeVisualizationLibrary();
}

 /**
 * TO - DO
 * A file can contain two types of information:<br>
 * 1. Interaction Data - edges between messenger RNA and micro RNA components<br>
 * 2. Node Data - expression levels associated to messenger RNA components<br>
 * Each time a file is loaded, the corresponding information is parsed and added
 * to the network representation.<br>
 * For the parsing to work correctly, the user needs to select the type of
 * information he is intending to load. Confirmation windows are provided through
 * this process.
 * Once the information is loaded, the canvas is repainted.
 * @param {String} rnaData a String representing the content of the file uploaded by the
 * user
 */
function processNodeFile(inputData){
  // messengerRNA files include field names on the first line, we save each of
  // these for later use
  var fields = rnaData[0].split('\t');
  // NODE-type files contain information only about msgRNA node expression
  if( dataType === "Node" ){
    // UNCOMMENT WHEN GENE ID BECOMES AVAILABLE ON MICRO_RNA DATASET
    // var msn = new Node(line[0],'msgRNA', line[1]);
    // -------------------------------------------------------------
    var msg = new GraphicNode(line[1], 'msgRNA', line[1]);

    // var colorBy = document.getElementById('colorBy');
    // for(var i=2; i<fields.length; ++i){
    //   colorBy.options[colorBy.options.length] = new Option(fields[i], fields[i]);
    // }

    for(var j=2; j<fields.length;++j){
      msg.addWeight(fields[j],line[j]);
    }

    toxyNet.updateNode(msg);


  }

}

/**
* TO - DO
* @param {String} rnaData a String representing the content of the file uploaded by the
* user
*/
function processJsonFile(inputData){

    var inputData = JSON.parse(inputData);

    console.log(inputData["nodes"]);
    toxyNet.title = inputData["title"];
    toxyNet.nodes = inputData["nodes"];
    toxyNet.interactions = inputData["interactions"];

    repaint();
}

/**
 *
 */
function exportJson(library=document.getElementById("library").value){

  if( vizNet !== null ){
    // to store node positions we use ad-hoc functions for each visualization
    // library
    if( library === "Cyto" ){ // previous was vis.js
      vizNet.nodes().forEach(function(ele){
        this.updateNodePosition(ele.id(), ele.position().x, ele.position().y);
      },toxyNet);
    }
    else{ // previous was cytoscape.js
      var p = vizNet.getPositions();
      for(var k in p){
        // pos.push([k, p[k].x, p[k].y] );
        toxyNet.updateNodePosition(k, p[k].x, p[k].y);
      }

    }
  }

  var str = JSON.stringify(toxyNet);
  console.log(str);
  let dataUri = 'data:application/json;charset=utf-8,'+ encodeURIComponent(str);

    let exportFileDefaultName = 'data.json';

    let linkElement = document.createElement('a');
    linkElement.setAttribute('href', dataUri);
    linkElement.setAttribute('download', exportFileDefaultName);
    linkElement.click();
}

/**
 *
 */
function enable(element){
  element.disabled = false;
}

/**
 *
 */
function disable(element){
  element.disabled = true;
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
