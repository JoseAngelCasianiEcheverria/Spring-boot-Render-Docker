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

## Canal de correo

Hay dos adaptadores para `EmailSenderPort`, y se elige con `SPRING_PROFILES_ACTIVE`:

| Perfil | Adaptador | Transporte | Dónde funciona |
| --- | --- | --- | --- |
| _(ninguno, por defecto)_ | `JavaMailEmailSenderAdapter` | SMTP 25/465/587 | local |
| `brevo` | `BrevoRestEmailSenderAdapter` | HTTPS 443 | **Render** |

El motivo no es de diseño sino de plataforma: **el plan gratuito de Render bloquea el SMTP
saliente**, así que el adaptador de JavaMail no puede entregar desde allí sin importar qué
credenciales se configuren. La API REST de Brevo viaja por el 443 y sí pasa.

Se separan con perfiles excluyentes (`!brevo` y `brevo`), igual que los dos repositorios de
persistencia. Sin eso, Spring encontraría dos candidatos para el mismo puerto y no sabría cuál
inyectar.

```powershell
# Brevo por REST (produccion)
$env:SPRING_PROFILES_ACTIVE = "brevo"
$env:BREVO_API_KEY = "xkeysib-..."            # la de API, NO la xsmtpsib- de SMTP
$env:BREVO_FROM_ADDRESS = "remitente@ejemplo.com"
.\mvnw.cmd spring-boot:run
```

Tres errores que cuestan tiempo:

- **`BREVO_API_KEY` es la clave `xkeysib-`, no la `xsmtpsib-`.** Son credenciales distintas: cada
  una solo sirve para su canal.
- **`BREVO_FROM_ADDRESS` debe ser un remitente verificado en Brevo**, nunca el login
  `...@smtp-brevo.com`.
- **Falla al arrancar, no al enviar.** Si falta `BREVO_API_KEY`, `BrevoSpringConfig` lanza
  `ConfigurationException` en el arranque. Preferible a arrancar y descubrir el problema en cada
  envío como un 401 sin explicación.

Para probar el relay SMTP desde local:

```powershell
& "$env:USERPROFILE\.local\bin\smtp-probe.ps1" -Para "tu@correo.com"
```

## Motores de base de datos

La aplicación habla con dos motores según el perfil activo, que se elige con
`SPRING_PROFILES_ACTIVE`:

| Perfil | Driver | Repositorio | Configuración |
| --- | --- | --- | --- |
| _(ninguno, por defecto)_ | MySQL | `UserRepositoryMySQL` | `DataSourceSpringConfig` |
| `postgres` | PostgreSQL | `UserRepositoryPostgres` | `PostgresDataSourceSpringConfig` |

Ambos declaran `@Profile` excluyentes (`"postgres"` y `"!postgres"`). Sin eso los dos
repositorios implementarían los mismos puertos y Spring no sabría cuál inyectar.

Ambos leen las mismas claves `db.*`, así que cambiar de motor no obliga a tocar la configuración
de despliegue. La única clave propia de PostgreSQL es `db.postgres.sslmode`, que **no** es
intercambiable con `db.ssl-mode`: su vocabulario es `disable|allow|prefer|require` en minúsculas,
 mientras que el de MySQL es `DISABLED|PREFERRED|REQUIRED`.

### TiDB Cloud usa el adaptador de MySQL

TiDB es compatible con el protocolo MySQL, así que en producción **no** hace falta ningún perfil:
se conecta por el adaptador por defecto. Lo único que cambia respecto a un MySQL local es el TLS.

| Variable | MySQL local | TiDB Cloud Starter |
| --- | --- | --- |
| `DB_HOST` | `mysql` | `gateway-xxxxxx.<region>.aws.tidbcloud.com` |
| `DB_PORT` | `3306` | `4000` |
| `DB_SSL_MODE` | `DISABLED` | `VERIFY_IDENTITY` |

`VERIFY_IDENTITY` es obligatorio en TiDB: no acepta conexiones sin cifrar. A diferencia de un
MySQL con certificado propio, aquí **no hace falta truststore** — TiDB emite el certificado con
Let's Encrypt, y esa raíz ya está en el `cacerts` del JDK.

### Diferencias de SQL entre adaptadores

| MySQL | PostgreSQL | Motivo |
| --- | --- | --- |
| `NOW()` | `CURRENT_TIMESTAMP` | `NOW()` no existe en PostgreSQL |
| `ENUM('ADMIN',...)` | `VARCHAR(20) + CHECK` | no hay ENUM |
| `ON UPDATE CURRENT_TIMESTAMP` | valor explícito en el `UPDATE` | no existe el trigger automático |

`SELECT` y `DELETE` son idénticos: la sintaxis ANSI coincide en los dos motores.

### Usar PostgreSQL

```powershell
# 1. esquema
docker run -d --name pg-test -e POSTGRES_PASSWORD=test -p 5432:5432 postgres:16
Get-Content src/main/resources/schema-postgres.sql -Raw |
  docker exec -i pg-test psql -U postgres -d postgres

# 2. arrancar la API contra el
$env:SPRING_PROFILES_ACTIVE = "postgres"
$env:DB_HOST = "localhost"; $env:DB_PORT = "5432"; $env:DB_NAME = "postgres"
$env:DB_USERNAME = "postgres"; $env:DB_PASSWORD = "test"
$env:DB_POSTGRES_SSLMODE = "disable"   # local sin TLS; Supabase exige "require"
.\mvnw.cmd spring-boot:run
```

`DB_POSTGRES_SSLMODE` solo hay que cambiarlo en local: el default es `require`, que es lo que
pide Supabase. Para eliminar el contenedor de pruebas: `docker rm -f pg-test`.

## Despliegue en Render

El archivo `render.yaml` define el servicio web, el build con Docker y el despliegue
automático de cada commit que llegue a la rama `main`.

1. Crear un Blueprint en Render y seleccionar este repositorio.
2. Completar en el panel los secretos marcados como requeridos: `DB_HOST`,
   `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `SMTP_USERNAME`, `SMTP_PASSWORD` y
   `SMTP_FROM_ADDRESS`.
3. Crear un cluster **TiDB Cloud Starter** (plan $0, compatible con MySQL), generar la contraseña
   del usuario `root` en el diálogo *Connect* y ejecutar `src/main/resources/schema.sql` una vez
   en el SQL Editor para crear `crud_usuarios` y la tabla `users`. Después crear el
   administrador con un `INSERT` propio: el seed del repositorio está excluido a propósito.
4. Desplegar el Blueprint. La API quedará disponible en el subdominio
   `onrender.com` asignado por Render y Swagger UI en `/swagger-ui.html`.

Las credenciales nunca deben guardarse en `application.properties` ni en
`render.yaml`. Para desarrollo local, deben proporcionarse como variables de entorno.
