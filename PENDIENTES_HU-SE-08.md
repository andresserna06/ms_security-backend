# Criterios Pendientes y Hoja de Ruta - HU-SE-08 (PENDIENTES_HU-SE-08.md)

Este documento detalla minuciosamente los requisitos que faltan para dar por completada la historia de usuario **HU-SE-08: Autenticarse y gestionar sesión**, contrastando los criterios de aceptación oficiales con el estado actual del código y estableciendo el plan de acción técnico.

---

## 1. Matriz de Criterios de Aceptación vs. Estado Actual

| # | Criterio de Aceptación Oficial | Estado Actual | Qué Falta Exactamente |
|---|--------------------------------|---------------|-----------------------|
| **1** | Ofrecer dos mecanismos de autenticación: correo/contraseña y OAuth. | ⚠️ Parcial | Correo/contraseña implementado completamente con 2FA en Backend y Frontend; OAuth está pendiente. |
| **2** | Proveedores OAuth contemplados: Google, GitHub y otro (Apple/Microsoft). | ❌ Pendiente | Backend no cuenta con endpoints de integración OAuth ni validación de tokens de proveedores externos. En Frontend solo hay botones estáticos sin interactividad. |
| **3** | Para correo y contraseña, debe existir cuenta previa registrada. | ✅ Cumplido | Implementado en `AuthService.login` buscando en `UserRepository`. |
| **4** | Validar que las credenciales correspondan a un usuario registrado y habilitado. | ✅ Cumplido | Valida existencia, correspondencia con BCrypt y rechaza con `UNAUTHORIZED` si `user.getActive()` es falso. |
| **5** | Si credenciales son incorrectas, impedir el acceso. | ✅ Cumplido | Lanza `ApplicationException(ErrorCase.UNAUTHORIZED)` con código HTTP 401. |
| **6** | Tras validar correo y contraseña se solicita el segundo factor; la sesión no queda autenticada hasta validarlo. | ✅ Cumplido | Flujo 2FA completo: Backend genera y valida código de 6 dígitos que envía por correo vía `ms_notificaciones`. Frontend implementa pantalla interactiva para digitar el código y verificarlo. |
| **7** | En OAuth, validación de identidad realizada por proveedor externo. | ❌ Pendiente | Implementar recepción y validación del token de identidad (`id_token` o intercambio de código) de Google, GitHub y el tercer proveedor. |
| **8** | Usuario OAuth no debe proporcionar contraseña propia de la plataforma. | ❌ Pendiente | La entidad `User` exige `password` como `nullable = false`. Se debe permitir valor nulo o manejar indicador de cuenta externa. |
| **9** | Usuario OAuth no debe requerir 2FA de la plataforma. | ❌ Pendiente | El flujo OAuth debe emitir el JWT y la sesión directamente tras validar el proveedor externo. |
| **10** | Si persona entra por primera vez por OAuth y no existe, crear automáticamente su usuario. | ❌ Pendiente | Servicio de aprovisionamiento automático de usuario (nombre, correo, rol por defecto, `active = true`, proveedor OAuth). |
| **11** | Si el correo ya fue usado en autenticación básica y se intenta usar OAuth, rechazar inicio de sesión. | ❌ Pendiente | Agregar campo `authProvider` (`LOCAL`, `GOOGLE`, `GITHUB`, etc.) en `User`. Si existe usuario con `authProvider == LOCAL` y se intenta entrar con OAuth, arrojar excepción de rechazo. |
| **12** | En posteriores accesos OAuth con el mismo proveedor, identificar usuario sin duplicar. | ❌ Pendiente | Lógica de consulta por correo y proveedor; si coincide, iniciar sesión y emitir sesión interna. |
| **13** | Evitar creación de usuarios duplicados. | ⚠️ Parcial | `email` tiene restricción única en BD, pero falta el control de negocio en el flujo OAuth. |
| **14** | Independientemente del mecanismo, generar sesión interna asociada con el usuario. | ⚠️ Parcial | Cumplido para el flujo con 2FA; pendiente integrarlo para OAuth. |
| **15** | Cada sesión almacena identificador/token y fecha de expiración. | ✅ Cumplido | Entidad `Session` cuenta con campos `token` y `expiration`. |
| **16** | La sesión debe permitir identificar al usuario autenticado. | ✅ Cumplido | Relación `@ManyToOne` entre `Session` y `User`. |
| **17** | Una sesión expirada no debe permitir realizar nuevas operaciones protegidas. | ✅ Cumplido | Spring Security valida la expiración del JWT (15 min) y al expirar o hacer logout la sesión se invalida en BD. |
| **18** | Cierre de sesión (Logout): token asociado deja de ser válido. | ✅ Cumplido | Endpoint `POST /api/auth/logout` implementado en Backend y activado en Frontend (`authService.logout()` en `AppHeaderDropdown.jsx`), invalidando la sesión en base de datos. |
| **19** | Usuario puede iniciar nueva sesión tras expirar o cerrar la anterior. | ✅ Cumplido | El flujo permite generar nuevas sesiones consecutivas. |
| **20** | Acceso a funcionalidades según roles y permisos del usuario. | ✅ Cumplido | `JwtService` extrae roles y permisos (`method:url`) del usuario y los inyecta como claims `"roles"` y `"permissions"` dentro del JWT. |


