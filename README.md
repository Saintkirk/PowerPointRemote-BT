# PPT Remote BT – Control de PowerPoint por Bluetooth

App Android + servidor Windows que controla PowerPoint **solo con Bluetooth** (sin WiFi).

---

## Requisitos

### Notebook (Windows 10/11)
- Python **3.9 o superior** (muy importante)
- Bluetooth activado
- `pip install keyboard`

### Teléfono
- Android 7.0 o superior
- Bluetooth activado

---

## Paso a paso

### 1. Emparejar los dispositivos (importante)

1. En el **notebook**: Ajustes → Bluetooth → Activar y hacer visible.
2. En el **teléfono**: Ajustes → Bluetooth → Buscar dispositivos → Empareja con el notebook.
3. Acepta el emparejamiento en ambos lados.

### 2. Servidor en el notebook

1. Descomprime el ZIP.
2. Ve a la carpeta `windows_server`.
3. **Clic derecho** en `INICIAR_SERVIDOR_BLUETOOTH.bat` → **Ejecutar como administrador**.
4. Debe aparecer el mensaje “Esperando conexión Bluetooth…”.

> Ejecutar como Administrador es importante para que las teclas funcionen cuando PowerPoint está en pantalla completa.

### 3. Generar la APK

1. Instala **Android Studio**: https://developer.android.com/studio
2. Abre Android Studio → **Open** → selecciona la carpeta `PowerPointRemote`
3. Espera a que sincronice Gradle.
4. Menú: **Build → Build Bundle(s) / APK(s) → Build APK(s)**
5. Cuando termine, haz clic en **locate**. El APK estará en:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```
6. Copia el APK al teléfono e instálalo (permite “orígenes desconocidos”).

### 4. Usar la app

1. Abre **PPT Remote BT**.
2. Concede los permisos de Bluetooth que pida.
3. En el listado selecciona tu **notebook**.
4. Pulsa **Conectar**.
5. ¡Listo! Usa los botones grandes.

---

## Comandos que envía la app

| Botón              | Tecla que simula |
|--------------------|------------------|
| Siguiente          | → (flecha derecha) |
| Anterior           | ← (flecha izquierda) |
| Iniciar (F5)       | F5               |
| Finalizar (Esc)    | Esc              |
| Pantalla Negra     | B                |
| Pantalla Blanca    | W                |

---

## Solución de problemas

| Problema | Qué hacer |
|---------|----------|
| “No se pudo abrir el socket Bluetooth” | Usa Python 3.9 o superior. Verifica con `python --version` |
| No aparece el notebook en la lista | Empareja primero desde Ajustes del teléfono |
| Conecta pero no cambia de diapositiva | Ejecuta el .bat **como Administrador** |
| Error de conexión | Reinicia Bluetooth en ambos dispositivos y vuelve a emparejar |
| Canal ocupado | Edita `RFCOMM_CHANNEL = 5` en el .py y prueba con 1 o 3 |

---

## Archivos incluidos

```
PowerPointRemote/
├── app/                          ← Código fuente Android (Bluetooth)
├── windows_server/
│   ├── ppt_server_bluetooth.py   ← Servidor Bluetooth
│   ├── INICIAR_SERVIDOR_BLUETOOTH.bat
│   └── requirements.txt
└── README.md
```

---

**Nota técnica:** Se usa el perfil Serial Port (SPP) con UUID `00001101-0000-1000-8000-00805F9B34FB` y canal RFCOMM 5. Funciona con la pila Bluetooth nativa de Windows + Python 3.9+.
