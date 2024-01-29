# ELZA - Elektronické zpracování archiválií

## Aktualizace datového schématu

Ke změnám datového schématu je využit framework Liquibase. Změnové soubory ve formátu XML jsou umístěny v core. 
Při startu aplikačního jsou automaticky provedeny nezpracované změnové dávky. Způsob zápisu XML viz dokumentace
http://www.liquibase.org/documentation/xml_format.html. Pokud se jedná o složitější změnu, je možné změnu realizovat 
v Javě, propojení XML a změnové metody je ukázáno na příkladu DbChangelog_1_0.java v programátorské dokumentaci.


## Vize způsobu nasazení

Aplikaci distribuovat ve 3 variantách formou zip archivu. Nový artefakt elza-zip sestaví pro obě varianty pomocí
maven-assembly-plugin zip archiv podle různých descriptorů elza-zip, elza-exe a elza-war.

První varianta zip bude obsahovat embedded aplikační server Tomcat (přes závislosti), závislosti připravené pomocí
maven-dependency-plugin:copy-dependencies, souštěcí skripty (.bat, .sh), informace o registraci služby do systémů
Linux (init.d a systemd), dokumentaci projektu (spuštění a konfigurace).

Druhá varianta exe bude sestavovat instalátor (http://nsis.sourceforge.net), jehož obsah bude totožný se zip variantou.
Instalátor může nabídnout uživateli konfiguraci základních parametrů (složka, portt,...). Dále instalátor zajístí 
registraci služby do systému Windows (https://nssm.cc), její spuštění a otevře uživateli prohlížeč s aplikací.

Třetí varianta war bude obsahovat war archiv artefaktu elza-war, dokumentaci projektu (spuštění a konfigurace) a
informace o způsobu nasazení do doporučených aplikačních server (Tomcat, Jetty, JBoss, GlassFish).

Dále je potřeba po startu AS detekovat první start aplikace a nabídnout uživateli přes webové rozhranní prvotní
konfigurace připojení k DB včetně ověření připojení před spuštěním dbupgradu Liquibase. Nejvhodněji zakomponovat 
do celkové webové konfigurace aplikace, realizované pomocí lehkého webframeworku.

Aktualizace všech variant je možné realizovat vytvořením nástroje který rozpozná variantu serveru, provede stažení 
nové verze (zip/war) a její rozbalení. Dále musí zajistit restart AS.

## Nástroj na visualizaci aktuálního datového modelu

Současně s aplikací je potřeba udržovat aktuální datový model ve formě diagramu, včetně popisků tabulek a sloupců.
Vhodný nástroj či kombinace nástrojů, který zajistí visualizaci musí být volně dostupný a nejlépe opensource,
pro uržitelnost případného dalšího rozvoje.

Model bude dokumentován formou javadoc popisu v entitách, nástroj liquibase bude rozšířen pro přenos popisu
do komentářů tabulek a sloupců v databázi. Tyto popisy jsou již další nástroje schopny reprezentovat.

Jako vhodný nástroj pro vizualizaci považuji SchemaCrawler (http://sualeh.github.io/SchemaCrawler), který nabízí
vizuální generování modelu dle databáze formou html textu, diagramu v obrázku/pdf, diff rozdíly dvou databází.
Ukázka vygenerovaného diagramu se nachází v data-model.pdf, v současné podobě je vygenerována bez komentářů.
Příkaz `./schemacrawler.sh -server=postgresql -host=... -database=elza -user=... -password=... -infolevel=standard \
 -command=graph -outputformat=pdf -outputfile=data-model.pdf`.

