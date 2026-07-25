@rem Gradle startup script for Windows

@if "%DEBUG%"=="" @echo off
@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set APP_HOME=%~dp0

set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

@rem Execute Gradle
java %JAVA_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

@if "%ERRORLEVEL%"=="0" goto mainEnd
:fail
exit /b 1
:mainEnd
if "%ERRORLEVEL%"=="0" if not "%GRADLE_TERMINATE_CMD%"=="" (%GRADLE_TERMINATE_CMD%)
:omega
