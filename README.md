# ELZA - Electronic Processing of Archival Materials

ELZA (*Elektronické zpracování archiválií*) is an archival management system for
the arrangement and description of archival funds: the hierarchy of units of
description, entity registries (persons, corporate bodies, places, ...), finding
aids and other outputs, digital archival objects and integration with digital
archives. It is used by Czech archives; the core is language-neutral and the
rules of description come from importable packages.

The backend is Java 17 / Spring Boot 3 with PostgreSQL + PostGIS, the user
interface is React / TypeScript.

## Documentation

| Where | For whom |
|---|---|
| [docs/admin-guide](docs/admin-guide) | administrators: installation, upgrade, configuration, security, backup, release notes |
| [docs/user-guide](docs/user-guide) | users of the international rule set (ISAD(G), ISAAR(CPF)) |
| [docs/implementation-guide](docs/implementation-guide) | authors of rules packages and their customizations |
| [docs/design](docs/design) | developers: design records and their rationale |

The guides are Sphinx projects; each folder has a README on how to build it.
The Czech documentation is maintained separately (elza-doc).

## Repository layout

| Module | Content |
|---|---|
| `elza-core` | backend: business logic, REST/WebSocket API, Liquibase migrations, OpenAPI spec |
| `elza-web` | Spring Boot entry point (`cz.tacr.elza.web.ElzaWebApp`), development configuration in `elza-web/config` |
| `elza-react` | user interface (React, TypeScript, Vite) |
| `elza-ws-api` | SOAP/WSDL interfaces |
| `package-cz-base`, `package-isaar-cpf` | base packages: code lists and entity description (CAM, ISAAR(CPF)) |
| `rules-cz-zp2015`, `rules-en-isadg`, `rules-simple-dev` | rules of archival description (ZP2015, ISAD(G), a minimal rule set for development) |
| `elza-development` | development material: TypeSpec API sources, demo data, database update samples |
| `dao-test-bench` | test counterpart of a digital-object repository (DAO test bench) |
| `distrib` | distribution: executable JAR, WAR, ZIP distribution, Docker image |

## Prerequisites

* [Git](https://git-scm.com/)
* [JDK 17](https://adoptium.net/temurin/releases/?version=17)
* [Apache Maven 3.9+](https://maven.apache.org/download.cgi)
* [Node.js 20+](https://nodejs.org/) - only for the user interface
* [PostgreSQL](https://www.postgresql.org/) with the [PostGIS](https://postgis.net/) extension

## Database

Create an empty database and enable PostGIS in it:

```
CREATE EXTENSION postgis;
```

The application creates and upgrades the schema itself (Liquibase) at startup.
Use a dedicated database user, not a superuser; details are in the
administration guide, chapter *Installation*.

## Building

```
mvn install -Pskiptest
```

builds all modules without tests. A plain `mvn install` also runs the backend
tests and the frontend tests (Vitest); `-DskipTests` or `-Pskiptest` skips both.

Release build, including the production build of the user interface:

```
mvn -Prelease install
```

It produces:

* `distrib/elza-tomcat/target/elza-tomcat.jar` - the application with an embedded web server (`java -jar elza-tomcat.jar`),
* `distrib/elza-war/target/elza-<version>.war` - for deployment to an external Tomcat 10,
* a ZIP distribution in `distrib/distribution/target` - the JAR, a sample configuration, the base packages and the list of institutions.

Installing and running these is described in the administration guide.

Some dependencies come from repositories served over plain HTTP. Maven 3.8.1+
blocks those by default; if the build cannot resolve them, see
[how to allow HTTP repositories](https://stackoverflow.com/questions/67001968/how-to-disable-maven-blocking-external-http-repositories).

Javadoc: `mvn -Pjdoc,skiptest -Dmaven.javadoc.skip=false package` writes it to
`target/apidocs`; the profile `jdocuml` adds UML diagrams of the model
(needs [Graphviz](https://graphviz.org/) with `dot` on the `PATH`).

## Running for development

1. **Configuration.** Copy `elza-web/config/elza.yaml.template` to
   `elza-web/config/elza.yaml` and set the database connection:

   ```yaml
   elza:
     data:
       url: jdbc:postgresql://localhost/elza
       user: elza
       pass: secret
   ```

   The template keeps `buildType: DEV` for development. All configuration keys
   are described in the administration guide, chapter *Configuration*.

2. **Build** once: `mvn install -Pskiptest` (in the root).

3. **Backend** (in `elza-web`):

   ```
   mvn spring-boot:run
   ```

   The server listens on http://localhost:8080. In an IDE run the main class
   `cz.tacr.elza.web.ElzaWebApp` from the module `elza-web` with the working
   directory `elza-web`, so that `config/elza.yaml` is found.

4. **User interface** (in `elza-react`): install the npm packages once, then
   start the Vite dev server:

   ```
   mvn exec:exec -Pnpm-install
   mvn exec:exec -Pfrontend-dev
   ```

   `frontend-dev` runs `npm start` (the dev server plus a watching type check);
   `npm run dev` starts the dev server alone. The user interface is at
   http://localhost:3000; API calls and WebSocket are proxied to the backend.

   The dev server reads `elza-react/.env.development.local`:

   ```
   ENDPOINT=http://localhost:8080
   ```

   `ENDPOINT` is the backend the proxy talks to (default
   `http://localhost:8080`). `KERBEROS=true` makes the proxy sign in to a
   backend protected by Kerberos (SPNEGO); `KERBEROS_SERVICE` overrides the
   service name.

5. **Packages.** An empty database has no rules. Import at least
   `package-cz-base/target/package-cz-base-<version>.zip` and
   `rules-cz-zp2015/target/rules-cz-zp2015-<version>.zip` in *Administration >
   Package management*, or copy the ZIP files to the `dpkg/` subdirectory of
   the working directory, which is loaded at every start. Then import the
   institutions (`package-cz-base/src/all-institutions-import.xml`, *Archival
   entity import* in the *Archival entities* module) to be able to create
   funds. The international packages (`package-isaar-cpf`, `rules-en-isadg`)
   and the rules of automatic loading are described in the administration
   guide, *Loading the packages*.

On an empty database the application offers the creation of the first
administrator.

## Tests

* Backend: `mvn -pl elza-core test` (a single class: `-Dtest=ClassName`).
* Frontend (in `elza-react`): `npm test`, `npm run test:watch`,
  `npm run test:coverage`; see [elza-react/README.md](elza-react/README.md).

## Development conventions

* Database changes only through Liquibase changesets in `elza-core`
  (`src/main/resources/db/changelog`).
* REST API changes start in the OpenAPI spec
  `elza-core/src/main/resources/rest/elza-openapi.yml`; the TypeScript client in
  `elza-react/src/api/generated` is generated from it and is never edited by hand.
* Design decisions are recorded in [docs/design](docs/design).

## Setting the version

```
mvn -U versions:set -DnewVersion=3.0-SNAPSHOT
```

## License

See [LICENSE.txt](LICENSE.txt) and [NOTICE](NOTICE).
