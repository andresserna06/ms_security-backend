---
name: flujo-git
description: Flujo Git para ms_security - ramas por historia HU-SE, Conventional Commits, y verificaciones Maven/Postman antes de commit/push. Úsala al crear ramas, commitear o hacer push.
---

# Flujo de Git - ms_security

## Ramas

- `main`: estable.
- Por historia: `feature/hu-se-01-users`, `feature/hu-se-08-auth-session`, etc.
- Cambios de configuración IA: `chore/ia-<descripcion>`.

## Commits

Conventional Commits, descripción en inglés, imperativo, sin punto final, ~72 chars:

- `feat(auth): add password reset token validation`
- `fix(users): reject duplicate email on update`
- `test(roles): add role assignment conflict cases`

Sin cuerpo salvo necesidad extrema. No mezclar historias. Sin coautoría IA ni firmas automáticas.

## Archivos sensibles/compartidos

- No commitear `.env`, credenciales, tokens, `target/`, `node_modules/`.
- Avisar antes de tocar archivos compartidos: `SecurityConfig`, `GlobalExceptionHandler`, `PermissionDataSeeder`, entidades comunes, `application.properties`, `.opencode/*`.

## Antes de commit y push

El agente no hace commit/push por su cuenta. Primero:

1. Mostrar archivos y resumen de cambios.
2. Verificar build/tests con Maven cuando aplique: `./mvnw.cmd test` o test específico.
3. Si hay colección Postman afectada, indicar si se ejecutó Newman y resultado.
4. Confirmar ausencia de secretos/`.env`/`target`.
5. Proponer mensaje de commit.
6. Esperar aprobación antes de commit, y otra antes de push.

Si algo falla, reportar y no continuar.

## Prohibido

- Commitear secretos.
- Forzar push a `main` sin solicitud explícita.
- Editar artefactos generados en `target/`.
