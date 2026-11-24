---
name: modelo-relacional
description: Modelo de datos de ms_security - usuarios, perfiles, roles, permisos, asignaciones, sesiones, tokens de reset y proveedores auth. Úsala al crear o cambiar entidades, relaciones, constraints o migraciones.
---

# Modelo relacional - ms_security

## Entidades principales

| Entidad | Tabla | Campos clave | Restricciones |
|---|---|---|---|
| User | `users` | id, name, email, password(nullable), active, authProvider | email único, password nullable para OAuth, active bool |
| Profile | `profiles` | id, user_id, phone?, photo? | 1:1 con User, perfil único por usuario |
| Role | `roles` | id, name, description? | name único (case-insensitive en negocio) |
| Permission | `permissions` | id, url, method, module? | UNIQUE/negocio por (url, method), identifica recurso+operación |
| UserRole | `user_roles` | id, user_id, role_id | N:N User-Role, evitar duplicado (user,role) |
| RolePermission | `role_permissions` | id, role_id, permission_id | N:N Role-Permission, evitar duplicado (role,permission) |
| Session | `sessions` | id, user_id, token/id, expiration, code_2fa?, codeUsed?, etc. | pertenece a User; expiración obligatoria; 2FA asociado a sesión/proceso |
| PasswordResetToken | `password_reset_tokens` | id, user_id, token/hash, expiration, used/invalidated | temporal, un solo uso, expiración limitada |
| AuthProvider | enum | LOCAL, GOOGLE, AZURE, MICROSOFT, GITHUB | define mecanismo de autenticación |

## Relaciones

- User 1:1 Profile.
- User 1:N Session.
- User N:N Role mediante UserRole.
- Role N:N Permission mediante RolePermission.
- User 1:N PasswordResetToken (o último activo según implementación).

## Reglas de integridad

- No permitir roles duplicados por nombre.
- No permitir permisos duplicados por operación/recurso (`url+method`).
- No asignar dos veces el mismo rol al mismo usuario.
- No asignar dos veces el mismo permiso al mismo rol.
- Perfil pertenece únicamente a su usuario; máximo un perfil por usuario.
- Eliminar/desactivar usuario no debe eliminar roles/permisos del sistema.
- Eliminar rol no debe eliminar usuarios/permisos; validar asignaciones antes de eliminar si la regla lo exige.
- Eliminar permiso no debe eliminar roles; validar asignaciones según regla.

## Borrado y cascadas

- Evitar cascadas destructivas hacia entidades principales (User, Role, Permission).
- Cascada/orphanRemoval aceptable solo en partes internas claramente dependientes (ej. datos internos de usuario) si no contradice HU-SE.
- Preferir validación en service + 409 cuando hay dependencias.

## Autorización y datos

- `Permission` modela método HTTP y patrón/ruta. `DynamicAuthorizationManager` y `PermissionService` deben coincidir en el formato.
- Cambios en permisos efectivos deben reflejarse al consultar/autorizar según diseño actual (JWT claim + BD).

## Pendientes

- [PENDIENTE] Confirmar si `Session` almacena token JWT completo o identificador; aplicar regla de no exponer secretos.
- [PENDIENTE] Confirmar política exacta de eliminación física vs desactivación para User/Profile/Session.
- [PENDIENTE] Confirmar si se requiere unicidad adicional por `module+url+method` para HU-SE-05.
