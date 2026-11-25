# Ojo 👁️

Aplicación Android que **lee el texto de documentos físicos** (como Google Lens) y te deja **copiarlo**.

- 📷 Escanea con la cámara o elige una foto de la galería.
- 🔤 Detecta el texto sin necesidad de internet (Google ML Kit).
- 📋 Toca el texto para seleccionarlo y copiarlo, o cópialo todo.
- 🏷️ Reconoce automáticamente correos, teléfonos, fechas, CURP/RFC y montos.

## Descargar
👉 **https://djtechcool-create.github.io/ojo/** (página de descarga)

O directo: [ojo.apk](https://github.com/djtechcool-create/ojo/releases/latest/download/ojo.apk)

## Cómo se publica
Cada vez que se integra un cambio a `main`, GitHub Actions:
1. Corre las pruebas.
2. Compila el APK.
3. Crea un *Release* con `ojo.apk`.
4. Actualiza la página de descarga (carpeta `web/`).

## Estructura
- `app/src/main/java/com/ojo/scanner/ocr/`: reconocimiento de texto (ML Kit) y detector de datos.
- `app/src/main/java/com/ojo/scanner/ui/`: pantallas de cámara y resultado.
- `web/`: página de descarga (GitHub Pages).
