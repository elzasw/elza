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

Po vyplnění parametrů v **_elza.cfg_** je instalace hotova. 
- VM: Popsat parametry

Signature file pro DROID lze stáhnout na adrese https://www.nationalarchives.gov.uk/aboutapps/pronom/droid-signature-files.htm
- VM: Tohle jsem nepochopil, proč bych to chtěl?
