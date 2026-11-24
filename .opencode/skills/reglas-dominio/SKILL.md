---
name: reglas-dominio
description: Reglas de negocio del módulo de seguridad HU-SE-01 a HU-SE-10: usuarios, perfiles, roles, permisos, asignaciones, registro, autenticación, sesión, recuperación de contraseña y 2FA. Úsala al implementar, revisar o probar validaciones.
---

# Reglas de dominio - Módulo de Seguridad

Basado en `Anexo_1_Módulo Seguridad.docx` (HU-SE-01..10) y el estado actual del código. Si una regla no está definida, no se inventa: va a Preguntas abiertas.

## HU-SE-01 Gestionar usuarios

- Registrar usuario con información requerida; email único.
- Contraseña almacenada de forma segura (BCrypt), nunca texto plano.
- Consultar listado, detalle, actualizar, eliminar/desactivar según reglas del sistema.
- Desde listado/detalle debe poder accederse a perfil y roles (API o contrato UI según alcance).
- Desactivar/eliminar usuario no elimina roles ni permisos del sistema.

## HU-SE-02 Gestionar perfiles de usuario

- Usuario debe existir previamente. Máximo un perfil por usuario.
- Consultar, registrar o actualizar teléfono/foto.
- Perfil asociado solo a su usuario.
- Cambios de perfil no alteran credenciales/auth provider.
- Cambios persistidos correctamente.

## HU-SE-03 Gestionar roles

- Crear rol con mínimo nombre y descripción.
- Nombre de rol no duplicado.
- Consultar listado/detalle, actualizar, eliminar cuando no haya restricciones.
- Antes de eliminar, validar si está asignado a usuarios (409 si la política lo impide).
- Desde rol se accede a usuarios y permisos.
- Eliminar rol no elimina usuarios ni permisos.

## HU-SE-04 Asignar roles a usuarios

- Usuario y rol previamente registrados.
- Un usuario puede tener varios roles; un rol puede tener varios usuarios.
- No asignar dos veces el mismo rol al mismo usuario (409).
- Permitir retirar rol; retirar no elimina usuario ni rol.
- Cambios reflejados en permisos efectivos del usuario.
- [PENDIENTE] Confirmación de retiro es requisito UI; API debe soportar operación idempotente/segura.

## HU-SE-05 Gestionar permisos

- Registrar permiso con recurso/módulo, operación, URL, método HTTP y modelo/recurso según modelo.
- Consultar listado/detalle, actualizar, eliminar cuando reglas lo permitan.
- Evitar permisos duplicados para misma operación y recurso (`url+method` o criterio definido).
- Eliminar permiso no elimina roles asociados; si está asignado, definir 409/invalidación según regla.

## HU-SE-06 Asignar permisos a roles

- Rol y permisos previamente registrados.
- Mostrar permisos asignados y agrupar por módulo/recurso.
- Matriz por recurso con operaciones Ver/Listar/Crear/Actualizar/Eliminar (mapeo a endpoints HTTP).
- Asignar marcando, retirar desmarcando; no duplicar asociación rol-permiso.
- Guardar cambios de matriz.
- Retirar permiso de rol no elimina permiso.
- Cambios aplican a usuarios con el rol.
- Si ningún rol del usuario tiene permiso requerido, denegar acceso (403). Si alguno lo tiene, permitir.
- Permisos persisten tras cerrar/iniciar sesión.

## HU-SE-07 Registrar usuario mediante credenciales

- Registro con mínimo nombre, correo, contraseña.
- Email único; validar campos obligatorios.
- Contraseña cumple política de seguridad; no texto plano.
- Crear usuario LOCAL al completar registro; puede autenticarse luego con email/contraseña.
- Informar correo ya registrado.
- Usuarios OAuth no usan registro manual con contraseña.

## HU-SE-08 Autenticarse y gestionar sesión

- Dos mecanismos: credenciales y OAuth (Google, GitHub, Microsoft/Azure según código actual).
- Credenciales: usuario registrado y habilitado; credenciales incorrectas impiden acceso.
- Si 2FA habilitado, tras validar credenciales se solicita segundo factor; sesión no autenticada hasta validarlo.
- OAuth: proveedor valida identidad; sin contraseña propia; sin 2FA interno de plataforma.
- Primer login OAuth: crear usuario si no existe, usando datos del proveedor.
- Si correo ya usado por autenticación básica y se intenta OAuth, rechazar login.
- Accesos posteriores OAuth identifican usuario existente sin duplicar.
- Al completar acceso, crear sesión interna con identificador/token y fecha de expiración.
- Sesión identifica usuario; sesión expirada no permite operaciones protegidas.
- Logout invalida token/sesión. Se puede iniciar nueva sesión tras cerrar/expirar.
- Acceso a funcionalidades determinado por roles/permisos.
- [PENDIENTE] Doc menciona "Google, GitHub y otro proveedor"; código soporta Azure/Microsoft/GitHub/Google. Mantener comportamiento real y documentar.

## HU-SE-09 Recuperar contraseña

- Solo para usuarios con autenticación por correo y contraseña (LOCAL).
- Solicitar recuperación por correo; generar mecanismo temporal con validez limitada.
- Permitir establecer nueva contraseña válida solo con solicitud válida.
- Solicitud de un solo uso; expirada rechazada.
- Tras cambiar contraseña, permitir login con nueva contraseña.
- Invalidar sesiones anteriores cuando corresponda.
- Usuarios solo OAuth recuperan credenciales con su proveedor, no en plataforma.

## HU-SE-10 Gestionar autenticación de dos factores

- Aplica solo a usuarios LOCAL.
- Permitir habilitar/deshabilitar 2FA cuando reglas lo permitan.
- Tras credenciales válidas, solicitar código si 2FA habilitado.
- Código asociado a proceso de autenticación/sesión; validez limitada.
- Código incorrecto/expirado no completa login; código de un solo uso.
- Tras validar 2FA, sesión activa con token y expiración.
- Si cancela/no completa 2FA, no accede a funcionalidades protegidas.
- Usuarios OAuth no pasan por 2FA interno.

## Reglas transversales

- Mensajes al usuario en español; nombres técnicos/código en inglés.
- No exponer `password`, tokens, códigos 2FA ni secretos en respuestas/logs.
- Duplicados/conflictos -> 409; no encontrado -> 404; validación/regla inválida -> 400; no autenticado -> 401; sin permiso -> 403.
- [PENDIENTE] Definir política exacta de "eliminar o desactivar cuando reglas lo permitan" para cada entidad.
