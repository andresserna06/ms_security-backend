---
name: historia-a-spec
description: Plantilla y procedimiento para convertir una historia HU-SE del módulo de seguridad en especificación técnica antes de implementarla. Úsala al analizar, especificar o planear una HU.
---

# De historia de usuario a especificación - HU-SE

## Procedimiento

1. Leer la historia HU-SE exacta (no reinterpretar).
2. Leer skills `reglas-dominio`, `modelo-relacional`, `api-rest`.
3. Revisar código existente relacionado en `src/main/java/com/uc/ms_security/`.
4. Escribir `docs/specs/HU-SE-XX.md` con la plantilla.
5. Lo no definido va a **Preguntas abiertas**; no asumir.

## Plantilla

```markdown
# HU-SE-XX — <Título>

## Historia
<copiada textual>

## Dependencias
- Historias/módulos previos:
- Código existente relacionado:

## Modelo
- Entidades/campos/restricciones nuevas o afectadas (referenciar `modelo-relacional`).

## Endpoints
| Método | Ruta | Auth/Permiso | Body | Éxito | Errores |
|---|---|---|---|---|---|

## Reglas de negocio
| # | Regla | Criterio origen | Error/errorCase |
|---|---|---|---|

## Seguridad
- Autenticación, autorización, sesión/token, datos sensibles, auditoría/logs.

## Trazabilidad de criterios
| Criterio de aceptación (textual) | Cómo se cumple (endpoint/regla/capa) |
|---|---|

## Casos límite
- 

## Pruebas sugeridas
- Postman/Newman:
- JUnit/MockMvc:

## Preguntas abiertas
- 
```

## Calidad

- Cada criterio de aceptación aparece en trazabilidad.
- Cada regla indica HTTP y `errorCase` cuando aplique.
- Rutas coherentes con `api-rest` o cambio justificado.
- Sin código de implementación.
