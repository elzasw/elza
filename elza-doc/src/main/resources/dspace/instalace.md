# Návod na instalaci modulů do DSpace
- V nainstalovaném DSpace je potřeba najít soubor _**dspace.cfg**_
- V souboru **_dspace.cfg_** zjistit adresář který je nastavený v proměnné _**dspace.dir**_
## Kopie souborů
- Naše moduly budeme distribuovat jako **_war_** soubory
- Soubory je potřeba rozbalit do adresáře **_${dspace.dir}/webapps_** do adresářů **_elza_** a **_xmlui_**
## Nastavení modulu ELZA
- Do adresáře **_${dspace.dir}/config/modules_** nahrát **_elza.cfg_**
- Na konec souboru **_dspace.cfg_** je potřeba vložit vazbu na **_elza.cfg_**
  - ```include = ${module_dir}/elza.cfg```

Po vyplnění parametrů v **_elza.cfg_** je instalace hotova. 

Signature file pro DROID lze stáhnout na adrese https://www.nationalarchives.gov.uk/aboutapps/pronom/droid-signature-files.htm
