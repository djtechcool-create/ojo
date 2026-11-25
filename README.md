# Ojo 👁️

Aplicación Android que **lee el texto de documentos físicos** (como Google Lens) y te deja **copiarlo**.

- ⚡ Reconoce texto **en vivo** mientras apuntas la cámara (sin tomar foto), como Google Lens.
- 👆 Toca un texto en pantalla y se copia al instante; "Seleccionar texto" congela la imagen para elegir varias líneas.
- ▮▮ Lee códigos de barras y QR (p. ej. la clave de acceso de facturas y guías del SRI).
- 🏷️ Detecta RUC, clave de acceso (49 dígitos), No. de comprobante, fechas, correos, teléfonos y montos.
- 🔤 Funciona sin internet (Google ML Kit). También acepta fotos de la galería.

## Descargar
👉 **https://djtechcool-create.github.io/ojo/** (página de descarga)

O directo: [ojo.apk](https://github.com/djtechcool-create/ojo/releases/latest/download/ojo.apk)

## Cómo se publica
Cada vez que se integra un cambio a `main`, GitHub Actions:
1. Corre las pruebas.
2. Compila el APK.
3. Crea un *Release* con `ojo.apk`.
4. Actualiza la página de descarga: copia la carpeta `web/` a la rama `gh-pages`.

## Estructura
- `app/src/main/java/com/ojo/scanner/ocr/`: reconocimiento de texto (ML Kit) y detector de datos.
- `app/src/main/java/com/ojo/scanner/ui/`: pantallas de cámara y resultado.
- `web/`: página de descarga (GitHub Pages).
