# ELZA - Elektronické zpracování archiválií

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