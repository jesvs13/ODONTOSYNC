Write-Host "=== OdontoSync · revisión Semana 10 ==="

Write-Host "1/4 Pruebas automatizadas"
mvn clean test
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "2/4 Empaquetado WAR"
mvn clean package
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "3/4 Dependencias con actualizaciones disponibles"
Write-Host "Este paso consulta repositorios Maven y necesita Internet."
mvn org.codehaus.mojo:versions-maven-plugin:2.18.0:display-dependency-updates
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "4/4 Vulnerabilidades de dependencias (OWASP Dependency-Check)"
Write-Host "La primera ejecución puede tardar porque descarga la base de vulnerabilidades."
mvn -Psecurity-audit org.owasp:dependency-check-maven:check
exit $LASTEXITCODE
