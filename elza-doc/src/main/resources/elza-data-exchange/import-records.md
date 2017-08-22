Párování rejstøíkù
------------------

# Postup párování:
1) UUID
2) ExternalId, ExternalSystemId

# Aktualizace záznamu:
Porovná se datum aktualizace xml a db záznamu, pokud je datum v xml novìjší, 
tak se aktualizuje celý záznam v db tj. vèetnì UUID a ExternalId (esc).

# Zpùsob zpracování:
0) Kontrola oprávnìní (zápis) na scope pro vkládání záznamù
1) Ètení po záznamu z XML
	- Je nutná kontrola zda-li typ rejstøíku je zapisovatelný (add_record).
	- Pokud vytváøím podøízený záznam je nutná kontrola, zda-li je typ rejstøíku hierarchický.
2) Vytvoøí se seznam nových záznamù k uložení do db 
3) Vyhledají se existující záznamy pro aktualizaci podle UUID (ve velikosti dávky)
	- uživatel musí mít oprávnìní na scope napárovaných rejstøíkù
	a) Aktualizace záznamù, kde xml datum <= db datum
		- pøemapuji xml recordId na párované db id
	b) Aktualizace záznamù, kde xml datum (nebo není definováno) > db datum 
		- pøemapuji xml recordId na párované db id
		- smazat reg_coordinates, reg_variant_record
		- nalzené id záznamù se pøenesou do záznamù v dávce (update)
	c) Nenapárovnáno
4) Seznam se v dávce uloží do db (výstupem jsou id záznamù).
	- nenapárované se založí (viz. 3.c)
	- aktualizované se aktualizují (viz. 3.b)
5) Vyhledají se existující záznamy pro aktualizaci podle ExternalId (vrací: id, lastUpdate, scope, recordType).
	- pro vstup odfiltrujeme 
6) Uživatel musí mít oprávnìní na scope napárovaných rejstøíkù (kontrola oprávnìní na zápis dle vrácených scope)
7) Aktualizace záznamù, kde xml datum <= db datum 
	- pøemapuji xml recordId na párované db id
	- smažu vložené záznamy z xml, které se pøemapovaly
	- vložení potomci se musí aktualizovat na nová id
8) Aktualizace záznamù, kde xml datum (nebo není definováno) > db datum 
	- aktulizace záznamu (smazat reg_coordinates, reg_variant_record; pøepsat hodnoty v reg_record)
          Impl. poznámka: zpoèátku øešit jen jako sadu update where id = ..., èasem lze zlepšit pomocí left join
	- vymazání vložených záznamù z XML, které jsou již pøepsány do pùvodních hodnot
9) Vložení GPS
10) Vložení variant records
11) V pamìti zùstane minimálnì: 
   record: xmlId, dbId, typRejstøíku
10) Kontrola shoda typu rejstøíku s rodièem (pokud rodiè existuje).
   - dohledání rodièe v mapì dle bodu 9)
	
pøíklad:
xml:
        id=0, uuid=U00, time=0, val=Zem
	id=1, uuid=U10, time=0, val=ÈRx
	id=2, uuid=U20, time=1, val=Teplice, pid=1
	id=3, uuid=U30, time=1, val=Nová Ves, pid=2
db:
	id=1000, uuid=U00, time=1, val=Zemì
	id=1001, uuid=U10, time=1, val=ÈR
	id=1002, uuid=U20, time=0, val=Ústí, pid=1001
	id=1003, uuid=U30, time=0, val=NováVes, pid=1002
	
Simulace)
mem: 
	id=0, dbId = 1007, uuid=U00, time=0, val=Zem
	id=1, dbId = 1004, uuid=U10, time=0, val=ÈRx
	id=2, dbId = 1005, uuid=U20, time=1, val=Teplice, pid=1
	id=3, dbId = 1006, uuid=U30, time=1, val=Nová Ves, pid=2
db:
	id=1000, uuid=U00, time=1, val=Zemì
	id=1001, uuid=U10, time=1, val=ÈR, pid=1000
	id=1002, uuid=U20, time=0, val=Ústí, pid=1001
	id=1003, uuid=U30, time=0, val=NováVes, pid=1002
	id=1004, uuid=U10, time=0, val=ÈRx, pid=1007
	id=1005, uuid=U20, time=1, val=Teplice, pid=1004
	id=1006, uuid=U30, time=1, val=Nová Ves, pid=1005
	id=1007, uuid=U00, time=0, val=Zem
3 = vrací - je v mem)
	id=1000, uuid=U00, time=1, val=Zemì
	id=1001, uuid=U10, time=1, val=ÈR
      	id=1002, uuid=U20, time=0, val=Ústí, pid=1001
	id=1003, uuid=U30, time=0, val=NováVes, pid=1002
 
Ovìøení bod 5)
mem: 
	id=0, dbId = 1007 -> 1000, uuid=U00, time=0, val=Zem
	id=1, dbId = 1004 -> 1001, uuid=U10, time=0, val=ÈRx
	id=2, dbId = 1005, uuid=U20, time=1, val=Teplice, pid=1
	id=3, dbId = 1006, uuid=U30, time=1, val=Nová Ves, pid=2
db:     delete in (1004, 1007)
        update set pid = 1000 where pid=1007
        update set pid = 1001 where pid=1004


Ovìøení bod 6)
db:     delete GPS where id in (1005, 1006)
        delete varNames where id in (1005, 1006)
        update record.... 1002  
        update record.... 1003
        delete where id in (1005, 1006)

Finální stav db:
	id=1001, uuid=U10, time=1, val=ÈR
	id=1002, uuid=U20, time=1, val=Teplice, pid=1001
	id=1003, uuid=U30, time=1, val=Nová Ves, pid=1002
