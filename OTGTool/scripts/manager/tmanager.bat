@echo off

set MAIN_ROOT=C:\Users\yuji\Documents\Toxygates\repository
set OTGTOOL_ROOT=%MAIN_ROOT%\OTGTool
set TOXY_ROOT=%MAIN_ROOT%\Toxygates

set CLASSPATH=%OTGTOOL_ROOT%\lib\jar\*;%TOXY_ROOT%\war\WEB-INF\lib\*;%OTGTOOL_ROOT%\mlib\*;%TOXY_ROOT%\mlib\*

set T_TS_URL=http://localhost:3030/otg/query
set T_TS_UPDATE_URL=http://localhost:3030/otg/update

::Forces read-only mode for triplestore when uncommented
::set T_TS_UPDATE_URL=

set T_TS_USER=x
set T_TS_PASS=y
::set T_TS_REPO=ttest

set T_DATA_DIR=C:\Users\yuji\Documents\Toxygates\kyoto_data
::set T_DATA_DIR=C:\Users\yuji\Documents\Toxygates\kyoto_testdata
set T_DATA_MATDBCONFIG=#pccap=1073741824#msiz=4294967296

::echo "%OTGTOOL_ROOT%\lib\jar;%OTGTOOL_ROOT%\lib\bundle;%OTGTOOL_ROOT%\mlib;%OTGTOOL_ROOT%\classes;%CLASSPATH%"

scala -J-Xmx4g -classpath "%OTGTOOL_ROOT%\lib\jar;%OTGTOOL_ROOT%\lib\bundle;%OTGTOOL_ROOT%\mlib;%OTGTOOL_ROOT%\classes;%CLASSPATH%" t.manager.Manager %*
