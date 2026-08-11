@REM ============================================================
@REM  Maven Wrapper para srm-mcc-credit-assignment-api
@REM  Usa MAVEN_HOME se definido, senão o Maven do IntelliJ
@REM ============================================================
@echo off
setlocal

@REM --- Localiza o Maven ---
IF DEFINED MAVEN_HOME (
    SET "MVN_CMD=%MAVEN_HOME%\bin\mvn.cmd"
    GOTO :run
)

IF DEFINED M2_HOME (
    SET "MVN_CMD=%M2_HOME%\bin\mvn.cmd"
    GOTO :run
)

@REM --- Fallback: Maven embutido no IntelliJ IDEA ---
SET "IDEA_MVN=C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.0.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd"
IF EXIST "%IDEA_MVN%" (
    SET "MVN_CMD=%IDEA_MVN%"
    GOTO :run
)

@REM --- Tenta mvn do PATH ---
WHERE mvn >nul 2>&1
IF %ERRORLEVEL% EQU 0 (
    SET "MVN_CMD=mvn"
    GOTO :run
)

ECHO [ERRO] Maven nao encontrado. Configure MAVEN_HOME ou instale o Maven.
EXIT /B 1

:run
"%MVN_CMD%" -f "%~dp0pom.xml" %*
EXIT /B %ERRORLEVEL%

