package cz.tacr.elza.doc;


import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.controller.ArrangementManager;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrDescItemInt;
import cz.tacr.elza.domain.ArrDescItemPartyRef;
import cz.tacr.elza.domain.ArrDescItemRecordRef;
import cz.tacr.elza.domain.ArrDescItemString;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.vo.ArrDescItemSavePack;
import cz.tacr.elza.domain.vo.ArrDescItems;


/**
 * Příklady zápisů a popis operací nad hodnotami atributů (způsoby použití metody saveDescriptionItems() z ArrangementManager).
 *
 * @author Martin Šlapa [martin.slapa@marbes.cz]
 * @since 17.9.2015
 */
public class DescItemOperations {

    @Autowired
    private ArrangementManager arrangementManager;

    /**
     * Příklad pro vytvoření nové hodnoty atributu. Ukazuje postupné naplnění vstupních dat pro správné vytvoření.
     */
    public void createDescItem() {

        // identifikátor verze - je nutné pro určení verze, do které se projeví změna (v našem případě to musí být poslední otevřená verze)
        Integer versionId = 1;

        /* uzel, který identifikuje zanoření (level uzlu) ve stromu - použivá se pro vzájemné vyloučení úpravy (optimistický zámek)
           konkrétní správné získání reference je z objektu level */
        ArrNode node = new ArrNode();

        // určuje, zda-li se má vytvářet nová verze - při vytváření se musí vždy volat s true
        Boolean createNewVersion = true;

        /* určuje typ vytvářeného atributu
           konkrétní správné získání reference je pomocí metody getDescriptionItemTypesForNodeId() z RuleManager */
        RulDescItemType descItemType = new RulDescItemType();

        /* určuje specifikaci vytvářeného atributu
           konkrétní správné získání reference je pomocí metody getDescItemSpecsFortDescItemType() z RuleManager */
        RulDescItemSpec descItemSpec = new RulDescItemSpec();

        List<ArrDescItem> descItems = new ArrayList<>();
        List<ArrDescItem> deleteDescItems = new ArrayList<>();

        // vytvoření nového objektu, který chceme uložit
        ArrDescItem newDescItem = new ArrDescItemInt();
        newDescItem.setPosition(1);                 /* nastavení pozice (není vyžadováno, core doplní správnou; obecně se přidává na konec)
                                                       pokud by atributy již existovaly, provede se posun v jádře a nastaví se požadovaná z UI */
        newDescItem.setDescItemType(descItemType);  // nastavení typu atributu (nesmí být null, musí odpovídat povoleným typům)
        newDescItem.setDescItemSpec(descItemSpec);  // nastavení specifikace atributu (může být null pokud nemá vazbu z DescItemType)
        newDescItem.setNode(node);                  // nastavení uzlu
        ((ArrDescItemInt) newDescItem).setValue(1); // nastavuje konkrétní hodnotu atributu, která se má uložit

        descItems.add(newDescItem);                 // přidání nového objektu do seznamu

        // vytvoření VO pro přenesení vstupních dat
        ArrDescItemSavePack pack = new ArrDescItemSavePack();

        // naplnění VO
        pack.setDescItems(descItems);               // nastavení seznamu položek pro vytvoření/úpravu (v tomto případě jedna položka na vytvoření)
        pack.setDeleteDescItems(deleteDescItems);   // nastavení seznamu položek pro smazání (je prázdný, ale nesmí být null)
        pack.setCreateNewVersion(createNewVersion); // zda-li se má vytvářet nová verze (při vytváření hodnoty atributu musí být true)
        pack.setFaVersionId(versionId);             // nastavení identifikátoru verze
        pack.setNode(node);                         // nastavení uzlu

        /* provedení operace v jádře
           v result se vrátí VO se seznamem všech finálních objektů - v našem případě jeden nově vytvořený */
        ArrDescItems result = arrangementManager.saveDescriptionItems(pack);

    }

