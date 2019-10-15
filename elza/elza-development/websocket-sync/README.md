# Úvod
Tento demonstraèní projekt je návrhem øešení zpùsobu komunikace mezi ELZA clientem (webová aplikace) a ELZA serverem.

Východiska:
 * zmìny jsou na server z klienta zasílány ihned pøi jejich detekci, tj. opuštìní editovaného pole
 * na serveru jsou využívány optimistické zámky s cílem zamezit vzniku nechtìného pøepsání informací zadaných jiným uživatelem
 * na jednotlivé klienty jsou zasílány již nyní notifikace o zmìnách pomocí WebSocketu

Problémy stávajícího øešení (do záøí 2016):
 * pøi rychlé editaci dochází k synchronizaèní chybì
 * pøi vzniku synchronizaèní chyby (i jiné chyby) není na klientovi ošetøeno její øešení, resp. zajištìn konzistentní stav. Model se nevrací do pøedchozího stavu
 
Dùvody vzniku synchronizaèních chyb:
  * prohlížeè pro odesílání HTTP requestù využívá nìkolik vláken, není tím pádem zaruèeno poøadí jejich pøijetí na serveru
  * server zpracovává pøijaté requesty více vláknovì a není zaruèeno jejich uspoøádání (ani z hlediska zahájení èi ukonèení)

# Principiální návrh øešení
Cílem je zajistit uspoøádání požadavkù od jedné klientské aplikace a zaruèit jejich zpracování v tom poøadí v jakém vznikly. 
Toho lze dosáhnout tak, že bude zaruèeno:
 * Požadavky jsou pøenášeny v tom poøadí v jakém vznikly
 * Požadavky jsou zpracovány v tom poøadí v jakém vznikly

Požadavky lze pøenášet z klienta na server pomocí WebSocketù. Tím, že se využije tohoto spojení dojde k serializaci 
odesílaných požadavkù na již klientovi. Souèasnì je zaruèeno, že požadavky na server dorazí ve správném poøadí. Druhou èástí 
je zajištìní vyøizování požadavkù od jednoho klienta na serveru serializovanì. Toho lze dosáhnout implementací vhodného plánovaèe,
který bude vyøizovat synchronnì požadavky pocházející z jedné klientské aplikace, tj. jednoho websocketu.

# Zpùsob øešení komunikace
Komunikace je založena na využití WebSocket protokolu v kombinaci se Stomp knihovnou. 
Oproti pùvodní implementaci je implementace založena na èistých WebSocketech tj. bez knihovny SockJS.

Stomp protokol je využíván ve verzi 1.2. Klientská strana je založena na knihovnì Stomp.js (https://github.com/ThoughtWire/stomp-websocket)

Využívané vlastnosti Stomp protokolu:
 * heartbeat - pro zajištìní živého kanálu
 * receipt - možnost zajištìní potvrzení zpracování požadavku na stranì serveru
 * error - v pøípadì vzniku výjimky pøi zpracování požadavku je zaslána zpráva error a dojde k odpojení klienta

Z klienta budou novì požadavky na zmìnu uzlu pøenášeny na server jako JSON requesty pøes WebSocket protokol. 
Zpracování ostatních požadavkù, kde nehrozí vznik synchronizaèních chyb mohou být pøenášeny stávajícím zpùsobem beze zmìny.

Minimální seznam operaci u nichž musí dojít ke zmìnì zpùsobu pøenosu požadavkù:
 * vznik jednotky popisu - øeší se napø. pøípad, kdy se zmìní JP a hned poté se pod ní založí nová JP - tyto operace chceme synchronnì
 * zmìna jednotky popisu
 * vymazání jednotky popisu
 * pøesun jednotky popisu

Logika na stranì klienta:
 * Klient odesílá požadavek pøes websocket a souèasnì si zvýší lokální èíslo verze na úrovni JP (nutno SYNCHRONIZOVAT !!!)
 * Pokud bude chtít klient dostávat potvrzení vyøízení požadavku, tak musí mít jejic counter a odesílat na server atribut receipt v hlavièce požadavku
 * Pokud pøijde ze serveru notifikace o nové verzi JP a pokud je verze menší rovna než verze na klientovi, tak se notifikace ignoruje
 * Pokud pøijde ze serveru notifikace o nové verzi JP a pokud je verze vìtší než verze na klientovi, tak se JP aktualizuje ze serveru
 * Pokud pøijde error zpráva ze serveru pøes WS, tak dojde k ukonèení WS (zajistí Stomp.js) a zobrazení chyby. 
   Klient musí souèasnì zneplatnit všechny vyžádané uzly, znovu se pøipojí k WS a obnoví informace o JP

# Ukázkové øešení
Demonstraèní projekt je pøipraven do stavu, kdy je možné jeho tøídy pøímo pøenést do aplikace a zaèít je využívat.
Package core - obecné tøídy, lze plnì využít ve stávající podobì
Package fund - ukázka reálného nasazení v aplikaci a vytvoøení pøíslušných endpoint

# Budoucí zmìny
Pøepracování WebSocketTaskProcessor, aby využíval ThreadPool
Pro využití Scope("session") bude nutné využít abstrakci nad HttpSession, napøíklad: Spring session
 * session je pak platná i po dobu websocket komunikace, viz: https://spring.io/blog/2014/09/16/preview-spring-security-websocket-support-sessions