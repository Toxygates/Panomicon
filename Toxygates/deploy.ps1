Set-Variable -Name "TGCP"  -Value "war/WEB-INF/classes"
Set-Variable -Name "TOOLCP" -Value "../OTGTool/classes"

Set-Variable -Name "WARLIB" -Value "war/WEB-INF/lib"

Remove-Item ($WARLIB + "/*jar")
Copy-Item "lib/jar/*.jar" $WARLIB
Copy-Item "lib/bundle/*.jar" $WARLIB
Copy-Item "mlib/*.jar" $WARLIB
Copy-Item "../OTGTool/lib/jar/*.jar" $WARLIB
Copy-Item ../OTGTool/lib/bundle/*.jar $WARLIB #error
Copy-Item ../OTGTool/mlib/*.jar $WARLIB

# These should be in the shared tomcat lib dir (tglobal.jar)
Remove-Item ($WARLIB + "/kyotocabinet*jar")
Remove-Item ($WARLIB + "/scala-library.jar") #error
# These should not be deployed in a servlet context
Remove-Item ($WARLIB + "servlet-api*.jar")
Remove-Item ($WARLIB + "javax.servlet-api*.jar")
Remove-Item ($WARLIB + "javaee-api*jar")
Remove-Item ($WARLIB + "scalatest*jar")
Remove-Item ($WARLIB + "gwt-user.jar") # error
Remove-Item ($WARLIB + "scala-xml*.jar")

Copy-Item war/WEB-INF/web.xml war/WEB-INF/web.xml.bak

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
$files = Get-ChildItem -Recurse -File -Path WEB-INF | Where {$_.FullName -notlike "*WEB-INF\classes\t\admin*"} |`
  Where {$_.FullName -notlike "*WEB-INF\classes\t\global*"} |`
  Where {$_.FullName -notlike "*WEB-INF\classes\t\tomcat*"} | foreach {$_.FullName}
foreach ($sublist in (Chunk-Array $files)) {
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
$files = Get-ChildItem -Recurse -File -Path WEB-INF | Where {$_.FullName -notlike "*WEB-INF\classes\t\global*"} |`
  foreach {$_.FullName}
foreach ($sublist in (Chunk-Array $files)) {
  jar uf admin.war $sublist
}
Set-Location ..

Move-Item -Force war/WEB-INF/web.xml.bak war/WEB-INF/web.xml

jar cf gwtTomcatFilter.jar -C $TGCP t/tomcat