---
name: pruebas-api
description: Pruebas de ms_security - matriz trazada a HU-SE, colección Postman/Newman, y pruebas Java JUnit/MockMvc cuando aplique. Úsala al diseñar, generar, ejecutar o analizar pruebas.
---

# Pruebas de API - ms_security

## Tipos de prueba

- **Integración API (principal)**: Postman/Newman contra API en ejecución.
- **Unitarias/integración Java**: JUnit 5, Mockito, Spring Test/MockMvc según exista configuración; ubicar en `src/test/java/com/uc/ms_security/...`.
- **Evidencia**: reporte de ejecución y fallos; no modificar código productivo para pasar pruebas.

## Matriz de casos

Antes de generar colección, entregar matriz y esperar aprobación:

| ID | Historia | Criterio | Endpoint | Precondición | Entrada | Esperado HTTP | Aserciones | Datos/variables |
|---|---|---|---|---|---|---|---|---|

- Cada criterio HU-SE tiene al menos un caso.
- Cubrir éxito, 400 validación, 401/403 auth, 404 no encontrado, 409 conflicto, casos límite.

## Casos límite obligatorios

- Email duplicado en registro/usuario/actualización.
- Contraseña inválida, vacía, o no cumple política.
- Login con credenciales incorrectas o usuario inactivo.
- 2FA: código incorrecto, expirado, reutilizado, sesión/proceso no correspondiente.
- OAuth: correo LOCAL intenta OAuth (rechazo), primer login crea usuario, login posterior no duplica.
- Reset password: token expirado, reutilizado, usuario OAuth/no LOCAL.
- Sesión expirada o logout: token no permite operación protegida.
- Asignaciones duplicadas user-role y role-permission (409).
- Eliminación con dependencias (rol asignado, permiso asignado) según regla.
- Acceso sin permiso -> 403; sin token/sesión -> 401.

## Postman

- Archivos sugeridos: `postman/Pruebas.postman_collection.json`, `postman/Endpoints.postman_collection.json`, `postman/Local.postman_environment.json`.
- Carpetas por historia HU-SE.
- Nombres de request comienzan con ID de caso.
- Scripts/aserciones en inglés; mensajes de negocio esperados en español cuando se valide texto.
- Guardar IDs/tokens con `pm.collectionVariables` o environment.
- Datos únicos con sufijo dinámico para ejecuciones repetibles.
- No hardcodear secretos reales; usar variables de entorno.

Ejecución:

```bash
newman run postman/Pruebas.postman_collection.json -e postman/Local.postman_environment.json
```

## Maven / JUnit

```bash
./mvnw.cmd test
./mvnw.cmd -Dtest=ClaseTest test
```

Si no hay tests, reportarlo y proponer estructura mínima sin alterar productivo.

## Reporte de fallos

Para cada fallo: caso, endpoint, aserción, esperado, recibido, evidencia (status/body/log relevante), causa probable y capa (controller/service/repository/config/security/mapper).
