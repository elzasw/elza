# ELZA - Elektronické zpracování archiválií (Electronic Archival Processing)

Czech archival management system with Java/Spring Boot backend and React/TypeScript frontend.

## Project Structure

```
├── elza-core/          # Backend: Java/Spring Boot (main business logic, ~1800 Java files)
├── elza-react/         # Frontend: React/TypeScript/Redux (~480 TSX files)
├── elza-web/           # Spring Boot entry point (thin wrapper around elza-core)
├── elza-ws-api/        # WSDL/SOAP Web Services API definitions
├── elza-doc/           # Documentation module
├── package-cz-base/    # Czech base data package (institutions, etc.)
├── rules-cz-zp2015/    # Czech archival rules (ZP2015)
├── rules-simple-dev/   # Simple dev rules
├── dao-test-bench/     # DAO testing utility
└── distrib/            # Distribution packaging (war, exe, docker, tomcat)
```

## Tech Stack

- **Backend:** Java 17, Spring Boot 3, Hibernate 6 + Hibernate Search (Lucene), Spring Security, JPA, WebSockets (STOMP)
- **Frontend:** React 18, TypeScript, Redux 4, Vite, React Bootstrap, Fluent UI, SCSS
- **Rules Engine:** Drools 10
- **Reporting:** JasperReports, PDFBox
- **Database:** PostgreSQL (with PostGIS)
- **DB Migrations:** Liquibase (XML changesets in elza-core)
- **API:** REST (OpenAPI spec), SOAP/WSDL, WebSocket
- **Build:** Maven, Node.js

Exact versions live in `pom.xml` / `package.json`.

## Key Backend Packages (elza-core)

Path prefix: `elza-core/src/main/java/cz/tacr/elza/`

| Package | Purpose |
|---------|---------|
| `controller/` | REST API controllers (ArrangementController, FundController, AccessPointController, etc.) |
| `api/` | REST API DTOs and endpoint definitions |
| `service/` | Business logic services |
| `repository/` | Spring Data JPA repositories |
| `domain/` | JPA entity classes (database model) |
| `config/` | Spring configuration classes |
| `security/` | Authentication and authorization |
| `drools/` | Business rules (Drools integration) |
| `cam/` | Common Archival Model (CAM) |
| `dataexchange/` | Data import/export |
| `bulkaction/` | Bulk operation processing |
| `asynchactions/` | Async action handling |
| `websocket/` | WebSocket communication |
| `print/` | Report/print generation |
| `dbchangelog/` | Liquibase Java-based migrations |
| `groovy/` | Groovy scripting support |
| `validation/` | Data validation |
| `ws/` | SOAP web service implementations |

## Key Frontend Structure (elza-react)

Path prefix: `elza-react/src/`

| Directory | Purpose |
|-----------|---------|
| `components/` | React components organized by domain (arr/, admin/, registry/, fund/, shared/) |
| `pages/` | Page-level components (arr/, admin/, entity/, fund/, registry/, reports/) |
| `actions/` | Redux action creators |
| `stores/` | Redux store configuration |
| `api/generated/` | **Auto-generated** TypeScript API client from OpenAPI spec (DO NOT edit manually) |
| `api/old/` | Legacy API code |
| `shared/` | Shared utilities, factories, field definitions |
| `typings/` | TypeScript type definitions |
| `utils/` | Utility functions and custom hooks |

## Important: Auto-Generated Code

- `elza-react/src/api/generated/` is auto-generated from `elza-core/src/main/resources/rest/elza-openapi.yml` by OpenAPI Generator. Never edit these files manually.
- SOAP stubs in `elza-ws-api/` are generated from WSDL files.

## Build Commands

```bash
# Full build (skip tests)
mvn install -Pskiptest

# Install npm packages (in elza-react/)
mvn exec:exec -Pnpm-install

# Start backend (in elza-web/)
mvn spring-boot:run

# Start frontend dev server (in elza-react/, Node.js v17+)
mvn exec:exec -Pfrontend-dev-legacy

# Release build with React
mvn -Prelease install
```

## Configuration

- Main config file: `elza.yaml` (in elza-web/config/)
- OpenAPI spec: `elza-core/src/main/resources/rest/elza-openapi.yml`
- Frontend proxy config: `elza-react/.env.development.local` (ENDPOINT=http://localhost:8080)
- Backend runs on port 8080, frontend dev server on port 3000

## Domain Context

ELZA is an archival management system used by Czech archives. Key domain concepts:
- **Fund (Fond)** - An archival fund (collection of archival materials)
- **Arrangement (Pořádání)** - The process of organizing archival materials into a hierarchy
- **Node** - A node in the arrangement tree (hierarchical unit)
- **Access Point (Přístupový bod)** - Entity registry entry (person, place, event, etc.)
- **Output (Výstup)** - Generated finding aid or report
- **Bulk Action** - Batch processing operation on arrangement nodes
- **Rule Set** - Set of validation/description rules for archival processing
- **Package** - Importable configuration package (rules, templates, etc.)
- **DAO (Digital Archival Object)** - Digital representation of physical archival material
- **CAM (Common Archival Model)** - Standard for archival entity description

## Language

- Source code: English (variable names, class names)
- Comments and documentation: predominantly Czech
- UI strings: Czech
- When writing commit messages or comments, use Czech if the surrounding context is Czech, otherwise English.
- New comments in code should preferably be written in English. The only exception is when fixing or updating a pre-existing comment that is already in Czech — in that case keep it in Czech to stay consistent with the surrounding text.
- Developers are non-native English speakers. When they write English — in code, comments, commit messages, or chat — gently correct grammar/phrasing mistakes and briefly explain the preferred wording so they can learn.

## Collaboration

- **Ask before acting when uncertain.** If you have doubts about the proposed approach, are missing information needed to implement it correctly, or believe the user's request is suboptimal (wrong abstraction, breaks an invariant, simpler alternative exists), stop and ask rather than guessing or silently "fixing" it. A short clarifying question is always cheaper than a wrong implementation.
- Surface the specific doubt or concern concretely (what you'd do vs. what seems better, or what information you'd need), not a generic "are you sure?".

## Conventions

- Database changes must go through Liquibase migrations (XML changesets in `elza-core`; Java-based migrations in `dbchangelog/`).
- REST API changes start from the OpenAPI spec (`elza-core/src/main/resources/rest/elza-openapi.yml`); the TypeScript client in `elza-react/src/api/generated/` is regenerated from it.
