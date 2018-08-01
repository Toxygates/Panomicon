"use strict";
/** variable we need to link the internal work of toxygates with the testing
 * wrapper */
var convertedNetwork;

/**
 * Initialization function , called by JQuery once the html of the site is
 * completely loaded
 */
// $( function(){
//   console.log("Site Loaded");
// });

/**
 * Convenience function used to load a file containing network data, stored
 * directly from toxygates on a JSON file.<br>
 */
function loadFile(){
  var file = document.getElementById("fileSelect").files[0];
  // create an url and request the file
  var url = URL.createObjectURL(file);
  var rqst = new XMLHttpRequest();
  rqst.open("GET", url);
  rqst.onreadystatechange = function(){
    // loading of data finished correctly, thus we can proceed to its processing
    if( rqst.readyState == 4 && rqst.status == 200 ){
      convertedNetwork = JSON.parse(rqst.responseText);
      // aqui es donde la ejecucion se pone a la par con lo que pasa en toxygates
      onReadyForVisualization();
    }
  }
  rqst.send();
}