    /**
     * Příklad pro vytvoření nové hodnoty atributu - Odkaz na původce. Ukazuje postupné naplnění vstupních dat pro správné vytvoření.
     */
    public void createDescItemParty() {

        // identifikátor verze - je nutné pro určení verze, do které se projeví změna (v našem případě to musí být poslední otevřená verze)
        Integer versionId = 1;

        /* uzel, který identifikuje zanoření (level uzlu) ve stromu - použivá se pro vzájemné vyloučení úpravy (optimistický zámek)
           konkrétní správné získání reference je z objektu level */
        ArrNode node = new ArrNode();

        // určuje, zda-li se má vytvářet nová verze - při vytváření se musí vždy volat s true
        Boolean createNewVersion = true;

        /* určuje typ vytvářeného atributu
           konkrétní správné získání reference je pomocí metody getDescriptionItemTypesForNodeId() z RuleManager */
        RulDescItemType descItemType = new RulDescItemType();

        /* určuje specifikaci vytvářeného atributu
           konkrétní správné získání reference je pomocí metody getDescItemSpecsFortDescItemType() z RuleManager */
        RulDescItemSpec descItemSpec = new RulDescItemSpec();

        /* určuje vybranou hodnotu průvodce
           konkrétní správné získání reference je pomocí metody findParty() z PartyManager */
        ParParty party = new ParParty();

        List<ArrDescItem> descItems = new ArrayList<>();
        List<ArrDescItem> deleteDescItems = new ArrayList<>();

        // vytvoření nového objektu, který chceme uložit
        ArrDescItem newDescItem = new ArrDescItemPartyRef();
        newDescItem.setPosition(1);                             /* nastavení pozice (není vyžadováno, core doplní správnou; obecně se přidává na konec)
                                                                   pokud by atributy již existovaly, provede se posun v jádře a nastaví se požadovaná z UI */
        newDescItem.setDescItemType(descItemType);              // nastavení typu atributu (nesmí být null, musí odpovídat povoleným typům)
        newDescItem.setDescItemSpec(descItemSpec);              // nastavení specifikace atributu (může být null pokud nemá vazbu z DescItemType)
        newDescItem.setNode(node);                              // nastavení uzlu
        ((ArrDescItemPartyRef) newDescItem).setParty(party);    // nastavení reference na původce

        descItems.add(newDescItem);                 // přidání nového objektu do seznamu

        // vytvoření VO pro přenesení vstupních dat
        ArrDescItemSavePack pack = new ArrDescItemSavePack();

        // naplnění VO
        pack.setDescItems(descItems);               // nastavení seznamu položek pro vytvoření/úpravu (v tomto případě jedna položka na vytvoření)
        pack.setDeleteDescItems(deleteDescItems);   // nastavení seznamu položek pro smazání (je prázdný, ale nesmí být null)
        pack.setCreateNewVersion(createNewVersion); // zda-li se má vytvářet nová verze (při vytváření hodnoty atributu musí být true)
        pack.setFaVersionId(versionId);             // nastavení identifikátoru verze
        pack.setNode(node);                         // nastavení uzlu

        /* provedení operace v jádře
           v result se vrátí všechny finální objekty - v našem případě jeden nově vytvořený */
        ArrDescItems result = arrangementManager.saveDescriptionItems(pack);

    }

