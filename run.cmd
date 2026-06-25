@echo off
rem Uruchomienie rdzenia Spring (server/) - JDK 21 + Maven 3.9.x.
setlocal
if "%JAVA_HOME21%"=="" (echo [ERROR] Brak zmiennej JAVA_HOME21 ^(JDK 21^)& exit /b 1)
if "%MAVEN_HOME9%"=="" (echo [ERROR] Brak zmiennej MAVEN_HOME9 ^(Maven 3.9.x^)& exit /b 1)
set "JAVA_HOME=%JAVA_HOME21%"
call "%MAVEN_HOME9%\bin\mvn.cmd" -B -f "%~dp0pom.xml" spring-boot:run %*
set "RC=%ERRORLEVEL%"
endlocal & exit /b %RC%
