# Úvod
Tento demonstrační projekt je návrhem řešení způsobu komunikace mezi ELZA clientem (webová aplikace) a ELZA serverem.

Východiska:
 * změny jsou na server z klienta zasílány ihned při jejich detekci, tj. opuštění editovaného pole
 * na serveru jsou využívány optimistické zámky s cílem zamezit vzniku nechtěného přepsání informací zadaných jiným uživatelem
 * na jednotlivé klienty jsou zasílány již nyní notifikace o změnách pomocí WebSocketu

Problémy stávajícího řešení (do září 2016):
 * při rychlé editaci dochází k synchronizační chybě
 * při vzniku synchronizační chyby (i jiné chyby) není na klientovi ošetřeno její řešení, resp. zajištěn konzistentní stav. Model se nevrací do předchozího stavu
 
Důvody vzniku synchronizačních chyb:
  * prohlížeč pro odesílání HTTP requestů využívá několik vláken, není tím pádem zaručeno pořadí jejich přijetí na serveru
  * server zpracovává přijaté requesty více vláknově a není zaručeno jejich uspořádání (ani z hlediska zahájení či ukončení)

# Principiální návrh řešení
Cílem je zajistit uspořádání požadavků od jedné klientské aplikace a zaručit jejich zpracování v tom pořadí v jakém vznikly. 
Toho lze dosáhnout tak, že bude zaručeno:
 * Požadavky jsou přenášeny v tom pořadí v jakém vznikly
 * Požadavky jsou zpracovány v tom pořadí v jakém vznikly

Požadavky lze přenášet z klienta na server pomocí WebSocketů. Tím, že se využije tohoto spojení dojde k serializaci 
odesílaných požadavků na již klientovi. Současně je zaručeno, že požadavky na server dorazí ve správném pořadí. Druhou částí 
je zajištění vyřizování požadavků od jednoho klienta na serveru serializovaně. Toho lze dosáhnout implementací vhodného plánovače,
který bude vyřizovat synchronně požadavky pocházející z jedné klientské aplikace, tj. jednoho websocketu.

# Způsob řešení komunikace
Komunikace je založena na využití WebSocket protokolu v kombinaci se Stomp knihovnou. 
Oproti původní implementaci je implementace založena na čistých WebSocketech tj. bez knihovny SockJS.

Stomp protokol je využíván ve verzi 1.2. Klientská strana je založena na knihovně Stomp.js (https://github.com/ThoughtWire/stomp-websocket)

Využívané vlastnosti Stomp protokolu:
 * heartbeat - pro zajištění živého kanálu
 * receipt - možnost zajištění potvrzení zpracování požadavku na straně serveru
 * error - v případě vzniku výjimky při zpracování požadavku je zaslána zpráva error a dojde k odpojení klienta

Z klienta budou nově požadavky na změnu uzlu přenášeny na server jako JSON requesty přes WebSocket protokol. 
Zpracování ostatních požadavků, kde nehrozí vznik synchronizačních chyb mohou být přenášeny stávajícím způsobem beze změny.

Minimální seznam operaci u nichž musí dojít ke změně způsobu přenosu požadavků:
 * vznik jednotky popisu - řeší se např. případ, kdy se změní JP a hned poté se pod ní založí nová JP - tyto operace chceme synchronně
 * změna jednotky popisu
 * vymazání jednotky popisu
 * přesun jednotky popisu

Logika na straně klienta:
 * Klient odesílá požadavek přes websocket a současně si zvýší lokální číslo verze na úrovni JP (nutno SYNCHRONIZOVAT !!!)
 * Pokud bude chtít klient dostávat potvrzení vyřízení požadavku, tak musí mít jejic counter a odesílat na server atribut receipt v hlavičce požadavku
 * Pokud přijde ze serveru notifikace o nové verzi JP a pokud je verze menší rovna než verze na klientovi, tak se notifikace ignoruje
 * Pokud přijde ze serveru notifikace o nové verzi JP a pokud je verze větší než verze na klientovi, tak se JP aktualizuje ze serveru
 * Pokud přijde error zpráva ze serveru přes WS, tak dojde k ukončení WS (zajistí Stomp.js) a zobrazení chyby. 
   Klient musí současně zneplatnit všechny vyžádané uzly, znovu se připojí k WS a obnoví informace o JP

# Ukázkové řešení
Demonstrační projekt je připraven do stavu, kdy je možné jeho třídy přímo přenést do aplikace a začít je využívat.
Package core - obecné třídy, lze plně využít ve stávající podobě
Package fund - ukázka reálného nasazení v aplikaci a vytvoření příslušných endpoint

# Budoucí změny
Přepracování WebSocketTaskProcessor, aby využíval ThreadPool
Pro využití Scope("session") bude nutné využít abstrakci nad HttpSession, například: Spring session
 * session je pak platná i po dobu websocket komunikace, viz: https://spring.io/blog/2014/09/16/preview-spring-security-websocket-support-sessions