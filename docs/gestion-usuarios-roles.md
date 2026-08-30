# Definiciones: Sistema de Gestión de Usuarios y Roles

Documento de trabajo para acotar el requerimiento antes de implementar.
Complementa a [`CLAUDE.md`](../CLAUDE.md) (que define arquitectura y stack
generales del BFF y las Azure Functions); este documento define **qué**
construye este sistema en particular. Las secciones marcadas como
"pendiente" son decisiones a cerrar con el equipo antes de codear esa
parte.

## 1. Requerimiento

> Sistema de Gestión de Usuarios y Roles: CRUD sobre usuarios y roles,
> ejecutado por funciones serverless. Sin componente frontend. El backend
> debe incluir como mínimo un microservicio BFF que orqueste las llamadas
> a las funciones serverless.

## 2. Alcance

**Incluido**

- CRUD completo de `Usuario`.
- CRUD completo de `Rol`.
- Gestión de la relación entre `Usuario` y `Rol` (asignar/quitar roles a
  un usuario).
- Un BFF que expone la API y orquesta las llamadas a las Azure Functions.
- Azure Functions que implementan el CRUD real y acceden a Oracle.

**Explícitamente fuera de alcance**

- Frontend (indicado en el requerimiento).
- Autenticación/login de usuarios finales. **Decidido**: `Usuario` es una
  entidad de datos administrada por CRUD (username, email, nombre,
  roles), sin password ni mecanismo de login. Si más adelante se necesita
  autenticación de usuarios finales, es un requerimiento nuevo con su
  propio análisis de seguridad (hashing, políticas de password, etc.),
  no una extensión de este CRUD.
- Autorización fina (permisos por rol sobre otras partes del sistema) —
  este requerimiento gestiona roles como catálogo, no implementa un
  motor de permisos.

## 3. Componentes y responsabilidades

| Componente | Responsabilidad |
|---|---|
| **BFF** (`bff/`) | Expone la API REST del sistema, valida input a nivel de contrato, orquesta llamadas a las Azure Functions, agrega respuestas (ej. usuario + sus roles resueltos). Sin acceso a Oracle. |
| **Azure Functions** (`azure-functions/`) | Implementan el CRUD real de `Usuario` y `Rol` contra Oracle. Contienen las reglas de negocio/validación de datos (unicidad, integridad referencial, etc.). |
| **Oracle** | Persistencia de usuarios, roles y su relación. |

**Pendiente**: ¿una Function App con una function por operación/entidad, o
functions separadas para `usuarios` y `roles`? Ver [`CLAUDE.md`](../CLAUDE.md)
sección "Azure Functions" — se recomienda agrupar por dominio (`usuarios`,
`roles`) dentro de un único Function App para empezar.

## 4. Modelo de dominio

### Usuario

| Campo | Tipo | Notas |
|---|---|---|
| `id` | `NUMBER` | PK, generado por `IDENTITY` column (Oracle 12c+) |
| `username` | string | único |
| `email` | string | único |
| `nombreCompleto` | string | |
| `estado` | enum (`ACTIVO`/`INACTIVO`) | soft delete vía estado, no borrado físico (ver §6) |
| `fechaCreacion` | timestamp | |
| `fechaModificacion` | timestamp | |
| `roles` | Rol[] | resuelto vía relación N:M, no columna directa. Un usuario puede existir sin roles asignados (se asignan después de crearlo). |

### Rol

| Campo | Tipo | Notas |
|---|---|---|
| `id` | `NUMBER` | PK, generado por `IDENTITY` column |
| `nombre` | string | único (ej. `ADMIN`, `OPERADOR`) |
| `descripcion` | string | |
| `estado` | enum (`ACTIVO`/`INACTIVO`) | |
| `fechaCreacion` | timestamp | |

No hay roles "de sistema" protegidos contra edición/eliminación en este
alcance — todos los roles se gestionan igual vía CRUD. Si se necesita
proteger roles críticos (ej. `ADMIN`), es una regla a agregar más
adelante, no parte de este requerimiento inicial.

### Relación Usuario–Rol

- **Cardinalidad**: N:M — un usuario puede tener varios roles, un rol
  puede estar asignado a varios usuarios.
- Requiere una tabla puente (`USUARIO_ROL`) y, del lado de la API,
  operaciones propias además del CRUD plano:
  - Asignar un rol a un usuario.
  - Quitar un rol de un usuario.
  - Listar los roles de un usuario.

## 5. Modelo de datos Oracle

```sql
USUARIOS
├── ID              NUMBER          PK, GENERATED ALWAYS AS IDENTITY
├── USERNAME        VARCHAR2(100)   UNIQUE, NOT NULL
├── EMAIL           VARCHAR2(150)   UNIQUE, NOT NULL
├── NOMBRE_COMPLETO VARCHAR2(200)
├── ESTADO          VARCHAR2(20)    DEFAULT 'ACTIVO'
├── FECHA_CREACION  TIMESTAMP       DEFAULT SYSTIMESTAMP
└── FECHA_MODIF     TIMESTAMP

ROLES
├── ID              NUMBER          PK, GENERATED ALWAYS AS IDENTITY
├── NOMBRE          VARCHAR2(100)   UNIQUE, NOT NULL
├── DESCRIPCION     VARCHAR2(300)
├── ESTADO          VARCHAR2(20)    DEFAULT 'ACTIVO'
└── FECHA_CREACION  TIMESTAMP       DEFAULT SYSTIMESTAMP

USUARIO_ROL
├── USUARIO_ID      NUMBER          FK → USUARIOS.ID
├── ROL_ID          NUMBER          FK → ROLES.ID
├── FECHA_ASIGNACION TIMESTAMP      DEFAULT SYSTIMESTAMP
└── PK (USUARIO_ID, ROL_ID)
```