---

## 2. Hoja de Ruta de Implementación Detallada

### ✅ Fase 1: Autenticación Básica, 2FA, Sesión y Roles (COMPLETADA)
1. **Validación de Usuario Habilitado:**
   - En `AuthService.login` y `verify2Fa`, se valida `user.getActive()`. Si está inactivo, lanza `ApplicationException(ErrorCase.UNAUTHORIZED, "El usuario se encuentra inhabilitado")`.
2. **Flujo Obligatorio de 2FA por Correo:**
   - Al validar credenciales con BCrypt, se genera un código seguro de 6 dígitos con expiración de 5 minutos y se despacha al correo a través de `ms_notificaciones`.
   - `POST /api/auth/verify-2fa` valida coincidencia, tiempo de vida y marca `isCodeUsed=true` para evitar reutilización antes de emitir el JWT.
3. **Inclusión de Roles y Permisos en JWT (`JwtService`):**
   - Se cargan las relaciones de usuario con `@EntityGraph` (`findWithRolesAndPermissionsByEmail`).
   - Se inyectan los claims `"roles"` (lista de roles) y `"permissions"` (`METHOD:URL`) dentro del JWT para control de acceso.
4. **Endpoint y Mecanismo de Cierre de Sesión (Logout):**
   - Creado `POST /api/auth/logout` en `AuthController`.
   - Invalida la sesión en la base de datos (`SessionRepository.findByToken`) estableciendo su expiración al momento actual.
5. **Implementación en Frontend (`Frontend`):**
   - Servicio centralizado `src/services/authService.js` con métodos `login`, `verify2Fa`, `logout` y persistencia en `localStorage`.
   - Interceptor en `src/services/apiClient.js` que inyecta automáticamente `Authorization: Bearer <token>` en todas las peticiones HTTP.
   - Vista interactiva en `Login.jsx` con transición entre el formulario de credenciales y la pantalla de verificación 2FA con input numérico formateado.
   - Botón interactivo de "Cerrar Sesión" en `AppHeaderDropdown.jsx`.

---

### ⏳ Fase 2: Módulo OAuth 2.0 en Backend (PENDIENTE)
1. **Modelo de Datos para Proveedores:**
   - Crear enumeración `AuthProvider { LOCAL, GOOGLE, GITHUB, APPLE }`.
   - Agregar campo `authProvider` en entidad `User`.
   - Modificar columna `password` en `User` para que admita `nullable = true` (usuarios externos no poseen contraseña de la plataforma).
2. **Lógica de Validación y Aprovisionamiento OAuth:**
   - Crear DTOs: `OAuthLoginRequestDTO` (proveedor, token de identidad o código externo).
   - Crear servicio `OAuthService` o extender `AuthService`:
     - Validar la identidad con el proveedor externo seleccionado (Google, GitHub, Apple/Microsoft).
     - Extraer email y datos del perfil del proveedor.
     - **Regla de conflicto obligatoria:** Si existe un usuario con ese correo y `authProvider == AuthProvider.LOCAL`, rechazar el inicio de sesión con error de negocio: *"El correo ya fue registrado mediante autenticación directa. Inicie sesión con sus credenciales."*.
     - **Primer acceso:** Si el usuario no existe, crearlo automáticamente (`active = true`, proveedor OAuth, rol por defecto, sin contraseña).
     - **Accesos posteriores:** Si ya existe con el mismo proveedor, identificarlo y emitir sesión interna sin generar un registro duplicado.
     - **Omitir 2FA:** La autenticación OAuth emite directamente el JWT y la sesión sin requerir el 2FA de la plataforma.

---

### ⏳ Fase 3: Integración de OAuth en Frontend (PENDIENTE)
1. **Botones de Proveedores OAuth Funcionales en `Login.jsx`:**
   - Conectar los botones de Google y GitHub para abrir el flujo/popup de autenticación del proveedor (usando Google OAuth SDK o redirección).
   - Enviar el token externo recibido al endpoint de backend.
   - Almacenar la sesión interna emitida y navegar al Dashboard.

---

## 3. Resumen de Archivos a Intervenir para OAuth (Sujeto a Autorización)

- **Backend:**
  - `Backend/ms_security/src/main/java/com/uc/ms_security/entity/User.java` (campo `authProvider`, `password` nullable).
  - `Backend/ms_security/src/main/java/com/uc/ms_security/service/AuthService.java` (o nuevo `OAuthService`).
  - `Backend/ms_security/src/main/java/com/uc/ms_security/controller/AuthController.java` (endpoint `POST /api/auth/oauth`).
- **Frontend:**
  - `Frontend/src/services/authService.js` (método `loginOAuth`).
  - `Frontend/src/views/authentication/login/Login.jsx` (interactividad de botones OAuth).

