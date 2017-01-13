# Casti

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

Pravidla
--------
1 Package -^- (0..1) AS
1 DAO -^-  (0..1) Jednotka Popisu - bud je nebo neni k necemu prilinkovana

Otazky
------
Ma Storage vedet ke kteremu AS patri Package?
