# ELZA - Elektronické zpracování archiválií

## Sestavení a spuštění

Předpokládané softwarové vybavení:

* [Git 1.9+](https://git-scm.com/download/win)
* [Oracla JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Apache Maven 3.2.+](https://maven.apache.org/download.cgi)
* [Node.js 5.1.0+](https://nodejs.org/)

### Získání zdrojových kódů
Příkaz:
```
git clone https://open_marbes@bitbucket.org/tacr/elza-core-marbes-consulting.git
```

### Sestavení vč dokumentace
Příkaz:
```
mvn -Pjdoc,skiptest -Dmaven.javadoc.skip=false package
```
Ve složce /target/apidocs se nachází Javadoc dokumentace, otevřete index.html.
Pro sestavení dokumentace včetně UML diagramů modelu použijte profil jdocuml (je potřeba mít nainstalován Graphviz a v PATH spustitelný dot).


### Sestavení a spuštění embed (finální UI - React), určeno pro vývoj

* Nastavení připojení k DB
V projektu elza-web je nutné v adresáři config založit soubor elza.yaml s připojením k databázi.
Podrobnosti o nastavení viz sekce 'Databázové připojení'.

* Přeložení projektu (root celého projektu) (pokud již nebylo přeloženo kvůli dokumentaci - sekce 'Sestavení vč dokumentace')
mvn install -Pskiptest

* Aktualizace NPM balíčků (v adresáři elza-web)
```
mvn exec:exec -Pnpm-install
```

* Spuštění aplikačního serveru (v adresáři elza-web)
```
mvn spring-boot:run
```

* Spuštění serveru pro frontend (v adresáři elza-web)
```
mvn exec:exec -Pfrontend-dev
```

Po sestavení dojde ke spuštění embedded aplikačního serveru Tomcat.
Uživatelské rozhraní najdete na adrese http://localhost:8080 - není možné přistupovat z jiného počítače (sestavte a zprovozněte war v Tomcat).

#### Debug nástroje - redux-tools
V projektu elza-web v adresáři ```src/main/resources/frontend``` pokud vytvoříme soubor ```.dev``` a spustíme server frontendu, tak po stisku ctrl+h se zobrazí logovací nástroje redux storu.
Pomocí stisku ctrl+q se mění umístění panelu nástrojů.


### Sestavení a spuštění war v Tomcat
```
mvn -Pexec install
```

Sestavenou webovou aplikaci najdete v `elza-war/target/elza.war`.
Proveďte standardním způsobem deploy na aplikační server Tomcat verze 8.0.

### Spuštění v IDE (UI - React), určeno pro vývoj
Importujte projekt maven.
Pro spuštění jádra aplikace obsahující rest služby spusťte `cz.tacr.elza.ElzaCore`.
Pro spuštění uživatelského rozhranní spusťte `cz.tacr.elza.ElzaWebApp`.
Pro spuštění serveru pro frontend spusťte příkaz (v modulu elza-web):
```
mvn exec:exec -Pfrontend-dev
```
Uživatelské rozhranní najdete na adrese http://localhost:8080.

## Konfigurace a logování

Aplikace je konfigurována pomocí souboru `elza.yaml`. Umístění konfiguračních souborů:

* war Tomcat `webapps/elza/WEB-INF/classes`
* embed načítá v aktuální cestě nebo složce config

### Databázový server
Od MT14 je nutné mít připravený databázový server pro datový typ Geometry.

#### PostgreSQL
Pro tento DB server je vyžadována verze 9.1 a vyšší a rozšíření [PostGis](http://postgis.net/).
Po instalaci je nutné rozšíření zprovoznit přes ```psql``` nebo PgAdmin.
Následující příkaz zprovozní rozšíření nad vybranou databází.
```
CREATE EXTENSION postgis;
```
#### MySQL
Je vyžadována verze 5.0.1 - není potřeba žádná speciální instalace.

#### Oracle
Tento DB server je vyžadována verze Enterprise. Pouze verze Enterprise umí pracovat s datovým typem Geometry.

#### MSSQL
Je vyžadována verze MSSQL 2008 - není potřeba žádná speciální instalace.

### Databázové připojení
Do konfigurace vložte nastavení datového zdroje. Při prvním připojení se datové struktury vytvoří automaticky.
Nutno také vybrat dialekt dle použité databáze.

```
elza:
    data:
        url: jdbc:postgresql://server/databaze
        username: uzivatel
        password: heslo

spring:
  jpa:
      properties:
          hibernate:
              # PostgreSQL
              dialect: org.hibernate.spatial.dialect.postgis.PostgisDialect
              # H2
              #dialect: org.hibernate.spatial.dialect.h2geodb.GeoDBDialect
              # MySQL
              #dialect: org.hibernate.spatial.dialect.mysql.MySQLSpatialDialect
              # Oracle
              #dialect: org.hibernate.spatial.dialect.oracle.OracleSpatial10gDialect
              # MSSQL
              #dialect: org.hibernate.spatial.dialect.sqlserver.SqlServer2008SpatialDialect
```

### Logování událostí
V aplikačním serveru Tomcat ve složce logs jsou archivovány logy rozdělené po jednotlivých dnech.

* `elza-stdout-...` standardní informace událostí v aplikaci
* `elza-stderr-...` chybové hlášeí v aplikaci

### Nastavení úrovně logování
Nastavení se provádí v souboru `elza.yaml`. Pro různé balíčky a třídy jdou nastavit odlišné úrovně logování. Standardně
je nastaveno logování na úroveň INFO. 

Toto nastavení zapne logování volání metod na kontrolerech:

```
logging:
  level:
    cz.tacr.elza: DEBUG
```

Nastaveni logovani do souboru pro embed server

```
logging:
  file: ${elza.workingDir}/log/elza.log
```

### Import balíčku s pravidly
Po přeložení spuštění je potřeba naimportovat základní pravidla - soubor elza-war/target/elza-packages.zip

### Import institucí
Aby bylo možné vytvářen archivní fondy, je nutné importovat instituci v sekci Osoby - soubor elza-core/src/test/resources/institution-import.xml
