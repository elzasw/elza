Struktura souborů a vzorová konfigurace serveru
-----------------------------------------------
storage_file_structure.md

Nastavení v Elza - vzorové nastavení odpovídající vzorovému nastavení dao testovacímu serveru
---------------------------------------------------------------------------------------------
1) Digitalizační linka
Třída: Digitalizační linka
Kód: dl-test-kod
Název: dl-test-nazev
URL: http://localhost:8085/repo/ws/
Username:
Heslo:
Kód ELZA: digilinka-local

2) Úložiště digitalizátů
Třída: Uložiště digitalizátů
DaoURL:
FileURL:
ThumbnailURL:
Zasílání upozornění: Ne
Kód: repo
Název: u-test-nazev
URL: http://localhost:8085/repo/ws/
Username:
Heslo:
Kód ELZA: uloziste-local

Nastavení pro dao
-----------------

1) adresa na externí systém:
	formát:		YAML, Map<String,String>
	umístění:	${basePath}/${repositoryIdentifier}/external-systems-config.yaml
	pozn.:		jeden záznam vyjadřuje "systemIdentifier: http://domain/services/"

2) konfigurace package:
	formát:		YAML, cz.tacr.elza.dao.bo.resource.DaoPackageConfig
	umístění:	${basePath}/${repositoryIdentifier}/packages/{packageIdentifier}/package-config.yaml
	pozn.:		důležitý především "fundIdentifier: fund_uid"

3) konfigurace dao:
	formát:		YAML, cz.tacr.elza.dao.bo.resource.DaoConfig
	umístění:	${basePath}/${repositoryIdentifier}/packages/{packageIdentifier}/{daoIdentifier}/dao-config.yaml
	pozn.:		# důležitý především "didIdentifier: did_uid"
				# jakýkoliv soubor v dao složce je považován za platný DaoFile (mimo konfigurace a příznaku smazání)

Pravidla
--------
1 Package -^- (0..1) AS
1 DAO -^-  (0..1) Jednotka Popisu - bud je nebo neni k necemu prilinkovana

Poznamky pro implementaci
-------------------------
1. Radkova utilita - dtbCmd
(Faze 1)
- umoznuje volat funkce Elza, zakladem je moznost importovat do Elzy neco z uloziste volanim metody "Import"

(Faze 2)
- umoznuje zaslat odpoved na prijaty pozadavek na digitalizaci - chyba DigitizationRequestRevoked
- umoznuje zaslat odpoved na prijaty pozadavek na digitalizaci - ok DigitizationRequestFinished

2. "Uloziste" / dtbStorage
(Faze 1)
- poskytuje prislusne rozhrani
- obsluhuje "dao" ulozene ve vhodne strukture na disku
  - napr. Package <ID> /Dao <ID>
- fronta daoDestrRequest
  - z UI/cmd muzu odpovidat -> ano = vymazu
                         -> zamitnuto = nedelam nic
- fronta daoTransferRequest
  - z UI/cmd muzu odpovidat -> ano = "prusunu" do jine slozky, resp. fakticky se nemusi se stat nic
                         -> zamitnuto = nedelam nic

(Faze 2)
3. "Digitalizacni linka" / dtbDigitizationService
- frona Pozadavku - kazdy pozadavek je zarazen a je mu prideleno Id
   - lze take implementovat pozastaveni prijimani pozadavku a jejich odmitani s chybou
