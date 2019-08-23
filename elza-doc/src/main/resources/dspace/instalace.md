# Návod na instalaci modulů do DSpace
- V nainstalovaném DSpace je potřeba najít soubor _**local.cfg**_
- V souboru **_local.cfg_** zjistit adresář který je nastavený v proměnné _**dspace.dir**_
- Pro import digitalizátů je potřeba znát e-mail existujícího uživatele s právy na zápis
  - pokud žádný takový uživatel neexistuje je možné ho vytvořit příkazem  
    ```${dspace.dir}/bin/dspace create-administrator```
- Vypnout tomcat na kterém běží DSpace a po provedení změn ho zase spustit 
## Kopie souborů 
- Naše moduly budeme distribuovat jako **_war_** soubory - VM: Kde je vezmu? Popsal bych i to, jak se budou jmenovat...
- Soubory je potřeba nakopírovat do adresáře **_webapps_** v tomcatu
- JV: Celou kapitolu upravíme až budeme vědět jak budeme soubory distribuovat
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
