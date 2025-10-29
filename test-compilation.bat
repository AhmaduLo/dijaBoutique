@echo off
echo ========================================
echo TEST DE COMPILATION
echo ========================================
echo.

echo Nettoyage du projet...
call mvnw.cmd clean

echo.
echo Compilation...
call mvnw.cmd compile -DskipTests

echo.
echo ========================================
echo Si vous voyez "BUILD SUCCESS", c'est bon !
echo ========================================
pause