    /**
     * Příklad pro vytvoření nové hodnoty atributu - Role entit. Ukazuje postupné naplnění vstupních dat pro správné vytvoření.
     */
    public void createDescItemRecord() {

        // identifikátor verze - je nutné pro určení verze, do které se projeví změna (v našem případě to musí být poslední otevřená verze)
        Integer versionId = 1;

        /* uzel, který identifikuje zanoření (level uzlu) ve stromu - použivá se pro vzájemné vyloučení úpravy (optimistický zámek)
           konkrétní správné získání reference je z objektu level */
        ArrNode node = new ArrNode();

        // určuje, zda-li se má vytvářet nová verze - při vytváření se musí vždy volat s true
        Boolean createNewVersion = true;

        /* určuje typ vytvářeného atributu
           konkrétní správné získání reference je pomocí metody getDescriptionItemTypesForNodeId() z RuleManager */
        RulDescItemType descItemType = new RulDescItemType();

        /* určuje specifikaci vytvářeného atributu
           konkrétní správné získání reference je pomocí metody getDescItemSpecsFortDescItemType() z RuleManager */
        RulDescItemSpec descItemSpec = new RulDescItemSpec();

        /* určuje vybranou hodnotu role entit
           konkrétní správné získání reference je pomocí metody findRecord() z RegistryManager */
        RegRecord record = new RegRecord();

        List<ArrDescItem> descItems = new ArrayList<>();
        List<ArrDescItem> deleteDescItems = new ArrayList<>();

        // vytvoření nového objektu, který chceme uložit
        ArrDescItem newDescItem = new ArrDescItemRecordRef();
        newDescItem.setPosition(1);                             /* nastavení pozice (není vyžadováno, core doplní správnou; obecně se přidává na konec)
                                                                   pokud by atributy již existovaly, provede se posun v jádře a nastaví se požadovaná z UI */
        newDescItem.setDescItemType(descItemType);              // nastavení typu atributu (nesmí být null, musí odpovídat povoleným typům)
        newDescItem.setDescItemSpec(descItemSpec);              // nastavení specifikace atributu (nesmí být null, odvíjí se na kombinaci s record)
        newDescItem.setNode(node);                              // nastavení uzlu
        ((ArrDescItemRecordRef) newDescItem).setRecord(record); // nastavení reference na Role entit

        descItems.add(newDescItem);                 // přidání nového objektu do seznamu

        // vytvoření VO pro přenesení vstupních dat
        ArrDescItemSavePack pack = new ArrDescItemSavePack();

        // naplnění VO
        pack.setDescItems(descItems);               // nastavení seznamu položek pro vytvoření/úpravu (v tomto případě jedna položka na vytvoření)
        pack.setDeleteDescItems(deleteDescItems);   // nastavení seznamu položek pro smazání (je prázdný, ale nesmí být null)
        pack.setCreateNewVersion(createNewVersion); // zda-li se má vytvářet nová verze (při vytváření hodnoty atributu musí být true)
        pack.setFaVersionId(versionId);             // nastavení identifikátoru verze
        pack.setNode(node);                         // nastavení uzlu

        /* provedení operace v jádře
           v result se vrátí všechny finální objekty - v našem případě jeden nově vytvořený */
        ArrDescItems result = arrangementManager.saveDescriptionItems(pack);

    }

    /**
     * Příklad pro smazání existující hodnoty atributu. Ukazuje postupné naplnění vstupních dat pro správné smazání.
     */
    public void deleteDescItem() {

        // identifikátor verze - je nutné pro určení verze, do které se projeví změna (v našem případě to musí být poslední otevřená verze)
        Integer versionId = 1;

        /* uzel, který identifikuje zanoření (level uzlu) ve stromu - použivá se pro vzájemné vyloučení úpravy (optimistický zámek)
           konkrétní správné získání reference je z objektu level */
        ArrNode node = new ArrNode();

        // určuje, zda-li se má vytvářet nová verze - při mazání se musí vždy volat s true
        Boolean createNewVersion = true;

        List<ArrDescItem> descItems = new ArrayList<>();
        List<ArrDescItem> deleteDescItems = new ArrayList<>();

        // vytvoření nového objektu, který chceme smazat (obecně se předpokládá načtený objekt)
        ArrDescItem deleteDescItem = new ArrDescItemInt();
        deleteDescItem.setDescItemObjectId(1000);       /* nastavení identifikátoru, podle kterého se bude mazat položka z otevřené verze
                                                           hodnota 1000 je jen příkladná, záleží na konkrétní hodnotě atributu */

        deleteDescItems.add(deleteDescItem);            // přidání nového objektu do seznamu ke smazání

        // vytvoření VO pro přenesení vstupních dat
        ArrDescItemSavePack pack = new ArrDescItemSavePack();

        // naplnění VO
        pack.setDescItems(descItems);               // nastavení seznamu položek pro vytvoření/úpravu (je prázdný, ale nesmí být null)
        pack.setDeleteDescItems(deleteDescItems);   // nastavení seznamu položek pro smazání (v tomto případě jedna položka na smazání)
        pack.setCreateNewVersion(createNewVersion); // zda-li se má vytvářet nová verze (při mazání hodnoty atributu musí být true)
        pack.setFaVersionId(versionId);             // nastavení identifikátoru verze
        pack.setNode(node);                         // nastavení uzlu

        /* provedení operace v jádře
           v result se vrátí všechny finální objekty - v našem případě jeden po smazání */
        ArrDescItems result = arrangementManager.saveDescriptionItems(pack);

    }

