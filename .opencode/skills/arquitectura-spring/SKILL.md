---
name: arquitectura-spring
description: Arquitectura por capas en Spring Boot para ms_security. Úsala al crear o modificar controllers, services, repositories, entities, DTOs, mappers, config, seguridad o manejo de excepciones.
---

# Arquitectura Spring Boot - ms_security

## Stack y estructura

- Spring Boot 4.1.1, Java 17, Maven (`pom.xml`).
- Paquete base: `com.uc.ms_security`.
- Capas: `controller/`, `service/`, `repository/`, `entity/`, `dto/`, `mapper/`, `exception/`, `config/`, `authorization/`.

## Flujo de dependencias

```
Controller -> Service -> Repository -> JPA/MySQL
                \-> Mapper
                \-> NotificationService / OAuth strategies (según caso)
```

El controller no accede a repositorios ni entidades JPA para lógica de negocio.

## Responsabilidades

**Controller**
- Recibe request, valida con `@Valid`, delega a service, devuelve DTO/ResponseEntity/status.
- Sin reglas de negocio ni consultas.
- Códigos por defecto/`@ResponseStatus` coherentes con `api-rest`.

**Service**
- Reglas de negocio HU-SE, orquestación, transacciones (`@Transactional` cuando aplica).
- Lanza `ApplicationException(ErrorCase, message)` para errores de dominio.
- No expone entidades JPA a la capa web.

**Repository**
- Interfaces Spring Data JPA; consultas específicas (`existsBy...`, `findWith...`).
- Sin lógica de negocio.

**Entity**
- Modelo persistente JPA. No se devuelve al cliente.
- Relaciones/cascadas cuidadosas; no borrar en cascada entidades principales sin regla explícita.

**DTO**
- Entrada: Bean Validation; mensajes claros en español.
- Salida: DTOs explícitos; nunca incluir `password`, secretos, códigos internos.

**Mapper**
- Conversión entidad<->DTO. Sin consultas, sin reglas de negocio, sin HTTP.

**Exception/Config/Auth**
- `GlobalExceptionHandler`: traduce excepciones a formato estándar.
- `SecurityConfig`: stateless JWT, rutas públicas, resto con `DynamicAuthorizationManager`.
- `PermissionDataSeeder`: permisos/rol admin bootstrap; mantener alineado con endpoints.

## SOLID

- **S**: separar auth, OAuth, notificaciones, permisos y CRUD por responsabilidad.
- **D**: usar interfaces/estrategias donde ya existe (`OAuthProviderStrategy`); evitar acoplar controllers a implementaciones concretas innecesarias.

## Seguridad

- Contraseñas: BCrypt (`PasswordEncoder`), nunca texto plano.
- JWT/sesiones: expiración, invalidación en logout, verificación de vigencia.
- 2FA/reset: código/token temporal, un solo uso, invalidar al usar o expirar.
- OAuth: validar identidad con proveedor; no pedir contraseña local ni 2FA interno para OAuth.
- Logs sin secretos/tokens/códigos.

## Configuración

- `application.properties` usa variables de entorno; secretos en `.env` (no commitear).
- `spring.jpa.hibernate.ddl-auto=update` es configuración actual; cambios de esquema deben revisarse cuidadosamente antes de producción.

## Pruebas y build

- Maven Wrapper: `./mvnw.cmd test`, `./mvnw.cmd -Dtest=... test`.
- Mantener compatibilidad Java 17.
