```bat
@echo off
title Simulador de Catastrofes - Alerta Temprana

REM ==========================================
REM CONFIGURACION FIREBASE
REM ==========================================

set PROJECT_ID=alertatemprana-47c0e

REM IDs de las alertas
set ID_TERREMOTO=Frpc18ooKRns4LYiKgZN
set ID_INUNDACION=M3wLisTJabSnqBToUfYV
set ID_INCENDIO=wZAxfEvSL2URjpedY4wn


:MENU
cls
echo ==========================================
echo       SIMULADOR DE CATASTROFES
echo ==========================================
echo.
echo Seleccione una accion:
echo.
echo 1 - Activar terremoto
echo 2 - Desactivar terremoto
echo 3 - Activar inundacion
echo 4 - Desactivar inundacion
echo 5 - Activar incendio forestal
echo 6 - Desactivar incendio forestal
echo 7 - Desactivar todas las alertas
echo 0 - Salir
echo.
set /p opcion=Opcion:


if "%opcion%"=="1" (
    set "tipo=terremoto"
    set "id=%ID_TERREMOTO%"
    set "estado=true"
    goto ACTUALIZAR
)

if "%opcion%"=="2" (
    set "tipo=terremoto"
    set "id=%ID_TERREMOTO%"
    set "estado=false"
    goto ACTUALIZAR
)

if "%opcion%"=="3" (
    set "tipo=inundacion"
    set "id=%ID_INUNDACION%"
    set "estado=true"
    goto ACTUALIZAR
)

if "%opcion%"=="4" (
    set "tipo=inundacion"
    set "id=%ID_INUNDACION%"
    set "estado=false"
    goto ACTUALIZAR
)

if "%opcion%"=="5" (
    set "tipo=incendio forestal"
    set "id=%ID_INCENDIO%"
    set "estado=true"
    goto ACTUALIZAR
)

if "%opcion%"=="6" (
    set "tipo=incendio forestal"
    set "id=%ID_INCENDIO%"
    set "estado=false"
    goto ACTUALIZAR
)

if "%opcion%"=="7" goto DESACTIVAR_TODAS

if "%opcion%"=="0" goto FIN


echo.
echo Opcion no valida.
pause
goto MENU


:ACTUALIZAR
cls
echo ==========================================
echo       SIMULADOR DE CATASTROFES
echo ==========================================
echo.
echo Catastrofe: %tipo%
echo.

if "%estado%"=="true" (
    echo Estado: ACTIVADA
) else (
    echo Estado: DESACTIVADA
)

echo.
echo Actualizando Firebase...
echo.

curl -s -X PATCH ^
-H "Content-Type: application/json" ^
--data "{\"fields\":{\"estado\":{\"booleanValue\":%estado%}}}" ^
"https://firestore.googleapis.com/v1/projects/%PROJECT_ID%/databases/(default)/documents/alertas/%id%?updateMask.fieldPaths=estado"

if errorlevel 1 (
    echo.
    echo ==========================================
    echo   ERROR: No se pudo actualizar la alerta.
    echo ==========================================
) else (
    echo.
    echo ==========================================
    echo   Alerta actualizada correctamente.
    echo ==========================================
)

echo.
pause
goto MENU


:DESACTIVAR_TODAS
cls
echo ==========================================
echo       DESACTIVAR TODAS LAS ALERTAS
echo ==========================================
echo.
echo Desactivando terremoto...

call :DESACTIVAR "%ID_TERREMOTO%"

echo Desactivando inundacion...

call :DESACTIVAR "%ID_INUNDACION%"

echo Desactivando incendio forestal...

call :DESACTIVAR "%ID_INCENDIO%"

echo.
echo ==========================================
echo   TODAS LAS ALERTAS ESTAN DESACTIVADAS
echo ==========================================
echo.
pause
goto MENU


:DESACTIVAR
curl -s -X PATCH ^
-H "Content-Type: application/json" ^
--data "{\"fields\":{\"estado\":{\"booleanValue\":false}}}" ^
"https://firestore.googleapis.com/v1/projects/%PROJECT_ID%/databases/(default)/documents/alertas/%~1?updateMask.fieldPaths=estado" >nul

exit /b


:FIN
cls
echo ==========================================
echo       SIMULADOR DE CATASTROFES
echo ==========================================
echo.
echo Desactivando todas las alertas...

call :DESACTIVAR "%ID_TERREMOTO%"
call :DESACTIVAR "%ID_INUNDACION%"
call :DESACTIVAR "%ID_INCENDIO%"

echo.
echo Todas las alertas fueron desactivadas.
echo.
echo Cerrando simulador...

timeout /t 2 >nul

endlocal
exit
```
