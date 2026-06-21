@echo off
rem Build rdzenia Spring (server/) - JDK 21 + Maven 3.9.x. Bez argumentow: clean package.
setlocal
if "%JAVA_HOME21%"=="" (echo [ERROR] Brak zmiennej JAVA_HOME21 ^(JDK 21^)& exit /b 1)
if "%MAVEN_HOME9%"=="" (echo [ERROR] Brak zmiennej MAVEN_HOME9 ^(Maven 3.9.x^)& exit /b 1)
set "JAVA_HOME=%JAVA_HOME21%"
set "GOALS=%*"
if "%GOALS%"=="" set "GOALS=clean package"
call "%MAVEN_HOME9%\bin\mvn.cmd" -B -f "%~dp0pom.xml" %GOALS%
set "RC=%ERRORLEVEL%"
endlocal & exit /b %RC%
