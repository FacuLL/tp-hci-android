 # Lumina — Instructivo de instalación

Aplicación móvil de gestión de casas inteligentes (Trabajo Práctico de HCI — Tercera Entrega, Grupo 1).

## 1. Requisitos del dispositivo

- Teléfono o tableta con **Android 10 (API 29) o superior**.
- Conexión a Internet (la aplicación se comunica con el API de Smart Home).

## 2. Dispositivos utilizados durante las pruebas

| Dispositivo | Tipo | Android / API Level |
|---|---|---|
| Pixel 7 (orientación vertical y horizontal) | Emulador | Android 15 (API 35) |

> La aplicación declara `minSdk = 29` y `targetSdk = 36`, por lo que funciona en cualquier dispositivo con Android 10 o superior.

## 3. Instalación del APK en un dispositivo físico

1. Copiar el archivo **`lumina.apk`** al teléfono (por cable USB, Google Drive o cualquier medio).
2. En el teléfono, abrir el archivo APK desde el explorador de archivos.
3. Si Android lo solicita, habilitar **"Instalar aplicaciones desconocidas"** para la aplicación desde la que se abre el APK (Ajustes → Aplicaciones → Acceso especial → Instalar aplicaciones desconocidas).
4. Confirmar la instalación. El APK está firmado con la clave del grupo, por lo que Play Protect puede pedir una confirmación adicional ("Instalar de todos modos").
5. Al abrir la aplicación por primera vez e iniciar sesión, **aceptar el permiso de notificaciones** para recibir los avisos de eventos del hogar (Android 13+ lo pide explícitamente).

## 4. Configuración de la conexión con el API

La aplicación viene preconfigurada para usar el servidor de la cátedra:

- **URL del API**: `https://hci.it.itba.edu.ar/api`
- **Clave de API**: la clave del grupo (ya cargada por defecto).

Para apuntar a una **instancia local del API** (por ejemplo, corriendo en una computadora de la misma red):

1. Iniciar sesión (o crear una cuenta) en la aplicación.
2. Ir a **Ajustes** (ícono de engranaje en la barra superior) → sección **"Conexión con el servidor"**.
3. Reemplazar la URL por la de la instancia local, con IP y puerto: por ejemplo `http://192.168.0.10:8080/api`.
4. Ajustar la clave de API si la instancia local usa otra.
5. Tocar **Guardar**. El cambio aplica de inmediato, sin reinstalar ni recompilar.
6. El botón **"Restaurar valores predeterminados"** vuelve a la configuración del servidor de la cátedra.

> La aplicación acepta conexiones HTTP sin TLS (texto plano) justamente para permitir instancias locales por IP y puerto.

## 5. Adecuaciones necesarias en el API

- No se requiere ninguna modificación del API: la aplicación usa los endpoints tal como están publicados.
- Para que lleguen los correos con los **códigos de verificación y recuperación de contraseña**, la instancia del API debe tener configurado su servidor de correo (configuración propia del API, externa a la aplicación).
- Las **rutinas** se crean desde la versión web de Lumina; la aplicación móvil las consulta y ejecuta (alcance definido por el enunciado).

## 6. Primeros pasos sugeridos

1. **Crear una cuenta** con un correo real (el código de verificación llega por email).
2. **Verificar la cuenta** con el código recibido.
3. **Iniciar sesión** (se puede dejar activada la opción "Mantener la sesión iniciada").
4. Desde Inicio, **crear un hogar** (opcionalmente invitando a otros usuarios registrados por email).
5. Crear **habitaciones** y **dispositivos**, y controlarlos desde el detalle de cada uno.