    /**
     * První příklad pro úpravu hodnoty atributu. Ukazuje postupné naplnění vstupních dat pro správnou úpravu bez verzování.
     */
    public void updateDescItem1() {
        /* identifikátor verze - je nutné pro určení verze, do které se projeví změna
           (v našem případě libovolná verze, protože při neverzované úpravě se upravuje pouze uložená hodnota)
         */
        Integer versionId = 1;

        /* uzel, který identifikuje zanoření (level uzlu) ve stromu - použivá se pro vzájemné vyloučení úpravy (optimistický zámek)
           konkrétní správné získání reference je z objektu level */
        ArrNode node = new ArrNode();

        // určuje, zda-li se má vytvářet nová verze - při neverzované úpravě se volá s false
        Boolean createNewVersion = false;

        /* určuje typ upravovaného atributu
           konkrétní správné získání reference je pomocí metody getDescriptionItemTypesForNodeId() z RuleManager */
        RulDescItemType descItemType = new RulDescItemType();

        /* určuje specifikaci vytvářeného atributu
           konkrétní správné získání reference je pomocí metody getDescItemSpecsFortDescItemType() z RuleManager */
        RulDescItemSpec descItemSpec = new RulDescItemSpec();

        List<ArrDescItem> descItems = new ArrayList<>();
        List<ArrDescItem> deleteDescItems = new ArrayList<>();

        // vytvoření nového objektu, který chceme upravit (obecně se předpokládá načtený objekt s upravenými hodnotami v UI)
        ArrDescItem updateDescItem = new ArrDescItemString();
        updateDescItem.setPosition(1);                                      // pozice musí odpovídat hodnotě v databázi, jinak bude vyhozena výjimka kvůli změně pozice
        updateDescItem.setDescItemObjectId(1000);                           /* nastavení identifikátoru, podle kterého se bude upravovat položka
                                                                               hodnota 1000 je jen příkladná, záleží na konkrétní hodnotě atributu */
        updateDescItem.setDescItemType(descItemType);                       // nastavení typu atributu (nesmí být null, musí odpovídat povoleným typům)
        updateDescItem.setDescItemSpec(descItemSpec);                       // nastavení specifikace atributu (může být null pokud nemá vazbu z DescItemType)
        ((ArrDescItemString) updateDescItem).setValue("Nová hodnota atr");  // nastavuje novou hodnotu atributu, která se má uložit

        descItems.add(updateDescItem);                 // přidání nového objektu do seznamu

        // vytvoření VO pro přenesení vstupních dat
        ArrDescItemSavePack pack = new ArrDescItemSavePack();

        // naplnění VO
        pack.setDescItems(descItems);               // nastavení seznamu položek pro vytvoření/úpravu (v tomto případě jedna položka na úpravu)
        pack.setDeleteDescItems(deleteDescItems);   // nastavení seznamu položek pro smazání (je prázdný, ale nesmí být null)
        pack.setCreateNewVersion(createNewVersion); // zda-li se má vytvářet nová verze (v tomto případě false, protože se jedná o neverzovanou úpravu)
        pack.setFaVersionId(versionId);             // nastavení identifikátoru verze
        pack.setNode(node);                         // nastavení uzlu

        /* provedení operace v jádře
           v result se vrátí všechny finální objekty - v našem případě jeden nově upravený */
        ArrDescItems result = arrangementManager.saveDescriptionItems(pack);
    }

