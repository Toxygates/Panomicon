This is the Toxygates back-end, OTGTool.
To compile with ant, first set SCALA_HOME (a scala 2.12.x series version is needed).

The ivy ant task is also needed.
Place ivy.jar in a directory called e.g. antlib, and run the following command:

$ant -lib antlib compile

To generate scaladoc documentation in docs/ :
$ant -lib antlib docs

To run unit tests, the location of kyoto cabinet libraries (native and Java bindings) must be supplied:
$export KC_LIB_DIR=/usr/local/lib
$ant -lib antlib test

