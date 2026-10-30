# Memoria del Proyecto y Estado de la HU-SE-08 (AGENTE.md)

Este documento centraliza la memoria técnica del proyecto, su arquitectura, el contexto del equipo, y el estado exacto de desarrollo con respecto a la historia de usuario **HU-SE-08: Autenticarse y gestionar sesión**.

---

## 1. Contexto General del Proyecto

- **Nombre del Sistema:** JPA Sports (Plataforma Deportiva).
- **Módulo Actual:** Microservicio de Seguridad y Control de Acceso (`ms_seguridad`).
- **Arquitectura del Ecosistema:**
  1. **Frontend:** React 19 + Vite + CoreUI / Bootstrap 5 (Puerto `3000`).
  2. **Backend de Seguridad (`ms_security`):** Java 17 + Spring Boot 3 + Spring Data JPA + Spring Security 6 + MySQL (Puerto `8181`).
  3. **Microservicio de Notificaciones (`ms_notificaciones`):** FastAPI + Python (Puerto `8000`) para envío de correos vía SMTP.

---

## 2. Historia de Usuario en Curso

### **HU-SE-08 – Autenticarse y gestionar sesión**

> **Como** usuario,  
> **quiero** autenticarme mediante credenciales o utilizando un proveedor externo de identidad,  
> **para** acceder de manera segura a las funcionalidades que tengo autorizadas en la plataforma.

### Criterios de Aceptación Definidos:
1. Ofrecer dos mecanismos de autenticación:
   - Correo electrónico y contraseña.
   - Autenticación OAuth mediante proveedores externos (Google, GitHub y otro proveedor como Apple o Microsoft).
2. Para autenticación con correo/contraseña, debe existir previamente la cuenta y validarse credenciales contra usuario registrado y habilitado.
3. Si las credenciales son incorrectas o el usuario está inhabilitado, impedir el acceso.
4. Si el usuario tiene habilitada la autenticación de dos factores (2FA), tras validar correo y contraseña se debe solicitar el segundo factor. La sesión no queda autenticada hasta validar dicho factor.
5. Para OAuth, la validación la realiza el proveedor externo; el usuario no ingresa contraseña propia de la plataforma ni requiere el 2FA de la plataforma.
6. Si una persona se autentica por primera vez vía OAuth, se debe crear automáticamente su usuario.
7. Si el correo electrónico ya fue registrado previamente mediante autenticación básica y se intenta usar OAuth, se debe rechazar el inicio de sesión.
8. En accesos posteriores con el mismo proveedor OAuth, identificar al usuario existente sin duplicar registros.
9. En cualquier caso, al completar el acceso exitoso, generar una sesión interna con identificador/token, fecha de expiración y vinculación al usuario.
10. Una sesión expirada no debe permitir operaciones protegidas.
11. Debe existir mecanismo de cierre de sesión (Logout), invalidando el token.
12. El usuario debe poder iniciar una nueva sesión tras expirar o cerrar la anterior.
13. El acceso a las funcionalidades del sistema debe determinarse según los roles y permisos del usuario.

---

## 3. Estado Actual de la Rama (`feature/authImproved`)

Actualmente se está trabajando en la rama `feature/authImproved`, donde se refinó el proceso de autenticación por credenciales incorporando el segundo factor de autenticación (2FA) por correo electrónico.

### A. Lo que está Desarrollado en Backend (`Backend/ms_security`)

