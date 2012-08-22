@echo off

setlocal

if exist "%FIPER_HOME%\sdk\sdk.jar" goto SKIPD
call "%~dp0%\..\..\..\bin\win64\fiperenv.bat"
if exist "%FIPER_HOME%\sdk\sdk.jar" goto CONTINUE

:SKIPD

if NOT exist "%FIPER_HOME%\sdk\sdk.jar" goto FAILED
call "%FIPER_HOME%\bin\win64\fiperenv.bat" 
goto CONTINUE

:FAILED
echo "FIPER_HOME must point to a valid Isight installation"
exit 2

:CONTINUE

set FiperLibList=.;%FiperJars%;lib\gui.jar;lib\dev-visual-util.jar;lib\dashboard.jar;lib\toolgui.jar;lib\frapi.jar;lib\vdd.jar;lib\mdolTranslator.jar;lib\correlationmap.jar

rem --- Set up and launch
set LaunchClasspath=%FiperJreLib%;%FiperLibList%;%EJBLib%;.;D:\Working\hy-addon\isight\application\hello\out\production\hello
REM set LaunchPgm=%1
set LaunchPgm=org.huys.isight.MainProgram
SHIFT
set LaunchArgs="%1 %2 %3 %4 %5 %6 %7 %8 %9"
call "%FIPER_HOME%\bin\win64\launch.bat"

@pause