    /**
     * Druhý příklad pro úpravu hodnoty atributu. Ukazuje postupné naplnění vstupních dat pro správnou úpravu s verzováním.
     */
    public void updateDescItem2() {
        /* identifikátor verze - je nutné pro určení verze, do které se projeví změna
           (v našem případě musí být poslední-otevřená verze)
         */
        Integer versionId = 1;

        /* uzel, který identifikuje zanoření (level uzlu) ve stromu - použivá se pro vzájemné vyloučení úpravy (optimistický zámek)
           konkrétní správné získání reference je z objektu level */
        ArrNode node = new ArrNode();

        // určuje, zda-li se má vytvářet nová verze - při verzované úpravě se volá s true
        Boolean createNewVersion = true;

        /* určuje typ upravovaného atributu
           konkrétní správné získání reference je pomocí metody getDescriptionItemTypesForNodeId() z RuleManager */
        RulDescItemType descItemType = new RulDescItemType();

        /* určuje specifikaci vytvářeného atributu
           konkrétní správné získání reference je pomocí metody getDescItemSpecsFortDescItemType() z RuleManager */
        RulDescItemSpec descItemSpec = new RulDescItemSpec();

        List<ArrDescItem> descItems = new ArrayList<>();
        List<ArrDescItem> deleteDescItems = new ArrayList<>();

        // vytvoření nového objektu, který chceme upravit (obecně se předpokládá načtený objekt s upravenými hodnotami v UI)
        ArrDescItem updateDescItem = new ArrDescItemString();
        updateDescItem
                .setPosition(1);                                      // pozice určuje reálnou pozici, na které má položka být (pokud je null, předpokládá se nezměnění pozice)
        updateDescItem.setDescItemObjectId(1000);                           /* nastavení identifikátoru, podle kterého se bude upravovat položka
                                                                               hodnota 1000 je jen příkladná, záleží na konkrétní hodnotě atributu */
        updateDescItem.setDescItemType(descItemType);                       // nastavení typu atributu (nesmí být null, musí odpovídat povoleným typům)
        updateDescItem.setDescItemSpec(descItemSpec);                       // nastavení specifikace atributu (může být null pokud nemá vazbu z DescItemType)
        ((ArrDescItemString) updateDescItem)
                .setValue("Nová hodnota atr");  // nastavuje novou hodnotu atributu, která se má uložit - konkrétní formát řetězce je závislý na DescItemType

        descItems.add(updateDescItem);                 // přidání nového objektu do seznamu

        // vytvoření VO pro přenesení vstupních dat
        ArrDescItemSavePack pack = new ArrDescItemSavePack();

        // naplnění VO
        pack.setDescItems(descItems);               // nastavení seznamu položek pro vytvoření/úpravu (v tomto případě jedna položka na úpravu)
        pack.setDeleteDescItems(deleteDescItems);   // nastavení seznamu položek pro smazání (je prázdný, ale nesmí být null)
        pack.setCreateNewVersion(createNewVersion); // zda-li se má vytvářet nová verze (v tomto případě true, protože se jedná o verzovanou úpravu)
        pack.setFaVersionId(versionId);             // nastavení identifikátoru verze
        pack.setNode(node);                         // nastavení uzlu

        /* provedení operace v jádře
           v result se vrátí všechny finální objekty - v našem případě jeden nově upravený */
        ArrDescItems result = arrangementManager.saveDescriptionItems(pack);
    }

}
