# Sestavení

Předpokládané softwarové vybavení:

* [Git 1.9+](https://git-scm.com/download/win)
* [Oracle JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Apache Maven 3.2.+](https://maven.apache.org/download.cgi)

## Získání zdrojových kódů
Příkaz:
```
git clone https://elza-developer@bitbucket.org/tacr/elza-release.git
```

## Sestavení
Před sestavením modulů je nutné mít v mvn repository ostatní moduly projektu DSpace. Proto je potřeba stáhnout a sestavit kompletní projekt
DSpace. Pak je teprve možné sestavit integrační moduly.

* clone projektu [DSpace](https://github.com/DSpace/DSpace.git)
* přepnutí na větev dspace-6_x
* sestavení:
```
mvn install
```

Nyní je možné sestavit integrační moduly. Sestavení se provádí pro každý modul zvlášť. V adresářích **_dspace\modules\xmlui_** a **_dspace-elza_** spuštěním příkazu:
```
mvn clean package
```
Ve složce /target v každém z adresářů vzniknou soubory **_xmlui-6.4-SNAPSHOT.war_** a **_dspace-elza-6.4-SNAPSHOT.war_**.

# Návod na instalaci modulů do DSpace
- V nainstalovaném DSpace je potřeba najít soubor _**local.cfg**_
- V souboru **_local.cfg_** zjistit adresář který je nastavený v proměnné _**dspace.dir**_
- Pro import digitalizátů je potřeba znát e-mail existujícího uživatele s právy na zápis
  - pokud žádný takový uživatel neexistuje je možné ho vytvořit příkazem  
    ```${dspace.dir}/bin/dspace create-administrator```
- Vypnout tomcat na kterém běží DSpace a po provedení změn ho zase spustit 
## Nastavení tomcatu
- Ve složce **_tomcat\bin_** otevřít .exe soubor s parametry služby JV: Je nějaké pravidlo podle kterého se soubor jmenuje?
- Na záložce Java vložit na nový řádek do pole Java Options následující:
  - -Ddspace.dir=c:\dspace\
  - -Dorg.apache.cxf.stax.allowInsecureParser=1
- Uložit
## Kopie souborů 
- Naše moduly budeme distribuovat jako **_war_** soubory
- Soubory se po sestavení budou nacházet v aresářích **_dspace\modules\xmlui\target_** a **_dspace-elza\target_**  a budou
se jmenovat **_xmlui-6.4-SNAPSHOT.war_** a **_dspace-elza-6.4-SNAPSHOT.war_**
- Soubory je potřeba nakopírovat do adresáře **_webapps_** v tomcatu a přejmenovat je na **_xmlui.war_** a **_elza.war_**
## Nastavení modulu ELZA
- Do adresáře **_${dspace.dir}/config/modules_** nahrát **_elza.cfg_**
- Na konec souboru **_dspace.cfg_** je potřeba vložit vazbu na **_elza.cfg_**
  - ```include = ${module_dir}/elza.cfg```

Vyplnit parametry v **_elza.cfg_** 
- Připojení k aplikaci ELZA, **_povinné_**
  - **_elza.base.url_** - URL, na kterém běží aplikace ELZA
  - **_elza.base.username_** - uživatel z aplikace ELZA, pod kterým se provádějí všechny operace v ELZA. VM: Musí mít nějaké oprávnění nastavené?
  - **_elza.base.password_** - heslo uživatele
- Kód externího systému v ELZA, **_povinné_**
  - **_elza.repositoryCode_** - musí zde být stejná hodnota jako je v ELZA uvedena v atributu kód u externího systému
  - v ELZA se nastavuje v Administrace -> Externí systémy. Zde se buď upraví existují záznam nebo se založí nový.
- Pracovní adresář pro import, **_povinné_**
  - **_elza.daoimport.dir_** - VM: Přidat doporučení, jak nastavit 
- E-mail osoby založené v DSpace pod kterou spouští import, **_povinné_**
  - **_elza.daoimport.email_** - e-mail osoby z DSpace s právy na zápis. Může být e-mail administrátora kterého jsme založili na začátku.
- Cesta k souboru se vzory typů souborů, pokud nebude nastaven nelze určit MimeType importovaných souborů
  - **_elza.droid.signatureFile_** - Příklad: elza.droid.signatureFile=/DROID_SignatureFile_V95.xml
  - soubor lze stáhnout na adrese https://www.nationalarchives.gov.uk/aboutapps/pronom/droid-signature-files.htm
- Seznam MimeType souborů které lze importovat. Pokud je seznam prázdný importují se všechny soubory. Je možné uvést více hodnot oddělených mezerami.
  - **_elza.daoimport.supportedMimeTypes_** - Příklad: elza.daoimport.supportedMimeTypes=image/jpg image/jpeg
- Seznam MimeType souborů které se mají před importem konvertovat na jiný typ. Je možné uvést více hodnot oddělených mezerami.
  - **_elza.daoimport.convert.supportedMimeTypes_** - Příklad: elza.daoimport.convert.supportedMimeTypes=image/tif image/tiff
- Šablona příkazu pro konverzi souborů
  - **_elza.daoimport.convert.command_** - Příklad: elza.daoimport.convert.command=“conv” {input} {output}
  - “conv” - nahradit příkazem s parametry na spuštění externí konverzní aplikace. Výběr a zprovoznění konverzní aplikace je na zákazníkovi. Lze použít například aplikaci ImageMagick.
  - v příkazu pro konverzi lze použít proměnné {input} a {output} za které DSpace dosadí název vstupního a výstupního souboru
- Výraz pro vygenerování názvu DAO z metadat
  - **_elza.daoimport.dao.name_** - Příklad: elza.daoimport.dao.name=${dc.title.alternative} - ${dc.creator}


## Spárování entit v aplikacích DSpace a ELZA 
### ELZA
- Vytvoření externího systému
- V ELZA se nastavuje v Administrace -> Externí systémy -> Přidat systém
  - položka **_Třída_** - vybrat hodnotu **_Uložiště digitalizátů_** 
  - položka **_DaoURL_** - zadat hodnotu **_http(s)://server:port/xmlui/handle/{label}_**
  - položka **_FileURL_** - zadat hodnotu **_http(s)://server:port/xmlui/handle/{code}_**
  - položka **_ThumbnailURL_** - zadat hodnotu **_http(s)://server:port/xmlui/handle/{code}_**
  - položka **_Zasílání upozornění_** - vybrat hodnotu **_Ano_**
  - položka **_Název_** - zadat název externího systému (například: DSpace)
  - položka **_Kód_** - zadat kód externího systému (například: dspace), musí být stejný jako je v souboru **_elza.cfg_**, v proměnné **_elza.repositoryCode_** 
  - položka **_URL_** - zadat hodnotu **_http(s)://server:port/elza/ws/_**
### DSpace
- Komunity
  - Pro každý archiv se v DSpace musí vytvořit komunita
  - V atributu **_Krátký popis_** v DSpace musí být **_číslo archivu_**
- Kolekce
  - Pro každý archivní soubor v ELZA do kterého chceme přes DSpace importovat digitalizáty musíme v DSpace vytvořit kolekci
  - V atributu **_Krátký popis_** v DSpace musí být **_kód archivního souboru_** nebo jeho **_identifikátor_** 
