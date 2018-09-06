"use strict";
/** variable we need to link the internal work of toxygates with the testing
 * wrapper */
var convertedNetwork;

/**
 * Convenience function used to load a file containing network data, stored
 * directly from toxygates on a JSON file.<br>
 */
function loadFile(){

  $("#display").width(1000);
  $("#display").height(1000);

  var file = document.getElementById("fileSelect").files[0];
  // create an url and request the file
  var url = URL.createObjectURL(file);
  var rqst = new XMLHttpRequest();
  rqst.open("GET", url);
  rqst.onreadystatechange = function(){
    // loading of data finished correctly, thus we can proceed to its processing
    if( rqst.readyState == 4 && rqst.status == 200 ){
      convertedNetwork = JSON.parse(rqst.responseText);
      console.log("cnet", convertedNetwork);
      // aqui es donde la ejecucion se pone a la par con lo que pasa en toxygates
      onReadyForVisualization();
    }
  }
  rqst.send();
}

/**
 * Convenience function to see if the saving of network is working or not
 */
$(document).on("click", "#saveMe", function(event){
  saveStuff();
});
