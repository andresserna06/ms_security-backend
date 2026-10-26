# Reglas del Proyecto (AGENTS.md)

## 1. Flujo de Trabajo del Agente
- **Autorización previa obligatoria:** Nunca crear, modificar ni eliminar archivos de código sin antes explicar qué archivos se van a tocar, mostrar la propuesta y recibir confirmación explícita del usuario.
- **Alcance acotado:** Implementar únicamente la historia de usuario o funcionalidad que el usuario indique en el momento, sin adelantarse a otras historias de usuario.
- **Registro de pendientes:** Cualquier criterio de aceptación o ajuste que quede en espera (por dependencia de otro compañero o fase posterior) debe documentarse en `Backend/ms_security/pendientes/PENDIENTES.md`.

---

## 2. Convenciones de Git y Ramas
- **Estrategia de ramas:** Trabajar cada historia de usuario o módulo en su propia rama `feature/<modulo>` (ej. `feature/usuarios`, `feature/roles`, `feature/permisos`, `feature/seguridad`), manteniendo `main` estable.
- **Conventional Commits:** Usar siempre el formato `<tipo>(<scope>): <descripción en inglés>` (ej. `feat(users): add user management view`, `fix(config): update cors port`).
- **Archivos sensibles:** Nunca subir `.env` ni credenciales personales al repositorio.

---

## 3. Estándares de Backend (`Backend/ms_security`)
- **Stack:** Java 17, Spring Boot, Spring Data JPA, MySQL (`puerto 8181`).
- **Arquitectura en capas estricta:**
  - `controller`: Solo recibe peticiones HTTP (`@Valid`), delega al servicio y retorna DTOs con el código HTTP adecuado (`200`, `201`, `204`).
  - `service`: Contiene toda la lógica de negocio y validaciones; lanza `ApplicationException` usando `ErrorCase` (`NOT_FOUND`, `ALREADY_EXISTS`, `INVALID_OPERATION`).
  - `repository`: Interfaces JPA (usando `@EntityGraph` cuando se requiera cargar relaciones evitando N+1).
  - `dto` y `mapper`: Separación entre DTOs de entrada y salida; nunca exponer entidades JPA directamente en los controladores para evitar ciclos de recursividad.

---

## 4. Estándares y Arquitectura Modular en Frontend (`Frontend` - React)
- **Stack:** React 19 + Vite + CoreUI / Bootstrap 5 (`puerto 3000`).
- **Convención de Nombres (Estándar React):**
  - Componentes visuales (`.jsx`): `PascalCase` (ej. `UsersPage.jsx`, `UsersTable.jsx`).
  - Servicios y utilidades lógicas (`.js`): `camelCase` (ej. `apiClient.js`, `userService.js`).
- **Principio de Orden Modular:**
  Está **prohibido** crear componentes monolíticos gigantes que mezclen llamadas HTTP, tablas, formularios y modales en un solo archivo, y tampoco crear subcarpetas `components` redundantes dentro de cada vista. Cada módulo debe separar estrictamente sus responsabilidades:

  1. **Componentes Globales del Layout (`src/components/`):**
     - Única carpeta `components` del proyecto; contiene exclusivamente los elementos globales compartidos del layout (`AppHeader`, `AppSidebar`, `AppFooter`, `AppBreadcrumb`, `AppContent`).

  2. **Capa de Servicios HTTP (`src/services/`):**
     - Ningún componente visual puede hacer `fetch` directo con URLs quemadas.
     - Existirá un cliente base (`src/services/apiClient.js`) que centralice la URL base (`http://localhost:8181/api`) y el manejo de errores del Backend.
     - Cada entidad tendrá su propio servicio dedicado (ej. `userService.js`, `profileService.js`, `userRoleService.js`).

  3. **Estructura por Módulo (`src/views/<modulo>/`):**
     Cada módulo de negocio (ej. `users`, `roles`, `permissions`) tendrá su propia carpeta con sus archivos de vista y subcomponentes en un único nivel limpio:
     ```text
     src/views/<modulo>/
     ├── <Modulo>Page.jsx       # Componente contenedor (coordina estado y llamadas a servicios)
     ├── <Modulo>Table.jsx      # Tabla de listado y botones de acción
     ├── <Modulo>DetailCard.jsx # Tarjeta/panel lateral de detalle
     └── <Modulo>FormModal.jsx  # Modales de creación/edición o gestión específica
     ```

  4. **Rutas y Navegación Centralizadas:**
     - Registrar las páginas principales únicamente en `src/routes.js` (con `React.lazy`) y los accesos del menú lateral en `src/_nav.jsx`.
     - Reutilizar los componentes de CoreUI/Bootstrap (`CCard`, `CTable`, `CModal`, `CForm`, `CButton`, `CBadge`, `CAlert`, `CSpinner`) manteniendo consistencia visual en toda la aplicación.
