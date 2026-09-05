@echo off
title PowerPoint Remote - Bluetooth Server
echo.
echo  ========================================
echo   PPT Remote - Servidor Bluetooth
echo  ========================================
echo.
echo  Instalando dependencia (solo primera vez)...
pip install keyboard >nul 2>&1
echo.
echo  IMPORTANTE:
echo  - Ejecuta este .bat como Administrador
echo  - Ten el Bluetooth del notebook ACTIVADO
echo  - Empareja primero el telefono con el notebook
echo.
echo  Iniciando servidor...
echo.
python ppt_server_bluetooth.py
pause
