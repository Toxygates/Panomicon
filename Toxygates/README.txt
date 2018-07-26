This is the Toxygates front-end.
It depends on the OTGTool back-end, which should be compiled first.

To compile with ant, first set SCALA_HOME (the current tested version is 2.11.11).
Edit build.xml and set the location of the GWT SDK correctly (gwt.sdk).
Place ivy.jar in a directory called e.g. antlib, and run the following command:

ant -lib antlib build

