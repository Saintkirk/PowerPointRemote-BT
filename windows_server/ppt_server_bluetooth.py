"""
PowerPoint Remote Server - Bluetooth (RFCOMM)
=============================================
Recibe comandos por Bluetooth desde la app Android y controla PowerPoint.

Requisitos:
    - Python 3.9 o superior (recomendado 3.10+)
    - pip install keyboard
    - Bluetooth activado en Windows
    - Notebook visible / emparejado con el teléfono

Uso:
    1. Empareja el teléfono con el notebook (Ajustes → Bluetooth)
    2. Abre PowerPoint con tu presentación
    3. Ejecuta este script (mejor como Administrador):
           python ppt_server_bluetooth.py
    4. En la app Android selecciona el notebook y pulsa Conectar

Comandos:
    NEXT  → Flecha derecha
    PREV  → Flecha izquierda
    START → F5
    END   → Esc
    BLACK → B
    WHITE → W
"""

import socket
import threading
import sys
import time

try:
    import keyboard
except ImportError:
    print("ERROR: Falta la librería 'keyboard'")
    print("Ejecuta:  pip install keyboard")
    sys.exit(1)

# Canal RFCOMM (1-30). 1 o 5 suelen funcionar bien.
RFCOMM_CHANNEL = 5

def execute_command(cmd: str):
    cmd = cmd.strip().upper()
    try:
        if cmd == "NEXT":
            keyboard.press_and_release("right")
        elif cmd == "PREV":
            keyboard.press_and_release("left")
        elif cmd == "START":
            keyboard.press_and_release("f5")
        elif cmd == "END":
            keyboard.press_and_release("esc")
        elif cmd == "BLACK":
            keyboard.press_and_release("b")
        elif cmd == "WHITE":
            keyboard.press_and_release("w")
        else:
            print(f"    Comando desconocido: {cmd}")
            return
        print(f"    → Ejecutado: {cmd}")
    except Exception as e:
        print(f"    Error al ejecutar tecla: {e}")


def handle_client(client_sock, address):
    print(f"[+] Cliente Bluetooth conectado: {address}")
    try:
        buffer = ""
        while True:
            data = client_sock.recv(1024)
            if not data:
                break
            buffer += data.decode("utf-8", errors="ignore")
            while "\n" in buffer:
                line, buffer = buffer.split("\n", 1)
                if line.strip():
                    execute_command(line)
    except Exception as e:
        print(f"[!] Error con el cliente: {e}")
    finally:
        try:
            client_sock.close()
        except:
            pass
        print(f"[-] Cliente desconectado: {address}")


def main():
    print("=" * 55)
    print("  PowerPoint Remote Server - Bluetooth RFCOMM")
    print("=" * 55)
    print()
    print("  1. Asegúrate de que el Bluetooth del notebook esté ACTIVADO")
    print("  2. Empareja el teléfono con este notebook")
    print("  3. Abre PowerPoint")
    print("  4. En la app Android selecciona este PC y Conecta")
    print()
    print(f"  Escuchando en canal RFCOMM {RFCOMM_CHANNEL} ...")
    print("  (Ctrl+C para salir)")
    print("=" * 55)

    server_sock = None
    try:
        # Socket Bluetooth nativo (disponible desde Python 3.9 en Windows)
        server_sock = socket.socket(
            socket.AF_BLUETOOTH,
            socket.SOCK_STREAM,
            socket.BTPROTO_RFCOMM
        )
        server_sock.bind(("", RFCOMM_CHANNEL))  # "" = cualquier adaptador
        server_sock.listen(1)

        while True:
            print("\nEsperando conexión Bluetooth...")
            client_sock, address = server_sock.accept()
            # Un solo cliente a la vez es suficiente
            handle_client(client_sock, address)

    except OSError as e:
        print("\n[ERROR] No se pudo abrir el socket Bluetooth.")
        print(f"Detalle: {e}")
        print()
        print("Posibles causas:")
        print("  • Python menor a 3.9 (necesitas 3.9+)")
        print("  • Bluetooth apagado o no disponible")
        print("  • El canal RFCOMM está ocupado (prueba cambiar RFCOMM_CHANNEL)")
        print("  • Ejecuta el script como Administrador")
        sys.exit(1)
    except KeyboardInterrupt:
        print("\nServidor detenido.")
    finally:
        if server_sock:
            try:
                server_sock.close()
            except:
                pass


if __name__ == "__main__":
    main()
