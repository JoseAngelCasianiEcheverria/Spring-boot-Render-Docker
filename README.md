# Users Management — Spring Boot, Arquitectura Hexagonal y DDD

Aplicación de gestión de usuarios construida con Java 17 y Spring Boot. La API REST es el punto de entrada activo. El código de la antigua CLI se conserva como adaptador inactivo y no posee un contenedor de dependencias independiente.

Spring es el único *composition root*: `Main` inicia el contexto y las dependencias se resuelven mediante configuración y component scanning de Spring.

## Verificación

```bash
./mvnw clean test
./mvnw clean package
```

En Windows se puede utilizar `mvnw.cmd`.

## Configuración

La aplicación **no arranca sin secretos**. `JWT_SECRET` y `DB_PASSWORD` son obligatorios y no
tienen valor por defecto, a propósito: arrancar con una clave conocida sería peor que no arrancar.

```bash
copy .env.example .env    # Windows
cp .env.example .env      # Linux / macOS
```

Edita `.env` con tus valores. Para generar un `JWT_SECRET` válido (mínimo 32 bytes en Base64):

```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
```

`.env` está en `.gitignore` y no debe subirse.

## Primer arranque

`src/main/resources/schema.sql` crea el esquema pero **no siembra ningún usuario**: una
credencial de administrador versionada equivale a una credencial pública.

```bash
docker compose up -d mysql
```

Después crea tu administrador. `db/local-seed.sql` viene con un hash bcrypt ya generado
(está en `.gitignore`); si prefieres tu propia contraseña, genera el hash así y edita el
`INSERT`:

```powershell
# genera un hash bcrypt cost 12 para tu contraseña
$h = @"
System.out.println(at.favre.lib.crypto.bcrypt.BCrypt.withDefaults().hashToString(12, "TuContrasena".toCharArray()));
/exit
"@
$h | jshell --class-path "$env:USERPROFILE\.m2\repository\at\favre\lib\bcrypt\0.10.2\bcrypt-0.10.2.jar;$env:USERPROFILE\.m2\repository\at\favre\lib\bytes\1.5.0\bytes-1.5.0.jar"
```

```bash
docker exec -i spring-boot-arrieta-main-mysql-1 \
  mysql -uusers_app -p crud_usuarios < db/local-seed.sql
```

## Seguridad de la API

| Endpoint | Acceso |
| --- | --- |
| `POST /api/auth/register` | público, **siempre crea `MEMBER`** |
| `POST /api/auth/login` | público |
| `GET /api/users`, `GET /api/users/{id}` | `ADMIN` o `REVIEWER` |
| `POST /api/users` | `ADMIN` |
| `PUT`, `DELETE /api/users/{id}` | `ADMIN` |

El registro público no acepta `id` ni `role`: el identificador lo genera el servidor como UUID y
el rol se fija en `MEMBER`, de modo que un usuario anónimo no puede autoasignarse un rol
privilegiado. Los usuarios registrados quedan en estado `PENDING` y un `ADMIN` debe activarlos.

El puerto de MySQL se publica solo en `127.0.0.1`, no en todas las interfaces.

## Despliegue en Render

El archivo `render.yaml` define el servicio web, el build con Docker y el despliegue
automático de cada commit que llegue a la rama `main`.

1. Crear un Blueprint en Render y seleccionar este repositorio.
2. Completar en el panel los secretos marcados como requeridos: `DB_HOST`,
   `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `SMTP_USERNAME`, `SMTP_PASSWORD` y
   `SMTP_FROM_ADDRESS`.
3. Usar una instancia MySQL accesible desde Internet o desde la red privada de
   Render y ejecutar `src/main/resources/schema.sql` una vez para crear el esquema.
   Después crear el administrador con un `INSERT` propio: el seed del repositorio
   está excluido a propósito.
4. Desplegar el Blueprint. La API quedará disponible en el subdominio
   `onrender.com` asignado por Render y Swagger UI en `/swagger-ui.html`.

Las credenciales nunca deben guardarse en `application.properties` ni en
`render.yaml`. Para desarrollo local, deben proporcionarse como variables de entorno.
