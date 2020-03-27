Set-Variable -Name "TGCP"  -Value "war/WEB-INF/classes"
Set-Variable -Name "TOOLCP" -Value "../OTGTool/classes"

Set-Variable -Name "WARLIB" -Value "war/WEB-INF/lib"

Move-Item -Path $WARLIB lib-backup
New-Item -ItemType directory $WARLIB

Copy-Item "lib/jar/*.jar" $WARLIB
Copy-Item "lib/bundle/*.jar" $WARLIB
Copy-Item "mlib/*.jar" $WARLIB
Copy-Item "../OTGTool/lib/jar/*.jar" $WARLIB
Copy-Item "../OTGTool/lib/bundle/*.jar" $WARLIB #error
Copy-Item "../OTGTool/mlib/*.jar" $WARLIB
Copy-Item $env:GWT_SDK/gwt-servlet.jar $WARLIB

# These should be in the shared tomcat lib dir (tglobal.jar)
#Remove-Item ($WARLIB + "/*kyotocabinet*jar")
#Remove-Item ($WARLIB + "/scala-library.jar") #error
# These should not be deployed in a servlet context
#Remove-Item ($WARLIB + "servlet-api*.jar")
#Remove-Item ($WARLIB + "javax.servlet-api*.jar")
#Remove-Item ($WARLIB + "javaee-api*jar")
#Remove-Item ($WARLIB + "scalatest*jar")
#Remove-Item ($WARLIB + "gwt-user.jar") # error
#Remove-Item ($WARLIB + "scala-xml*.jar")

#We need to use backslashes here because we'll be matching full paths as strings
Set-Variable -Name "WEBINFLIB" -Value "WEB-INF\lib\"
$ExcludeJars = "*kyotocabinet*jar", "scala-library.jar", "servlet-api*.jar", "javax.servlet-api*.jar", "javaee-api*jar", "scalatest*jar", "gwt-user.jar", "scala-xml*.jar"
$ExcludeJars = foreach ($jar in $ExcludeJars) {
  "*" + $WEBINFLIB + $jar
}

Copy-Item war/WEB-INF/web.xml war/WEB-INF/web.xml.bak

# Filters out all elements of $Array that meet any of the wildcard matches
# in $Exclusions
function Exclude-All ($Array, $Exclusions) {
  foreach($exclusion in $Exclusions) {
    $Array = $Array | Where {$_ -notlike $exclusion}
  }
  $Array
}

# We use this to chunk large lists of filenames because PowerShell can't 
# handle commands that are more than 32,768 characters long
function Chunk-Array ($Array, $BoxSize = 100) {
  for($i=0; $i -lt $Array.length; $i += $BoxSize) {
    ,($Array[$i .. ($i + $BoxSize - 1)])
  }
}

# makeWar
Set-Variable -Name "OUTPUT" -Value "toxygates-template.war"
Copy-Item -Recurse -Force ($TOOLCP + "/friedrich") $TGCP
Copy-Item -Recurse -Force ($TOOLCP + "/otg") $TGCP
Copy-Item -Recurse -Force ($TOOLCP + "/t") $TGCP 
Set-Location war
Remove-Item $OUTPUT
Remove-Item WEB-INF/web.xml
If(!(test-path csv)) {
    New-Item -ItemType Directory -Force -Path csv
}
Remove-Item csv/*.csv
jar cf $OUTPUT toxygates images csv *.pdf *.css *.html.template *.zip
# Exclude classes in some packages
$AllFiles = Get-ChildItem -Recurse -File -Path WEB-INF | foreach {$_.FullName}
$ExcludePaths = "*WEB-INF\classes\t\admin*", "*WEB-INF\classes\t\global*", "*WEB-INF\classes\t\tomcat*"
$Exclusions = $ExcludeJars + $ExcludePaths
$Files = Exclude-All $AllFiles $Exclusions | foreach { Resolve-Path -Relative $_ }
foreach ($sublist in (Chunk-Array $Files)) {
  jar uf $OUTPUT $sublist
}
Set-Location ..

#makeAdminWar
Copy-Item -Recurse -Force ($TOOLCP + "/friedrich") war/WEB-INF/classes
Copy-Item -Recurse -Force ($TOOLCP + "/otg") war/WEB-INF/classes
Copy-Item -Recurse -Force ($TOOLCP + "/t") war/WEB-INF/classes
Set-Location war
Copy-Item WEB-INF/web.xml.admin WEB-INF/web.xml
Remove-Item admin.war
jar cf admin.war OTGAdmin admin.html *.css images
$Exclusions = $ExcludeJars + "*WEB-INF\classes\t\global*", "*WEB-INF\classes\t\tomcat*"
$Files = Exclude-All $AllFiles $Exclusions | foreach { Resolve-Path -Relative $_ }
foreach ($sublist in (Chunk-Array $Files)) {
  jar uf admin.war $sublist
}

Set-Location ..

Move-Item -Force war/WEB-INF/web.xml.bak war/WEB-INF/web.xml
Remove-Item -Recurse $WARLIB 
Move-Item -Path lib-backup $WARLIB

jar cf gwtTomcatFilter.jar -C $TGCP t/tomcat
