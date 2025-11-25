@echo off
title Mail.cm Builder

echo Cleaning old build...
if exist classes (
    rd /S /Q classes
)
mkdir classes

cls

echo Compiling project...
javac -cp ".;sqlite-jdbc-3.51.0.0.jar" -d classes *.java

if %errorlevel% neq 0 (
    echo.
    echo ================================================
    echo  COMPILATION FAILED. Please review errors above.
    echo ================================================
    echo.
    pause
    exit /b
)

echo Compilation successful.
cls

echo.
echo.$$^\      $$^\           $$^\ $$^\     $$$$$$^\  $$^\      $$^\ 
echo.$$$^\    $$$ ^|          ^\__^|$$ ^|   $$  __$$^\ $$$^\    $$$ ^|
echo.$$$$^\  $$$$ ^| $$$$$$^\  $$^\ $$ ^|   $$ ^/  ^\__^|$$$$^\  $$$$ ^|
echo.$$^\$$^\$$ $$ ^| ^\____$$^\ $$ ^|$$ ^|   $$ ^|      $$^\$$^\$$ $$ ^|
echo.$$ ^|$$$  $$ ^| $$$$$$$ ^|$$ ^|$$ ^|   $$ ^|      $$ ^|$$$  $$ ^|
echo.$$ ^|^\$  ^/$$ ^|$$  __$$ ^|$$ ^|$$ ^|   $$ ^|  $$^\ $$ ^|^\$  ^/$$ ^|
echo.$$ ^| ^\_^/ $$ ^|^\$$$$$$$ ^|$$ ^|$$ ^|$$^\^\$$$$$$  ^|$$ ^| ^\_^/ $$ ^|
echo.^\__^|     ^\__^| ^\_______^|^\__^|^\__^|^\__^|^\______^/ ^\__^|     ^\__^|
echo.made by Code Monarch
echo.

echo ---------------------------------------------------
echo.
echo Running Mail.CM...
echo.

java -cp "classes;sqlite-jdbc-3.51.0.0.jar" EmailClient

echo.
echo ---------------------------------------------------
echo Application closed. Press any key to exit.
pause