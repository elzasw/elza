# Návod na instalaci modulů do DSpace
- V nainstalovaném DSpace je potřeba najít soubor _**local.cfg**_
- V souboru **_local.cfg_** zjistit adresář který je nastavený v proměnné _**dspace.dir**_
## Kopie souborů
- Naše moduly budeme distribuovat jako **_war_** soubory - VM: Kde je vezmu? Popsal bych i to, jak se budou jmenovat...
- Soubory je potřeba rozbalit do adresáře **_${dspace.dir}/webapps_** do adresářů **_elza_** a **_xmlui_** - VM: Asi přeformulovat - v adresáři ... vytvořím podadresáře X a Y a do nich rozbalím warka A a B
## Nastavení modulu ELZA
- Do adresáře **_${dspace.dir}/config/modules_** nahrát **_elza.cfg_**
- Na konec souboru **_dspace.cfg_** je potřeba vložit vazbu na **_elza.cfg_**
  - ```include = ${module_dir}/elza.cfg```

Vyplnit parametry v **_elza.cfg_** 
- Připojení k aplikaci ELZA
  - elza.base.url - URL, na kterém běží aplikace ELZA
  - elza.base.username - uživatel ELZA, pod kterým se provádějí všechny operace v ELZA. VM: Musí mít nějaké oprávnění nastavené?
  - elza.base.password - heslo uživatele
- Kód externího systému v ELZA
  - elza.repositoryCode - VM: Kde se v ELZA nastavuje, doplnit
- Pracovní adresář pro import
  - elza.daoimport.dir - VM: Přidat doporučení, jak nastavit 
- E-mail osoby založené v DSpace pod kterou spouští import
  - elza.daoimport.email - VM: Asi doplnit?
- Cesta k souboru se vzory typů souborů, pokud nebude nastaven nelze určit MimeType importovaných souborů
  - elza.droid.signatureFile - Příklad: elza.droid.signatureFile=/DROID_SignatureFile_V95.xml VM: Doplnit, kde stáhnout
- Seznam MimeType souborů které lze importovat. Pokud je seznam prázdný importují se všechny soubory. Je možné uvést více hodnot oddělených mezerami.
  - elza.daoimport.supportedMimeTypes - Příklad: elza.daoimport.supportedMimeTypes=image/jpg image/jpeg
- Supported file types for convert VM: Přeložit
  - elza.daoimport.convert.supportedMimeTypes - Příklad: elza.daoimport.convert.supportedMimeTypes=image/tif image/tiff
- Convert files application commnand template
  - elza.daoimport.convert.command= “conv” {input} {output} - VM: Doplnit proč a jak?

Signature file pro DROID lze stáhnout na adrese https://www.nationalarchives.gov.uk/aboutapps/pronom/droid-signature-files.htm
- VM: Přidat výše