`GENERATED ALWAYS AS IDENTITY` (disponible desde Oracle 12c) evita el
boilerplate de secuencia + trigger para autogenerar el PK.

Migraciones (Flyway/Liquibase — herramienta pendiente de definir en
`CLAUDE.md`) se versionan dentro de `azure-functions/`, que es el único
componente con acceso a la base.

## 6. Operaciones CRUD requeridas

| Entidad | Operación | Verbo HTTP (BFF) | Function invocada |
|---|---|---|---|
| Usuario | Crear | `POST /usuarios` | `CrearUsuario` |
| Usuario | Listar | `GET /usuarios` | `ListarUsuarios` |
| Usuario | Obtener por id | `GET /usuarios/{id}` | `ObtenerUsuario` |
| Usuario | Actualizar | `PUT /usuarios/{id}` | `ActualizarUsuario` |
| Usuario | Eliminar | `DELETE /usuarios/{id}` | `EliminarUsuario` |
| Rol | Crear | `POST /roles` | `CrearRol` |
| Rol | Listar | `GET /roles` | `ListarRoles` |
| Rol | Obtener por id | `GET /roles/{id}` | `ObtenerRol` |
| Rol | Actualizar | `PUT /roles/{id}` | `ActualizarRol` |
| Rol | Eliminar | `DELETE /roles/{id}` | `EliminarRol` |
| Usuario–Rol | Asignar rol | `POST /usuarios/{id}/roles` | `AsignarRolAUsuario` |
| Usuario–Rol | Quitar rol | `DELETE /usuarios/{id}/roles/{rolId}` | `QuitarRolDeUsuario` |

**Eliminar = borrado lógico** (cambiar `estado` a `INACTIVO`), no `DELETE`
físico de la fila — así se preserva integridad referencial con
`USUARIO_ROL` e historial.

**Eliminar/inactivar un Rol en uso está bloqueado**: si el rol tiene
usuarios asignados (fila en `USUARIO_ROL`), la Function retorna `409
Conflict`. Hay que desasignarlo de todos los usuarios primero.

## 7. Contrato BFF ↔ Azure Functions (borrador)

- El BFF llama 1:1 a una Function por operación (sin agregar múltiples
  llamadas en la mayoría de los casos, salvo el detalle de usuario con
  roles resueltos, que si `usuarios` y `roles` son functions/tablas
  separadas puede requerir 2 llamadas: `ObtenerUsuario` +
  `ListarRolesDeUsuario`, agregadas en el `service` del BFF).
- Cada Function retorna JSON con el mismo shape que expone el BFF en su
  API (sin transformar demasiado) para minimizar lógica de mapeo — el BFF
  solo debería necesitar mapper cuando el contrato de la Function no
  calce 1:1 con el DTO expuesto al consumidor.
- Errores: la Function retorna códigos HTTP semánticos (`404` no
  encontrado, `409` username/email duplicado, `400` validación) y el BFF
  los propaga vía su `GlobalExceptionHandler` (ver `CLAUDE.md`), sin
  inventar una traducción de errores distinta.

## 8. Validaciones y reglas de negocio

- **Unicidad de `username`/`email`**: constraint `UNIQUE` en Oracle +
  capturar el error de violación en la Function (no validar antes con un
  `SELECT` separado, para evitar condiciones de carrera).
- **Rol en uso**: bloquear inactivación si tiene usuarios asignados (ver
  §6).
- **Usuario sin roles**: válido — un usuario puede crearse sin roles y
  asignárselos después.
- **Roles "de sistema" protegidos**: no aplica en este alcance (ver §4).
- [ ] Formato/validación de `email` y largo máximo de campos — definir en
      el DTO de entrada del BFF (falla rápido, antes de llegar a la
      Function). Detalle de implementación, no bloquea el scaffold.

## 9. Seguridad (referencia)

Quién puede invocar estos endpoints es una decisión de seguridad general
del proyecto, ya registrada como pendiente en `CLAUDE.md` (auth BFF hacia
Functions, y si el BFF expone auth propia hacia quien lo consuma). Este
documento no la duplica — solo señala que, al no haber frontend, el
consumidor de la API del BFF (Postman, otro servicio, un API Gateway)
debe quedar explícito antes de definir el mecanismo de auth.

## 10. Decisiones cerradas

| Decisión | Resultado |
|---|---|
| Cardinalidad Usuario–Rol | N:M, vía tabla puente `USUARIO_ROL` |
| Usuario incluye login/password | No — solo CRUD de datos |
| Borrado | Lógico (`ESTADO = INACTIVO`) |
| Rol en uso | Bloquear inactivación (`409`) |
| Generación de IDs | `NUMBER` con `GENERATED ALWAYS AS IDENTITY` |
| Roles de sistema protegidos | No aplica en este alcance |

Único punto abierto restante: formato/validación de campos individuales
(§8), que es un detalle de implementación y no bloquea el scaffold.
