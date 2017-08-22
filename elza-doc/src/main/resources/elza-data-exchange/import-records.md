P�rov�n� rejst��k�
------------------

# Postup p�rov�n�:
1) UUID
2) ExternalId, ExternalSystemId

# Aktualizace z�znamu:
Porovn� se datum aktualizace xml a db z�znamu, pokud je datum v xml nov�j��, 
tak se aktualizuje cel� z�znam v db tj. v�etn� UUID a ExternalId (esc).

# Zp�sob zpracov�n�:
0) Kontrola opr�vn�n� (z�pis) na scope pro vkl�d�n� z�znam�
1) �ten� po z�znamu z XML
	- Je nutn� kontrola zda-li typ rejst��ku je zapisovateln� (add_record).
	- Pokud vytv���m pod��zen� z�znam je nutn� kontrola, zda-li je typ rejst��ku hierarchick�.
2) Vytvo�� se seznam nov�ch z�znam� k ulo�en� do db 
3) Vyhledaj� se existuj�c� z�znamy pro aktualizaci podle UUID (ve velikosti d�vky)
	- u�ivatel mus� m�t opr�vn�n� na scope nap�rovan�ch rejst��k�
	a) Aktualizace z�znam�, kde xml datum <= db datum
		- p�emapuji xml recordId na p�rovan� db id
	b) Aktualizace z�znam�, kde xml datum (nebo nen� definov�no) > db datum 
		- p�emapuji xml recordId na p�rovan� db id
		- smazat reg_coordinates, reg_variant_record
		- nalzen� id z�znam� se p�enesou do z�znam� v d�vce (update)
	c) Nenap�rovn�no
4) Seznam se v d�vce ulo�� do db (v�stupem jsou id z�znam�).
	- nenap�rovan� se zalo�� (viz. 3.c)
	- aktualizovan� se aktualizuj� (viz. 3.b)
5) Vyhledaj� se existuj�c� z�znamy pro aktualizaci podle ExternalId (vrac�: id, lastUpdate, scope, recordType).
	- pro vstup odfiltrujeme 
6) U�ivatel mus� m�t opr�vn�n� na scope nap�rovan�ch rejst��k� (kontrola opr�vn�n� na z�pis dle vr�cen�ch scope)
7) Aktualizace z�znam�, kde xml datum <= db datum 
	- p�emapuji xml recordId na p�rovan� db id
	- sma�u vlo�en� z�znamy z xml, kter� se p�emapovaly
	- vlo�en� potomci se mus� aktualizovat na nov� id
8) Aktualizace z�znam�, kde xml datum (nebo nen� definov�no) > db datum 
	- aktulizace z�znamu (smazat reg_coordinates, reg_variant_record; p�epsat hodnoty v reg_record)
          Impl. pozn�mka: zpo��tku �e�it jen jako sadu update where id = ..., �asem lze zlep�it pomoc� left join
	- vymaz�n� vlo�en�ch z�znam� z XML, kter� jsou ji� p�eps�ny do p�vodn�ch hodnot
9) Vlo�en� GPS
10) Vlo�en� variant records
11) V pam�ti z�stane minim�ln�: 
   record: xmlId, dbId, typRejst��ku
10) Kontrola shoda typu rejst��ku s rodi�em (pokud rodi� existuje).
   - dohled�n� rodi�e v map� dle bodu 9)
	
p��klad:
xml:
        id=0, uuid=U00, time=0, val=Zem
	id=1, uuid=U10, time=0, val=�Rx
	id=2, uuid=U20, time=1, val=Teplice, pid=1
	id=3, uuid=U30, time=1, val=Nov� Ves, pid=2
db:
	id=1000, uuid=U00, time=1, val=Zem�
	id=1001, uuid=U10, time=1, val=�R
	id=1002, uuid=U20, time=0, val=�st�, pid=1001
	id=1003, uuid=U30, time=0, val=Nov�Ves, pid=1002
	
Simulace)
mem: 
	id=0, dbId = 1007, uuid=U00, time=0, val=Zem
	id=1, dbId = 1004, uuid=U10, time=0, val=�Rx
	id=2, dbId = 1005, uuid=U20, time=1, val=Teplice, pid=1
	id=3, dbId = 1006, uuid=U30, time=1, val=Nov� Ves, pid=2
db:
	id=1000, uuid=U00, time=1, val=Zem�
	id=1001, uuid=U10, time=1, val=�R, pid=1000
	id=1002, uuid=U20, time=0, val=�st�, pid=1001
	id=1003, uuid=U30, time=0, val=Nov�Ves, pid=1002
	id=1004, uuid=U10, time=0, val=�Rx, pid=1007
	id=1005, uuid=U20, time=1, val=Teplice, pid=1004
	id=1006, uuid=U30, time=1, val=Nov� Ves, pid=1005
	id=1007, uuid=U00, time=0, val=Zem
3 = vrac� - je v mem)
	id=1000, uuid=U00, time=1, val=Zem�
	id=1001, uuid=U10, time=1, val=�R
      	id=1002, uuid=U20, time=0, val=�st�, pid=1001
	id=1003, uuid=U30, time=0, val=Nov�Ves, pid=1002
 
Ov��en� bod 5)
mem: 
	id=0, dbId = 1007 -> 1000, uuid=U00, time=0, val=Zem
	id=1, dbId = 1004 -> 1001, uuid=U10, time=0, val=�Rx
	id=2, dbId = 1005, uuid=U20, time=1, val=Teplice, pid=1
	id=3, dbId = 1006, uuid=U30, time=1, val=Nov� Ves, pid=2
db:     delete in (1004, 1007)
        update set pid = 1000 where pid=1007
        update set pid = 1001 where pid=1004


Ov��en� bod 6)
db:     delete GPS where id in (1005, 1006)
        delete varNames where id in (1005, 1006)
        update record.... 1002  
        update record.... 1003
        delete where id in (1005, 1006)

Fin�ln� stav db:
	id=1001, uuid=U10, time=1, val=�R
	id=1002, uuid=U20, time=1, val=Teplice, pid=1001
	id=1003, uuid=U30, time=1, val=Nov� Ves, pid=1002
