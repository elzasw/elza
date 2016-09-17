# �vod
Tento demonstra�n� projekt je n�vrhem �e�en� zp�sobu komunikace mezi ELZA clientem (webov� aplikace) a ELZA serverem.

V�chodiska:
 * zm�ny jsou na server z klienta zas�l�ny ihned p�i jejich detekci, tj. opu�t�n� editovan�ho pole
 * na serveru jsou vyu��v�ny optimistick� z�mky s c�lem zamezit vzniku necht�n�ho p�eps�n� informac� zadan�ch jin�m u�ivatelem
 * na jednotliv� klienty jsou zas�l�ny ji� nyn� notifikace o zm�n�ch pomoc� WebSocketu

Probl�my st�vaj�c�ho �e�en� (do z��� 2016):
 * p�i rychl� editaci doch�z� k synchroniza�n� chyb�
 * p�i vzniku synchroniza�n� chyby (i jin� chyby) nen� na klientovi o�et�eno jej� �e�en�, resp. zaji�t�n konzistentn� stav. Model se nevrac� do p�edchoz�ho stavu
 
D�vody vzniku synchroniza�n�ch chyb:
  * prohl�e� pro odes�l�n� HTTP request� vyu��v� n�kolik vl�ken, nen� t�m p�dem zaru�eno po�ad� jejich p�ijet� na serveru
  * server zpracov�v� p�ijat� requesty v�ce vl�knov� a nen� zaru�eno jejich uspo��d�n� (ani z hlediska zah�jen� �i ukon�en�)

# Principi�ln� n�vrh �e�en�
C�lem je zajistit uspo��d�n� po�adavk� od jedn� klientsk� aplikace a zaru�it jejich zpracov�n� v tom po�ad� v jak�m vznikly. 
Toho lze dos�hnout tak, �e bude zaru�eno:
 * Po�adavky jsou p�en�eny v tom po�ad� v jak�m vznikly
 * Po�adavky jsou zpracov�ny v tom po�ad� v jak�m vznikly

Po�adavky lze p�en�et z klienta na server pomoc� WebSocket�. T�m, �e se vyu�ije tohoto spojen� dojde k serializaci 
odes�lan�ch po�adavk� na ji� klientovi. Sou�asn� je zaru�eno, �e po�adavky na server doraz� ve spr�vn�m po�ad�. Druhou ��st� 
je zaji�t�n� vy�izov�n� po�adavk� od jednoho klienta na serveru serializovan�. Toho lze dos�hnout implementac� vhodn�ho pl�nova�e,
kter� bude vy�izovat synchronn� po�adavky poch�zej�c� z jedn� klientsk� aplikace, tj. jednoho websocketu.

# Zp�sob �e�en� komunikace
Komunikace je zalo�ena na vyu�it� WebSocket protokolu v kombinaci se Stomp knihovnou. 
Oproti p�vodn� implementaci je implementace zalo�ena na �ist�ch WebSocketech tj. bez knihovny SockJS.

Stomp protokol je vyu��v�n ve verzi 1.2. Klientsk� strana je zalo�ena na knihovn� Stomp.js (https://github.com/ThoughtWire/stomp-websocket)

Vyu��van� vlastnosti Stomp protokolu:
 * heartbeat - pro zaji�t�n� �iv�ho kan�lu
 * receipt - mo�nost zaji�t�n� potvrzen� zpracov�n� po�adavku na stran� serveru
 * error - v p��pad� vzniku v�jimky p�i zpracov�n� po�adavku je zasl�na zpr�va error a dojde k odpojen� klienta

Z klienta budou nov� po�adavky na zm�nu uzlu p�en�eny na server jako JSON requesty p�es WebSocket protokol. 
Zpracov�n� ostatn�ch po�adavk�, kde nehroz� vznik synchroniza�n�ch chyb mohou b�t p�en�eny st�vaj�c�m zp�sobem beze zm�ny.

Minim�ln� seznam operaci u nich� mus� doj�t ke zm�n� zp�sobu p�enosu po�adavk�:
 * vznik jednotky popisu
 * zm�na jednotky popisu
 * vymaz�n� jednotky popisu
 * p�esun jednotky popisu

Logika na stran� klienta:
 * Klient odes�l� po�adavek p�es websocket a sou�asn� si zv��� lok�ln� ��slo verze na �rovni JP (nutno SYNCHRONIZOVAT !!!)
 * Pokud bude cht�t klient dost�vat potvrzen� vy��zen� po�adavku, tak mus� m�t jejic counter a odes�lat na server atribut receipt v hlavi�ce po�adavku
 * Pokud p�ijde ze serveru notifikace o nov� verzi JP a pokud je verze men�� rovna ne� verze na klientovi, tak se notifikace ignoruje
 * Pokud p�ijde ze serveru notifikace o nov� verzi JP a pokud je verze v�t�� ne� verze na klientovi, tak se JP aktualizuje ze serveru
 * Pokud p�ijde error zpr�va ze serveru p�es WS, tak dojde k ukon�en� WS (zajist� Stomp.js) a zobrazen� chyby. 
   Klient mus� sou�asn� zneplatnit v�echny vy��dan� uzly, znovu se p�ipoj� k WS a obnov� informace o JP

# Uk�zkov� �e�en�
Demonstra�n� projekt je p�ipraven do stavu, kdy je mo�n� jeho t��dy p��mo p�en�st do aplikace a za��t je vyu��vat.
Package core - obecn� t��dy, lze pln� vyu��t ve st�vaj�c� podob�
Package fund - uk�zka re�ln�ho nasazen� v aplikaci a vytvo�en� p��slu�n�ch endpoint

# Budouc� zm�ny
P�epracov�n� WebSocketTaskProcessor, aby vyu��val ThreadPool
