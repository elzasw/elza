LightComp Elza Distribution
---------------------------

Binární distribuce aplikace Elza pro instalaci 
do aplikačního serveru.

Podporované systémy:
* Java 1.8 a vyšší
* Microsoft Windows nebo Linux
* Apache Tomcat 8.0 a vyšší
* Databáze:
 * Postgresql
 * Microsoft SQL Server

Postup instalace Elza
=====================

Předpoklady:
 - nainstalována databáze Postgresql

Vhodné znalosti:
 - způsob administrace Apache Tomcat

1) Nainstalujte Apache Tomcat. Ke stažení zde: https://tomcat.apache.org/
   Elza v tuto chvíli musí být nainstalována jako kořenová aplikace v Tomcat

2) Vytvořte uživatele v Postgres pro přístup k databází. Například: 'elza'

3) Vytvořte prázdnou databázi, vlastníkem databáze nastavte uživatel z bodu 2)
   create database elza owner = 'elza';

4) Přidejte do databáze možnost používat rozšíření PostGIS:
   create extension postgis;

5) Připravte pracovní adresář pro Elzu. Do adresáře se budou ukládat 
   pravidla popisu, tiskové šablony, vygenerované tisky a další.
   Například: D:\Elza\work

6) V adresáři server je vzorový soubor elza-ui.yaml. Ten otevřete a upravte 
   dle nastavení v předchozích bodech. Minimálně je nutné nastavit:
   - připojení k databázi (sekce elza: data:)
   - cestu k pracovnímu adresáři (workingDir:)

7) Upravený soubor elza-ui.yaml nahrajte do souboru ROOT.war do adresáře
   WEB-INF/classes 
   Soubor ROOT.war je standardní ZIP souboru a je možné s ním pracovat pomocí
   běžných nástrojů pro práci se ZIP soubory. Ve Windows je nutné mu před úpravou
   změnit koncovku na .zip.

8) V připravené instalaci Tomcat smažte adresář webapps/ROOT a upravený ROOT.war
   nahrajte do adresáře webapps

9) Spusťte Tomcat
   V případě správné konfigurace dojde k vytvoření tabulek v databázi a nastartování
   aplikace.

10) Prvotní konfigurace Elza
    - ve správě balíčků do aplikace nahrajte balíček packages/package-cz-base.zip 
    - ve správě balíčků do aplikace nahrajte balíček packages/rules-cz-zp2015.zip
    - proveďte import paměťových institucí volbou Osoby / Import osob 
      ze souboru data/all-institutions-import.xml