1. **Flujo de Autenticación por Credenciales + 2FA (`AuthController` / `AuthService`):**
   - **Paso 1: `POST /api/auth/login`**
     - Recibe email y contraseña (`LoginRequestDTO`).
     - Busca al usuario en base de datos y valida que esté activo (`user.getActive()`).
     - Valida la contraseña cifrada usando `PasswordEncoder` (BCrypt fuerza 12).
     - Genera un código criptográficamente seguro de 6 dígitos numéricos (`SecureRandom`).
     - Registra una entidad `Session` vinculada al usuario con expiración de 5 minutos y `isCodeUsed = false`.
     - Invoca a `NotificationService` para despachar el código vía REST a `ms_notificaciones` (`POST /send-email`).
     - Responde con `LoginInitResponseDTO` notificando que el código fue despachado.
   - **Paso 2: `POST /api/auth/verify-2fa`**
     - Recibe email y código de 6 dígitos (`Verify2FaRequestDTO`).
     - Carga al usuario con sus roles y permisos mediante `@EntityGraph`.
     - Valida que el usuario continúe activo (`user.getActive()`).
     - Obtiene la última sesión generada para ese usuario.
     - Valida que el código no haya sido usado previamente (`isCodeUsed`).
     - Valida que la sesión no haya expirado (`expiration.isBefore(now)`).
     - Valida concordancia del código ingresado.
     - Marca `isCodeUsed = true`.
     - Genera el token JWT firmado mediante `JwtService` (15 min de vigencia, HMAC-SHA256) incorporando claims de `roles` y `permissions` (`method:url`).
     - Guarda el token generado en la entidad `Session`.
     - Retorna `AuthResponseDTO` (`accessToken`, `tokenType: "Bearer"`, `expiresAt`).
   - **Paso 3: `POST /api/auth/logout`**
     - Recibe cabecera `Authorization: Bearer <token>`.
     - Invalida la sesión activa en BD fijando su expiración a la fecha actual.

2. **Entidades y Persistencia:**
   - `User`: Contiene `id`, `name`, `email`, `password`, `active`, relaciones con `Profile`, `Session` y `UserRole`.
   - `Session`: Modificada para alojar `token`, `expiration`, `code_2fa`, `is_code_used` y relación `@ManyToOne` con `User`.
   - `SessionRepository`: Métodos para buscar última sesión por email (`findTopByUser_EmailOrderByIdDesc`) y por token (`findByToken`).
   - `UserRepository`: Consulta optimizada `findWithRolesAndPermissionsByEmail` evitando problemas de N+1.

3. **Seguridad y JWT (`SecurityConfig`, `JwtService`):**
   - Endpoints `/api/auth/login`, `/api/auth/verify-2fa` y `/api/auth/logout` configurados con acceso público (`permitAll`).
   - Servidor configurado como Stateless (`SessionCreationPolicy.STATELESS`) con soporte de Resource Server JWT.
   - Inyección de roles y permisos del usuario en los claims del token JWT.

4. **Integración con Microservicio de Notificaciones:**
   - `NotificationService`: Utiliza Spring `RestClient` para enviar la petición a `${services.notifications.url:http://localhost:8000}/send-email`.

---

### B. Lo que está en el Frontend (`Frontend`)

1. **Vistas e Interfaz:**
   - `Login.jsx` (`src/views/authentication/login/Login.jsx`):
     - Flujo de dos pasos interactivo:
       - **Paso 1:** Ingreso de credenciales con validación, spinner de carga y alertas de error.
       - **Paso 2:** Pantalla de verificación 2FA con input numérico formateado de 6 dígitos, opción para reenviar código o cambiar credenciales.
     - Redirección automática a `/dashboard` al autenticar exitosamente.
   - `AppHeaderDropdown.jsx`:
     - Opción interactiva "Cerrar Sesión" que invoca el endpoint de logout del backend, limpia el almacenamiento local y redirige al login.
2. **Servicios HTTP:**
   - `authService.js` (`src/services/authService.js`): Métodos `login`, `verify2Fa`, `logout`, almacenamiento de sesión en `localStorage` y helpers de autenticación.
   - `apiClient.js` (`src/services/apiClient.js`): Inyección automática de la cabecera `Authorization: Bearer <token>` en todas las peticiones salientes.

---

## 4. Estándares Técnicos Establecidos

- **Backend:**
  - Arquitectura limpia y estricta en capas: `controller` -> `service` -> `repository`.
  - Excepciones de negocio encapsuladas en `ApplicationException` con enumeración `ErrorCase`.
  - DTOs separados para petición (`*RequestDTO`) y respuesta (`*ResponseDTO`).
- **Frontend:**
  - Componentes funcionales modulares bajo `src/views/<modulo>/`.
  - Prohibido acoplar llamadas HTTP directas con `fetch` en componentes visuales.
  - Consistencia visual con CoreUI y tokens de diseño corporativos.
- **Git:**
  - Ramas descriptivas por funcionalidad (`feature/<modulo>`).
  - Commits bajo especificación de Conventional Commits en inglés.
  - Regla de confirmación previa antes de modificar archivos de código.
