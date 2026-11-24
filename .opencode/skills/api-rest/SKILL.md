---
name: api-rest
description: Convenciones REST del ms_security - rutas /api, métodos, códigos HTTP, formato de error ApplicationException y reglas de autenticación/autorización. Úsala al crear o cambiar endpoints, DTOs o contratos.
---

# API REST - ms_security

## Convenciones

- Rutas base bajo `/api/...`, recursos en plural cuando aplica: `/api/users`, `/api/roles`, `/api/permissions`.
- IDs numéricos en ruta: `/api/users/{id}`.
- Subrecursos por dueño: `/api/users/{userId}/profile`, `/api/users/{userId}/sessions`, `/api/role-permissions/roles/{roleId}`.
- Acciones de estado como subrecurso con verbo: `PATCH /api/users/{id}/activate`, `PATCH /api/users/{id}/deactivate`.
- JSON en camelCase. Fechas/instantes según DTO; sesiones/tokens con expiración.
- No devolver entidades JPA; usar DTOs de respuesta.

## Códigos de estado

| Código | Uso |
|---|---|
| 200 OK | Consulta o actualización exitosa con cuerpo |
| 201 Created | Creación exitosa |
| 204 No Content | Eliminación/cierre/logout exitoso sin cuerpo |
| 400 Bad Request | Validación DTO, regla de negocio inválida, formato incorrecto |
| 401 Unauthorized | No autenticado, credenciales inválidas, token/sesión inválida |
| 403 Forbidden | Autenticado sin permiso/rol requerido |
| 404 Not Found | Recurso o relación inexistente |
| 409 Conflict | Duplicado o conflicto de estado (ya asignado, restricción de eliminación, etc.) |
| 500 | Error inesperado (no intencional) |

Nunca devolver 200 con error de negocio en el cuerpo.

## Formato de error

Errores de negocio (`ApplicationException`):

```json
{ "errorCase": "ALREADY_EXISTS", "message": "Ya existe un usuario con este email" }
```

`ErrorCase` actuales: `NOT_FOUND`(404), `ALREADY_EXISTS`(409), `INVALID_OPERATION`(400), `UNAUTHORIZED`(401), `FORBIDDEN`(403).

Errores HTTP no controlados como negocio: `{ "errorCase": "HTTP_ERROR", "message": "..." }`.

Errores de validación (`MethodArgumentNotValidException`, 400): mapa plano campo->mensaje:

```json
{ "email": "El email no tiene un formato válido" }
```

Error inesperado 500: `INTERNAL_ERROR` con `errorId` y `path`; detalle solo en logs.

## Endpoints relevantes (estado actual)

| Área | Endpoints |
|---|---|
| Auth | `POST /api/auth/register`, `/login`, `/verify-2fa`, `/google`, `/github`, `/azure`, `/forgot-password`, `/reset-password`, `/logout` |
| Users | `POST/GET /api/users`, `GET/PUT/DELETE /api/users/{id}`, `GET /api/users/{id}/detail`, `GET /api/users/{id}/detail-with-sessions`, `GET /api/users/{id}/detail-with-roles`, `PATCH /api/users/{id}/activate`, `PATCH /api/users/{id}/deactivate` |
| Profile | `GET/POST/PUT/DELETE /api/users/{userId}/profile` |
| Sessions | `POST/GET /api/users/{userId}/sessions`, `GET/PUT/DELETE /api/users/{userId}/sessions/{sessionId}` |
| Roles | `POST/GET/PUT/DELETE /api/roles...`, detalle con permisos |
| Permissions | `POST/GET/PUT/DELETE /api/permissions...` |
| Role-Permissions | asignar/listar/retirar permisos de rol |
| User-Roles | asignar/listar/retirar roles de usuario |

Antes de agregar rutas, verificar controlador existente y mapa de HU-SE.

## Seguridad

- Públicos en `SecurityConfig`: register, login, verify-2fa, OAuth, forgot/reset password, logout.
- El resto protegido por `DynamicAuthorizationManager`: valida claim `permissions` del JWT y permisos vigentes por rol en BD.
- Respuestas 401/403 no deben filtrar información sensible.

## Documentación

- No hay Swagger configurado actualmente. La colección Postman es la documentación ejecutable.
- Si se agrega OpenAPI, documentar códigos, DTOs y seguridad Bearer JWT.
