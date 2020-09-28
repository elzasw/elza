Elza Distribution
---------------------------

Binární distribuce aplikace Elza pro samostatnou instalaci.

Podporované systémy:
* Java 1.8 a vyšší
* Microsoft Windows nebo Linux
* Databáze:
 * Postgresql
 * Microsoft SQL Server

V adresáři `server` je uložen hlavní soubor JAR a vzorová konfigurace.
Do souboru JAR je vložen webový server a aplikaci je možné spouštět
bez nutnosti instalace a konfigurace samostatného aplikačního serveru.

Podporované možnosti instalace jsou:
 * Linux/Unix - init.d služba (System V)
 * Linux/Unix - systemd služba
 * služba systému Windows

Podrobný seznam možností konfigurace zde:
https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html#deployment-install

Postup instalace Elza
=====================

Předpoklady:
 - nainstalována databáze Postgresql

Aplikace Elza vyžaduje instalační a pracovní adresář.
Příklad cílového rozložení na disku:
..../elza/server - adresář s nainstalovanou aplikací (JAR + konfigurační soubor)
..../elza/work - pracovní adresář

1) Vytvořte uživatele v Postgres pro přístup k databází. Například: 'elza'

2) Vytvořte prázdnou databázi, vlastníkem databáze nastavte uživatel z bodu 1)
   create database elza owner = 'elza';

3) Přidejte do databáze možnost používat rozšíření PostGIS:
   create extension postgis;

4) Připravte pracovní adresář pro Elzu. Do adresáře se budou ukládat 
   pravidla popisu, tiskové šablony, vygenerované tisky a další.
   Například: D:\Elza\work
   Je vhodné vytvořit rovnou adresář pro zápis logu, např: D:\Elza\work\log

5) Připravte instalační adresář pro Elzu. Do adresáře zkopírujte
   JAR a vzorovou konfiguraci.
   Příklad výsledné podoby:
   D:\Elza\server\elza.jar
   D:\Elza\server\config\elza.yaml

6) V adresáři servery je vzorový soubor elza.yaml. Ten otevřete a upravte 
   dle nastavení v předchozích bodech. Minimálně je nutné nastavit:
   - připojení k databázi (sekce elza: data:)
   - cestu k pracovnímu adresáři (workingDir:)

7) Ověřovací spuštění aplikace Elza
   Pro ověření lze aplikaci přímo spustit příkazem
   java -jar elza.jar

8) Nakonfigurujte automatické spouštění jako služba
   a službu spusťte

9) Prvotní konfigurace Elza
    - ve správě balíčků do aplikace nahrajte balíček packages/package-cz-base.zip 
    - ve správě balíčků do aplikace nahrajte balíček packages/rules-cz-zp2015.zip
    - proveďte import paměťových institucí volbou Archivní entity / Import 
      ze souboru data/all-institutions-import.xml
