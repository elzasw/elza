DaoTestBench
=================

DaoTestBench je aplikace, která umožňuje volat WSDL rozhraní Elza a
zasílat do ní DAO. Aplikace umožňuje testovat rozhraní pro úložiště
digitalizátů a digitalizační linku.

Aplikace se ovládá pomocí HTTP požadavků, data jsou uložena na disku.
Součástí aplikace je vzorová konfigurace (application.yaml.template) a
vzorové úložiště digitalizátů (demostorage).

DaoTestBench standardně běží na portu 8085.


Nastavení na straně Elza
-------------------------

Na straně Elza je nuté definovat úložiště digitalizátů s těmito parametry:

FileUrl: http://localhost:8085/file/{daoPackageCode}/{daoCode}/{code}
Zasílání upozornění: Ne
Kód: repo
Název: DaoTestBench
URL: http://localhost:8085/repo/ws
Kód ELZA: elza

Vytvoření prázdného AS s interním kódem DaoTestFund1


Volání příkazů serveru - vzor
-----------------------------

Odeslání připraveného balíčku ze serveru do Elzy (prázdný balíček):
curl -v -X POST http://localhost:8085/import/1/system/elza

Odebrání prázdného balíčku
curl -v -X GET http://127.0.0.1:8085/daoservice/remove/1/system/elza

Odeslání připraveného balíčku ze serveru do Elzy (balíček s jedním dao):
curl -v -X POST http://localhost:8085/import/2/system/elza

Odeslání připraveného balíčku ze serveru do Elzy (balíček s jedním dao typu Level):
curl -v -X POST http://localhost:8085/import/3/system/elza


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

Struktura souborů a vzorová konfigurace serveru
-----------------------------------------------
storage_file_structure.md

1) externí systém (adresa, uživatel, heslo):
	formát:		YAML, Map<String,Object>
	umístění:	${basePath}/${repositoryIdentifier}/external-systems-config.yaml
	pozn.:		jeden záznam vyjadřuje
       systemIdentifier: 
         address: http://domain/services/
         user: xx
         password: pass

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
