@echo off
title Asistencia - Representante

:MENU
cls
echo ==========================================
echo       ASISTENCIA - REPRESENTANTE
echo ==========================================
echo.
echo Seleccione una respuesta:
echo.
echo 1 - Hola, en que puedo ayudarte?
echo 2 - Te encontras en una situacion de emergencia?
echo 3 - Por favor, indicame tu ubicacion.
echo 4 - Mantenete alejado de la zona afectada.
echo 5 - Comunicate con los servicios de emergencia.
echo 6 - Gracias por comunicarte con asistencia.
echo 0 - Salir
echo.
set /p opcion=Opcion:

if "%opcion%"=="1" set "respuesta=Hola, en que puedo ayudarte?" & goto ENVIAR
if "%opcion%"=="2" set "respuesta=Te encontras en una situacion de emergencia?" & goto ENVIAR
if "%opcion%"=="3" set "respuesta=Por favor, indicame tu ubicacion." & goto ENVIAR
if "%opcion%"=="4" set "respuesta=Mantenete alejado de la zona afectada." & goto ENVIAR
if "%opcion%"=="5" set "respuesta=Comunicate con los servicios de emergencia." & goto ENVIAR
if "%opcion%"=="6" set "respuesta=Gracias por comunicarte con asistencia." & goto ENVIAR
if "%opcion%"=="0" goto FIN

echo.
echo Opcion no valida.
pause
goto MENU

:ENVIAR
cls
echo ==========================================
echo       ASISTENCIA - REPRESENTANTE
echo ==========================================
echo.
echo Respuesta:
echo %respuesta%
echo.
echo Enviando respuesta...
echo.

REM Crear JSON temporal
(
echo {"emisor":"representante","texto":"%respuesta%","timestamp":%RANDOM%}
) > respuesta.json

REM Enviar JSON a Firebase
curl -X POST -H "Content-Type: application/json" --data-binary "@respuesta.json" "https://alertatemprana-47c0e-default-rtdb.firebaseio.com/mensajes.json"

if errorlevel 1 (
echo.
echo ERROR: No se pudo enviar la respuesta.
) else (
echo.
echo ==========================================
echo   Respuesta enviada correctamente.
echo ==========================================
)

del respuesta.json

echo.
pause
goto MENU

:FIN
cls
echo Cerrando asistencia...
timeout /t 2 >nul

