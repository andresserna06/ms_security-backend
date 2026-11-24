---
name: notificaciones
description: Envío de correos desde ms_security mediante ms_notificaciones (FastAPI) para 2FA y recuperación de contraseña. Úsala al implementar, modificar o probar notificaciones.
---

# Notificaciones por correo - ms_security

ms_security **no envía correos directamente**. Delega en `ms_notificaciones` (FastAPI + Gmail API).

## Contrato externo

```
POST {services.notifications.url}/send-email   (default http://localhost:8000)
Content-Type: application/json
{ "to": "user@email.com", "subject": "Asunto", "html_content": "<p>HTML</p>" }
```

- Un destinatario por petición.
- `html_content` es contenido; el servicio externo puede envolverlo en plantilla.
- Respuestas esperadas: 200 éxito; 422 payload inválido; 502/error proveedor.

## Uso actual en ms_security

- `NotificationService.send2FaCode(toEmail, code)`: envía código 2FA.
- `NotificationService.sendPasswordResetLink(toEmail, resetLink, userName)`: envía enlace de recuperación.
- URL base: `services.notifications.url` en `application.properties` / env.

## Reglas

- El contenido visible al usuario (`subject`, `html_content`) va en español.
- No loguear códigos, tokens, enlaces completos sensibles en producción; si se loguea en desarrollo, minimizar exposición.
- Escapar/serializar correctamente el JSON antes de enviar.
- **2FA y recuperación**: si el correo no puede enviarse, la operación que depende del correo debe fallar con error controlado (`ApplicationException`, `INVALID_OPERATION`), porque el usuario no puede completar el flujo.
- Distinguir este comportamiento de dominios no críticos: en este módulo, 2FA/reset son críticos.

## Pruebas

- Unitarias con `RestClient`/cliente simulado: payload correcto, manejo de error, no exponer secretos.
- Integración manual: con ms_notificaciones arriba, verificar recepción; con ms_notificaciones caído, verificar error controlado y que no se complete autenticación/reset.
- No crear endpoints especiales para pruebas de correo.
