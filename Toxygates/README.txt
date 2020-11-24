This is the Toxygates front-end.
It depends on the OTGTool back-end (../OTGTool), which should be compiled first.

To compile with ant, the following configuration variables must be set 
(any scala 2.12.x version should work)

$export GWT_SDK=/path/to/gwt-2.8.2
$export SCALA_HOME=/path/to/scala-2.12.6   

The ivy ant task is also needed.
Place ivy.jar in a directory called e.g. antlib, and run the following command:

$ant -lib antlib build

To generate scaladoc documentation in docs/ :
$ant -lib antlib docs

To run unit tests:
$ant -lib antlib test

