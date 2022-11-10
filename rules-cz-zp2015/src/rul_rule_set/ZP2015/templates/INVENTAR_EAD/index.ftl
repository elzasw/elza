<#ftl output_format="XML" ns_prefixes={"ead":"http://ead3.archivists.org/schema/", "cam":"http://cam.tacr.cz/2019"}><#noautoesc><?xml version="1.0" encoding="UTF-8"?></#noautoesc>
<ead:ead xmlns:ead="http://ead3.archivists.org/schema/" xmlns:cam="http://cam.tacr.cz/2019">

<#-- Seznam mapování typů dle 5.9 -->
<#assign unitTypeMapping = { 
        "ZP2015_UNIT_TYPE_LIO": "lio",
        "ZP2015_UNIT_TYPE_LIP": "lip",
        "ZP2015_UNIT_TYPE_UKN": "ukn",
        "ZP2015_UNIT_TYPE_RKP": "rkp",
        "ZP2015_UNIT_TYPE_HDB": "rkp",
        "ZP2015_UNIT_TYPE_PPR": "ppr",
        "ZP2015_UNIT_TYPE_IND": "ind",
        "ZP2015_UNIT_TYPE_REP": "rep",
        "ZP2015_UNIT_TYPE_KTT": "ktt",
        "ZP2015_UNIT_TYPE_PEC": "pec",
        "ZP2015_UNIT_TYPE_RAZ": "raz",
        "ZP2015_UNIT_TYPE_OTD": "otd",
        "ZP2015_UNIT_TYPE_OTC": "otd",
        "ZP2015_UNIT_TYPE_MAP": "map",
        "ZP2015_UNIT_TYPE_ATL": "atl",
        "ZP2015_UNIT_TYPE_TVY": "tvy",
        "ZP2015_UNIT_TYPE_GLI": "gli",
        "ZP2015_UNIT_TYPE_KRE": "kre",
        "ZP2015_UNIT_TYPE_FSN": "fsn",
        "ZP2015_UNIT_TYPE_FSD": "fsd",
        "ZP2015_UNIT_TYPE_LFI": "lfi",
        "ZP2015_UNIT_TYPE_SFI": "sfi",
        "ZP2015_UNIT_TYPE_KIN": "kin",
        "ZP2015_UNIT_TYPE_MF": "mf",
        "ZP2015_UNIT_TYPE_MFS": "mfis",
        "ZP2015_UNIT_TYPE_FAL": "fal",
        "ZP2015_UNIT_TYPE_DFO": "dfo",
        "ZP2015_UNIT_TYPE_KZA": "kza",
        "ZP2015_UNIT_TYPE_ZZA": "zza",
        "ZP2015_UNIT_TYPE_TIO": "tio",
        "ZP2015_UNIT_TYPE_TIP": "tip",
        "ZP2015_UNIT_TYPE_POH": "poh",
        "ZP2015_UNIT_TYPE_PKT": "pkt",
        "ZP2015_UNIT_TYPE_CPA": "cpa",
        "ZP2015_UNIT_TYPE_STO": "sto",
        "ZP2015_UNIT_TYPE_PNP": "pnp",
        "ZP2015_UNIT_TYPE_PFP": "pfp",
        "ZP2015_UNIT_TYPE_JIN": "jin"
  }
>

<#-- Seznam mapování typů dle 5.4 -->
<#assign otherIdTypeMapping = { 
  "ZP2015_OTHERID_SIG_ORIG": "SIGNATURA_PUVODNI",
  "ZP2015_OTHERID_SIG": "SIGNATURA_ZPRACOVANI",
  "ZP2015_OTHERID_STORAGE_ID": "UKLADACI_ZNAK",
  "ZP2015_OTHERID_CJ": "CISLO_JEDNACI",
  "ZP2015_OTHERID_DOCID": "SPISOVA_ZNACKA",
  "ZP2015_OTHERID_FORMAL_DOCID": "CISLO_VLOZKY",
  "ZP2015_OTHERID_ADDID": "CISLO_PRIRUSTKOVE",
  "ZP2015_OTHERID_OLDSIG": "NEPL_SIGNATURA_ZPRACOVANI",
  "ZP2015_OTHERID_OLDSIG2": "UKLADACI_ZNAK",
  "ZP2015_OTHERID_OLDID": "NEPL_INV_CISLO",
  <#-- Chybí mapování "ZP2015_OTHERID_INVALID_UNITID": "neplatné ukládací číslo", -->
  "ZP2015_OTHERID_INVALID_REFNO": "NEPL_REFERENCNI_OZNACENI",
  "ZP2015_OTHERID_PRINTID": "NEPL_PORADOVE_CISLO",
  "ZP2015_OTHERID_PICID": "NAKL_CISLO",
  "ZP2015_OTHERID_NEGID": "CISLO_NEGATIVU",
  "ZP2015_OTHERID_CDID": "CISLO_PRODUKCE",
  "ZP2015_OTHERID_ISBN": "KOD_ISBN",
  "ZP2015_OTHERID_ISSN": "KOD_ISSN",
  "ZP2015_OTHERID_ISMN": "KOD_ISMN",
  "ZP2015_OTHERID_MATRIXID": "MATRICNI_CISLO"
  }
>
<#-- Mapování localType na label -->
<#assign otherIdLabelMapping = { 
  "SIGNATURA_PUVODNI": "signatura přidělená původcem",
  "SIGNATURA_ZPRACOVANI": "signatura přidělená při zpracování archiválie",
  "UKLADACI_ZNAK": "ukládací znak / spisový znak",
  "CISLO_JEDNACI": "číslo jednací",
  "SPISOVA_ZNACKA": "spisová značka",
  "CISLO_VLOZKY": "číslo vložky úřední knihy",
  "CISLO_PRIRUSTKOVE": "přírůstkové číslo",
  "NEPL_PORADOVE_CISLO": "neplatné pořadové číslo manipulačního seznamu",
  "NEPL_INV_CISLO": "neplatné inventární číslo",
  "NEPL_SIGNATURA_ZPRACOVANI": "signatura přidělená při předchozím zpracování",
  "NEPL_REFERENCNI_OZNACENI": "neplatné referenční označení",
  "NAKL_CISLO": "číslo pohlednice nakladatelství Orbis",
  "CISLO_NEGATIVU": "číslo negativu",
  "CISLO_PRODUKCE": "číslo produkce CD",
  "KOD_ISBN": "kód ISBN",
  "KOD_ISSN": "kód ISSN",
  "KOD_ISMN": "kód ISMN",
  "MATRICNI_CISLO": "matriční číslo (propůjčeného vyznamenání)",
  "JINE": "jiné"
  }
>

<#-- Mapování datace-->
<#assign dateOtherMapping = {
  "ZP2015_DATE_OF_CONTENT": "CONTENT",
  "ZP2015_DATE_DECLARED": "DECLARED",
  "ZP2015_DATE_ORIG": "ORIGIN",
  "ZP2015_DATE_OF_COPY": "COPY",
  "ZP2015_DATE_SEALING": "SEALING",
  "ZP2015_DATE_ACT_PUBLISHING": "ACT_PUBLISHING",
  "ZP2015_DATE_INSERT": "INSERT",
  "ZP2015_DATE_MOLD_CREATION": "MOLD_CREATION",
  "ZP2015_DATE_USAGE": "USAGE",
  "ZP2015_DATE_PUBLISHING": "PUBLISHING",
  "ZP2015_DATE_MAP_UPDATE": "MAP_UPDATE",
  "ZP2015_DATE_CAPTURING": "CAPTURING",
  "ZP2015_DATE_RECORDING": "RECORDING",
  "ZP2015_DATE_AWARDING": "AWARDING",
  "ZP2015_DATE_AWARD_CER": "AWARD_CER",
  "ZP2015_DATE_WITHDRAWAL": "WITHDRAWAL",
  "ZP2015_DATE_LEGALLY_EFFECTIVE_FROM": "LEGALLY_EFFECTIVE_FROM",
  "ZP2015_DATE_VALID_FROM": "VALID_FROM",
  "ZP2015_DATE_LEGALLY_EFFECTIVE_TO": "LEGALLY_EFFECTIVE_TO",
  "ZP2015_DATE_VALID_TO": "VALID_TO"  
  }
>

<#assign linkroleMapping = {
  "ZP2015_ENTITY_ROLE_1": "AUTHOR",
  "ZP2015_ENTITY_ROLE_2": "AUTHOR_DIALOGS",
  "ZP2015_ENTITY_ROLE_3": "AUTHOR_ACCOMP_TEXT",
  "ZP2015_ENTITY_ROLE_5": "COMPOSER",
  "ZP2015_ENTITY_ROLE_6": "COMPOSER",
  "ZP2015_ENTITY_ROLE_7": "CHOREOGRAPHER",
  "ZP2015_ENTITY_ROLE_8": "AUTHOR_COMMENT",
  "ZP2015_ENTITY_ROLE_9": "AUTHOR_TOPIC",
  "ZP2015_ENTITY_ROLE_10": "LYRICIST",
  "ZP2015_ENTITY_ROLE_11": "AUTHOR_TEXT",
  "ZP2015_ENTITY_ROLE_12": "TRICKS_EFFECTS",
  "ZP2015_ENTITY_ROLE_15": "PUBLISHER_OWNER",
  "ZP2015_ENTITY_ROLE_16": "PUBLISHER",
  "ZP2015_ENTITY_ROLE_17": "SEALER",
  "ZP2015_ENTITY_ROLE_18": "PRODUCER",
  "ZP2015_ENTITY_ROLE_19": "CLIENT",
  "ZP2015_ENTITY_ROLE_20": "DISTRIBUTOR",
  "ZP2015_ENTITY_ROLE_21": "RECIPIENT",
  "ZP2015_ENTITY_ROLE_22": "APPLICANT",
  "ZP2015_ENTITY_ROLE_23": "HOLDER_SECURITY",
  "ZP2015_ENTITY_ROLE_24": "SENDER",
  "ZP2015_ENTITY_ROLE_25": "APPROVER",
  "ZP2015_ENTITY_ROLE_26": "BUILDER",  
  "ZP2015_ENTITY_ROLE_27": "DIRECTOR",
  "ZP2015_ENTITY_ROLE_28": "DIRECTOR",
  "ZP2015_ENTITY_ROLE_29": "CAMERAMAN",
  "ZP2015_ENTITY_ROLE_30": "MUSIC_INTERPRETER",
  "ZP2015_ENTITY_ROLE_31": "PHOTOGRAPHER",
  "ZP2015_ENTITY_ROLE_32": "REDACTOR",
  "ZP2015_ENTITY_ROLE_33": "CARTOGRAPHER",
  "ZP2015_ENTITY_ROLE_34": "EDITOR",
  "ZP2015_ENTITY_ROLE_36": "DRAFTSMAN",
  "ZP2015_ENTITY_ROLE_37": "OWNER_AUTHORIZED",
  "ZP2015_ENTITY_ROLE_38": "CREATOR_TECHNICAL",
  "ZP2015_ENTITY_ROLE_39": "CREATOR_ARTWORK",
  "ZP2015_ENTITY_ROLE_40": "DRAMATURG",
  "ZP2015_ENTITY_ROLE_41": "CUTTER",
  "ZP2015_ENTITY_ROLE_42": "SOUND",
  "ZP2015_ENTITY_ROLE_43": "PERFORMER",
  "ZP2015_ENTITY_ROLE_44": "TRANSLATOR",
  "ZP2015_ENTITY_ROLE_45": "LECTOR",
  "ZP2015_ENTITY_ROLE_46": "WITNESS",
  "ZP2015_ENTITY_ROLE_47": "GUARANTOR",
  "ZP2015_ENTITY_ROLE_48": "SCRIBE",
  "ZP2015_ENTITY_ROLE_49": "PROCESSOR_CARRIER",
  "ZP2015_ENTITY_ROLE_50": "MANUFACTURER_CARRIER",
  "ZP2015_ENTITY_ROLE_52": "PRINTER",
  "ZP2015_ENTITY_ROLE_53": "MANUFACTURER",
  "ZP2015_ENTITY_ROLE_56": "LOCATION_SHOOTING",
  "ZP2015_ENTITY_ROLE_57": "LOCATION_PUBLISHER",
  "ZP2015_ENTITY_ROLE_58": "LOCATION_PUBLISHING",
  "ZP2015_ENTITY_ROLE_60": "PLACE_MANUFACTURE",
  "ZP2015_ENTITY_ROLE_61": "PLACE_ORIGIN",
  "ZP2015_ENTITY_ROLE_62": "PLACE_COPY_CREATION",
  "ZP2015_ENTITY_ROLE_63": "TYPE",
  "ZP2015_ENTITY_ROLE_64": "ENTITY",
  "ZP2015_ENTITY_ROLE_65": "ENTITY",
  "ZP2015_ENTITY_ROLE_66": "ENTITY",
  "ZP2015_ENTITY_ROLE_67": "ENTITY",
  "ZP2015_ENTITY_ROLE_68": "ENTITY",
  "ZP2015_ENTITY_ROLE_69": "ENTITY",
  "ZP2015_ENTITY_ROLE_70": "ENTITY",
  "ZP2015_ENTITY_ROLE_71": "ENTITY",
  "ZP2015_ENTITY_ROLE_72": "ENTITY",
  "ZP2015_ENTITY_ROLE_73": "AWARD",
  "ZP2015_ENTITY_ROLE_74": "PERSON_AWARDED",
  "ZP2015_ENTITY_ROLE_75": "PROPONENT",
  "ZP2015_ENTITY_ROLE_76": "PERSON_HANDING",
  "ZP2015_ENTITY_ROLE_77": "PERSON_APPOINTED",
  "ZP2015_ENTITY_ROLE_78": "POSITION",
  "ZP2015_ENTITY_ROLE_79": "CORPORATION_ASSIGNED",
  "ZP2015_ENTITY_ROLE_80": "LOCATION_ASSIGNED", 
  <#-- "ZP2015_ENTITY_ROLE_81": "katalogizační záznam", -->
  "ZP2015_ENTITY_ROLE_82": "PLACE_REGISTER",
  "ZP2015_ENTITY_ROLE_83": "CLASSIFICATION",
  "ZP2015_ENTITY_ROLE_84": "COPYIST",
  "ZP2015_ENTITY_ROLE_85": "OWNER",
  "ZP2015_ENTITY_ROLE_86": "LOCATION_PHOTOGRAPHING",
  "ZP2015_ENTITY_ROLE_87": "COOPERATION",
  "ZP2015_ENTITY_ROLE_88": "PLACE_HANDING"
}
>

<#assign linktitleMapping = {
	"AUTHOR": "autor",
	"AUTHOR_DIALOGS": "autor dialogu",
	"AUTHOR_ACCOMP_TEXT": "autor doprovodného textu",
	"COMPOSER": "autor hudby/skladatel",
	"CHOREOGRAPHER": "autor choreografie/choreograf",
	"AUTHOR_COMMENT": "autor komentáře",
	"AUTHOR_TOPIC": "autor námětu",
	"LYRICIST": "autor textové složky/textař",
	"AUTHOR_TEXT": "autor textu",
	"TRICKS_EFFECTS": "autor triků a speciálních efektů",
	"ARTWORK": "autorské dílo",
	"PUBLISHER_OWNER": "vydavatel",
	"PUBLISHER": "vydavatel/nakladatel",
	"SEALER": "pečetitel",
	"PRODUCER": "produkční společnost/producent",
	"CLIENT": "objednavatel/příjemce",
	"DISTRIBUTOR": "distributor",
	"RECIPIENT": "příjemce",
	"APPLICANT": "žadatel",
	"HOLDER_SECURITY": "držitel cenného papíru",
	"SENDER": "odesílatel",
	"APPROVER": "schvalovatel technického výkresu",
	"BUILDER": "stavitel",
	"DIRECTOR": "režisér",
	"SCRIPTWRITER": "scénárista",
	"CAMERAMAN": "kameraman",
	"MUSIC_INTERPRETER": "interpret hudby",
	"PHOTOGRAPHER": "fotograf",
	"REDACTOR": "redaktor",
	"CARTOGRAPHER": "kartograf",
	"EDITOR": "editor",
	"DRAFTSMAN": "kreslič",
	"OWNER_AUTHORIZED": "majitel typáře",
	"CREATOR_TECHNICAL": "tvůrce technického zpracování",
	"CREATOR_ARTWORK": "tvůrce výtvarné stránky",
	"DRAMATURG": "dramaturg",
	"CUTTER": "střih/střihač",
	"SOUND": "zvuk/zvukař",
	"PERFORMER": "účinkující",
	"TRANSLATOR": "překladatel",
	"LECTOR": "lektor",
	"WITNESS": "svědek",
	"GUARANTOR": "ručitel (rukojmě)", 
	"SCRIBE": "písař",
	"PROCESSOR_CARRIER": "zpracovatel nosiče záznamu",
	"MANUFACTURER_CARRIER": "výrobce nosiče záznamu",
	"PRINTER": "tiskárna/tiskař",
	"MANUFACTURER": "výrobce",
	"LOCATION_SHOOTING": "místo natáčení",
	"LOCATION_PUBLISHER": "místo vydavatele",
	"LOCATION_PUBLISHING": "místo vydání",
	"PLACE_MANUFACTURE": "místo výroby jednotky popisu",
	"PLACE_ORIGIN": "místo vzniku jednotky popisu",
	"PLACE_COPY_CREATION": "místo vzniku předlohy popisované kopie",
	"TYPE": "typové označení a název výrobku a typové stavby",
	"ENTITY": "související entita",
	"AWARD": "vyznamenání/cena",
	"PERSON_AWARDED": "nositel vyznamenání/ceny",
	"PROPONENT": "navrhovatel",
	"PERSON_HANDING": "předávající",
	"PERSON_APPOINTED": "osoba jmenovaná / ustanovená do funkce",
	"POSITION": "funkce",
	"CORPORATION_ASSIGNED": "korporace výkonu funkce",
	"LOCATION_ASSIGNED": "místo výkonu funkce",
	"PLACE_REGISTER": "matriční místo",
	"CLASSIFICATION": "sekundární klasifikace",
	"COPYIST": "opisovač",
	"OWNER": "vlastník",
	"LOCATION_PHOTOGRAPHING": "místo fotografování",
	"COOPERATION": "odborná spolupráce",
	"PLACE_HANDING": "místo předání",
	"CAPTURED_ENTITY": "obrazově a/nebo zvukově zachycená entita"
}
>

<ead:control>
  <#-- 2.1. recordid: uuid -->
  <ead:recordid>${output.uuid}</ead:recordid>

  <#-- 2.2. otherrecordid: finding_aid_id & internal_rev_id -->
  <#list output.items?filter(item -> item.type.code=="ZP2015_FINDING_AID_ID") as item>
  <ead:otherrecordid localtype="CZ_MVCR_FINDING_AID_ID">${item.serializedValue}</ead:otherrecordid>
  </#list>
  <#if output.internalCode?has_content>
  <ead:otherrecordid localtype="INTERNAL_REV_ID">${output.internalCode}</ead:otherrecordid>
  </#if>

  <#-- 2.3. filedesc: archivní soubor, encodinganalog obsahuje identifikátor archivního souboru -->
  <ead:filedesc <#if output.fund.fundNumber?has_content>encodinganalog="${output.fund.fundNumber?c}"</#if>>
    <ead:titlestmt>
      <!-- Povinný název archivního souboru -->
      <ead:titleproper>${output.fund.name}</ead:titleproper>
      <!-- Název archivní pomůcky -->
      <ead:subtitle>${output.name}</ead:subtitle>
    </ead:titlestmt>
  <@writePublStmt output.items />
  </ead:filedesc>

  <#-- 2.4. control/maintenancestatus -->
  <ead:maintenancestatus value="derived" />

  <#-- 2.5. control/maintenanceagency -->
  <ead:maintenanceagency countrycode="CZ">
    <!-- Identifikátor z číselníku archivů -->
    <ead:agencycode localtype="CZ_MVCR_INSTITUTION_ID">${output.fund.institution.code}</ead:agencycode>
    <!-- Jméno archivu -->
    <ead:agencyname>${output.fund.institution.record.preferredPart.value}</ead:agencyname>
  </ead:maintenanceagency>

  <#-- 2.6. Druh archivní pomůcky, control/localcontrol -->
  <#list output.items?filter(item -> item.type.code=="ZP2015_OUTPUT_TYPE") as item>
    <#switch item.specification.name?lower_case>
      <#case "prozatimní inventární seznam">
        <#assign identifier="PROZ_INV_SEZNAM">
        <#break>
      <#case "manipulační seznam">
        <#assign identifier="MANIP_SEZNAM">
        <#break>
      <#case "inventář">
        <#assign identifier="INVENTAR">
        <#break>
      <#case "katalog">
        <#assign identifier="KATALOG">
        <#break>
      <#default>
        <#assign identifier="Undefined">
        <#break>
    </#switch>
  <!-- Druh archivní pomůcky -->
  <ead:localcontrol localtype="FINDING_AID_TYPE">
    <ead:term identifier="${identifier}">${item.specification.name}</ead:term>
  </ead:localcontrol>
  </#list>

  <#-- 2.6.1. Pravidla tvorby archivního popisu -->
  <#list output.createFlatNodeIterator()?filter(node -> node.depth==1) as node>
    <#switch node.getSingleItem("ZP2015_ARRANGEMENT_TYPE").specification.code>
    <#case "ZP2015_ARRANGEMENT_OTHER">
      <#assign rules="CZ_ZP1958">
      <#assign description="základní pravidla z roku 1958">
      <#break>
    <#default>
      <#assign rules="CZ_ZP2013">
      <#assign description="základní pravidla od roku 2013">
      <#break>
    </#switch>  
  </#list>
  <!-- Pravidla tvorby archivního popisu -->
  <ead:localcontrol localtype="RULES">
    <ead:term identifier="${rules}">${description}</ead:term>
  </ead:localcontrol>

  <#-- TODO: odstranit nebo přesunout -->
  <#-- Pocet JP -->
  <#--
  <#list output.items?filter(item -> item.type.code=="ZP2015_UNIT_COUNT_SUM") as item>
  <!-- Součet JP 
  <ead:localcontrol localtype="TOTAL_UNIT_COUNT">
    <ead:term>${item.serializedValue}</ead:term>
  </ead:localcontrol>
  </#list>

  <#-- TODO: odstranit nebo přesunout -->
  <#-- Rozsah archiválií - ZP2015_UNITS_AMOUNT -->
  <#--
  <#list output.items?filter(item -> item.type.code=="ZP2015_UNITS_AMOUNT") as item>
  <!-- Rozsah zpristupnenych archivalii 
  <ead:localcontrol localtype="UNITS_AMOUNT">
    <ead:term>${item.serializedValue}</ead:term>
  </ead:localcontrol>
  </#list> -->  

  <#-- TODO: odstranit nebo přesunout -->
  <#-- Časové rozmezí v tiráži -->
  <#--
  <#list output.items?filter(item -> item.type.code=="ZP2015_DATE_RANGE") as item>
  <!-- Časové rozmezí v tiráži 
  <ead:localcontrol localtype="DATE_RANGE">
    <ead:term>${item.serializedValue}</ead:term>
  </ead:localcontrol>
  </#list> -->
  
  <#-- TODO: odstranit nebo přesunout -->
  <#--  Deklarovane jazyky -->
  <#-- 
  <#list output.items?filter(item -> item.type.code=="ZP2015_LANGUAGE") as item>
  <ead:localcontrol localtype="LANGUAGE">
    <ead:term>${item.specification.code}</ead:term>
  </ead:localcontrol>
  </#list> -->

  <#-- TODO: odstranit nebo přesunout -->
  <#-- <!-- Počet EJ
  <@writePocetEj output.items /> -->

  <#-- 2.7. control/maintenancehistory -->
  <ead:maintenancehistory>
    <ead:maintenanceevent>
      <ead:eventtype value="created"></ead:eventtype>
      <ead:eventdatetime standarddatetime="${output.changeDateTime}">${output.changeDateTime}</ead:eventdatetime>
      <!-- Typ vytvoření popisu machine|human -->
      <ead:agenttype value="machine"></ead:agenttype>
      <!-- Jméno agenta -->
      <ead:agent>ELZA ${output.appVersion}</ead:agent>
    </ead:maintenanceevent>
  </ead:maintenancehistory>
  
  <#-- Zápis entit -->
  <#assign sourcesElem=0>
  <#list output.records as ap >
    <#if (sourcesElem==0)>
      <#lt>  <ead:sources>
      <#assign sourcesElem=1>
    </#if>
    <#lt>    <ead:source id="ap${ap.id?c}"><ead:objectxmlwrap>
<#noautoesc>${ap.exportData("http://cam.tacr.cz/2019")?replace(' xmlns:cam="http://cam.tacr.cz/2019"','','f')}</#noautoesc>
<#--     <#lt>    ${ap.exportXmlData("http://cam.tacr.cz/2019")}-->
<#--     <#lt>    <#noautoesc>${ap.exportData("http://cam.tacr.cz/2019")}</#noautoesc>-->
<#--     <#lt>    <cam:ent><!-- Record id: ${ap.id?c} -->
<#--    <#lt>    </cam:ent>-->
    <#lt>    </ead:objectxmlwrap></ead:source>
  </#list>
  <#if (sourcesElem==1)>
    <#lt>  </ead:sources>
  </#if>
</ead:control>

<#-- TODO: nahradit funkčním kódem -->
<#-- <ead:archdesc level="fonds">
  <ead:did>
    <ead:didnote></ead:didnote>
  </ead:did>
</ead:archdesc>-->

<#-- 3.1. Hierarchie jednotek popisu -->
<#assign endtags=[]>
<@writeNodes output.createFlatNodeIterator() />
</ead:ead>

<#-- 3.1. Hierarchie jednotek popisu -->
<#macro writeNodes nodes>
  <#list nodes as node>
    <@writeTags node.depth />
    <#local tagname="ead:c">
    <#if node.depth==1>
      <#local tagname="ead:archdesc">    
    </#if>
    <#-- define level type -->
    <#switch node.getSingleItem("ZP2015_LEVEL_TYPE").specification.code>
    <#case "ZP2015_LEVEL_ROOT">
      <#local level="fonds">
      <#break>
    <#case "ZP2015_LEVEL_SECTION">
      <#local level="subfonds">
      <#break>
    <#case "ZP2015_LEVEL_SERIES">
      <#local level="series">
      <#break>
    <#case "ZP2015_LEVEL_FOLDER">
      <#local level="file">
      <#break>
    <#case "ZP2015_LEVEL_ITEM">
      <#local level="item">
      <#break>
    <#case "ZP2015_LEVEL_PART">
      <#local level="otherlevel">
      <#local otherlevel="itempart">
      <#break>
    <#default>
      <#local level="otherlevel">
      <#break>
    </#switch>
<${tagname} level="${level}"<#if level=="otherlevel"> otherlevel="${otherlevel}"</#if> base="https://archdesc.nacr.cz/dids/${node.uuid}">
    <#-- ${node.depth} -->
    <@writeNode node />
    <#if node.depth==1 && nodes?api.hasNext()>
      <#lt><ead:dsc>
      <#assign endtags=endtags+["</ead:dsc></"+tagname+">"]>
    <#else>
      <#assign endtags=endtags+["</"+tagname+">"]>
    </#if>
  </#list>
  <@writeTags 1 />
</#macro>

<#-- Zápis uzavíracího tagu nebo tagů -->
<#macro writeTags levelindex>
  <#-- check to close previouse tag -->
  <#if (levelindex<=endtags?size)>
    <#list endtags[(endtags?size)-1..levelindex-1] as tag><#lt>${tag?no_esc}</#list>  
    <#assign endtags = endtags[0..<(levelindex-1)]>
  </#if>
</#macro>

<#-- Zápis jednoho uzlu -->
<#macro writeNode node>
  <@writeDid node />
  <#if !node.nodeId.published>
  <ead:otherfindaid localtype="MightExist"><ead:p>Pro úroveň popisu existují nebo vzniknou další archivní pomůcky.</ead:p></ead:otherfindaid>
  </#if>
  <#-- Elements outside did -->
  <#local relations=[]>
  <#list node.items as item>
    <#switch item.type.code>
    <#case "ZP2015_POSITION">
      <#local relations = relations + [item]>
      <#break>
    <#case "ZP2015_ITEM_TITLE_REF">
      <#local relations = relations + [item]>
      <#break>
    <#case "ZP2015_ENTITY_ROLE">
      <#local relations = relations + [item]>
      <#break>
    <#case "ZP2015_UNIT_HIST">
      <#lt>  <ead:custodhist><ead:p>${item.serializedValue}</ead:p></ead:custodhist>
      <#break>
    <#case "ZP2015_UNIT_ARR">
      <#lt>  <ead:arrangement><ead:p>${item.serializedValue}</ead:p></ead:arrangement>
      <#break>
    <#case "ZP2015_UNIT_CONTENT">
      <#lt>  <ead:scopecontent><ead:p>${item.serializedValue}</ead:p></ead:scopecontent>
      <#break>
    <#case "ZP2015_UNIT_SOURCE">
      <#lt>  <ead:acqinfo><ead:p>${item.serializedValue}</ead:p></ead:acqinfo>
      <#break>
    <#case "ZP2015_FUTURE_UNITS">
      <#lt>  <ead:accruals><ead:p>${item.serializedValue}</ead:p></ead:accruals>
      <#break>
    <#case "ZP2015_UNIT_ACCESS">
      <#lt>  <ead:accessrestrict><ead:p>${item.serializedValue}</ead:p></ead:accessrestrict>
      <#break>      
    <#case "ZP2015_UNIT_CURRENT_STATUS">
      <#lt>  <ead:phystech><ead:p>${item.serializedValue}</ead:p></ead:phystech>
      <#break>
    <#case "ZP2015_COPY_SOURCE">
      <#lt>  <ead:originalsloc><ead:p>${item.serializedValue}</ead:p></ead:originalsloc>
      <#break>
    <#case "ZP2015_RELATED_UNITS">
      <#lt>  <ead:relatedmaterial><ead:p>${item.serializedValue}</ead:p></ead:relatedmaterial>
      <#break>
    <#case "ZP2015_EXISTING_COPY">
      <#lt>  <ead:altformavail><ead:p>${item.serializedValue}</ead:p></ead:altformavail>
      <#break>
    <#case "ZP2015_ARRANGEMENT_INFO">
      <#lt>  <ead:processinfo localtype="ARCHIVIST_NOTE"><ead:p>${item.serializedValue}</ead:p></ead:processinfo>
      <#break>
    <#case "ZP2015_ARRANGE_RULES">
      <#lt>  <ead:processinfo localtype="RULES"><ead:p>${item.serializedValue}</ead:p></ead:processinfo>
      <#break>
    <#case "ZP2015_DESCRIPTION_DATE">
      <#lt>  <ead:processinfo localtype="DESCRIPTION_DATE"><ead:p>${item.serializedValue}</ead:p></ead:processinfo>
      <#break>
    <#case "ZP2015_EDITION">
      <#lt>  <ead:bibliography><ead:p>${item.serializedValue}</ead:p></ead:bibliography>
      <#break>
    </#switch>
  </#list>
  <#if (relations?size>0)>
    <@writeRelations relations />
  </#if>
  
</#macro>

<#-- Zápis did -->
<#macro writeDid node>
  <#-- Proměná určující, zda se bude vypisovat charakteristika JP -->
  <#local needsCharakteristikaJP=false>
  <#-- Určení počtu datací -->
  <#local unitDates=[]>
  <#local unitTitles=[]>
  <#local unitPublicTitles=[]>
  <#local languages=[]>
<ead:did>
  <#if node.getSingleItem("ZP2015_LEVEL_TYPE").specification.code=="ZP2015_LEVEL_ROOT">
    <#-- Počet evidenčních jednotek -->
    <@writePocetEj output.items />
  </#if>
  <#list node.items as item>
    <#switch item.type.code>
      <#case "ZP2015_UNIT_ID">
        <#if output.fund.fundNumber?has_content>
        <#lt>  <ead:unitid localtype="REFERENCNI_OZNACENI" label="referenční označení">CZ${output.fund.institution.code}//${output.fund.fundNumber}//${item.serializedValue}</ead:unitid>
        </#if>
        <#break>
      <#case "ZP2015_SERIAL_NUMBER">
        <#lt>  <ead:unitid localtype="PORADOVE_CISLO" label="pořadové číslo">${item.serializedValue}</ead:unitid>
        <#break>
      <#case "ZP2015_INV_CISLO">
        <#lt>  <ead:unitid localtype="INV_CISLO" label="inventární číslo">${item.serializedValue}</ead:unitid>
        <#break>        
      <#case "ZP2015_OTHER_ID">
        <#if (otherIdTypeMapping?keys?seq_contains(item.specification.code)) >
        <#lt>  <ead:unitid localtype="${otherIdTypeMapping[item.specification.code]}" label="${otherIdLabelMapping[otherIdTypeMapping[item.specification.code]]}">${item.serializedValue}</ead:unitid>
        </#if>
        <#break>        
      <#case "ZP2015_TITLE">
        <#local unitTitles=unitTitles+[item]>
        <#break>
      <#case "ZP2015_TITLE_PUBLIC">
        <#local unitPublicTitles=unitPublicTitles+[item]>
        <#break>
      <#case "ZP2015_FORMAL_TITLE">
        <#lt>  <ead:unittitle localtype="FORMAL_TITLE">${item.serializedValue}</ead:unittitle>
        <#break>        
      <#case "ZP2015_UNIT_DATE">
        <#local unitDates=unitDates+[item]>        
        <#break>
      <#case "ZP2015_DATE_OTHER">
        <#local unitDates=unitDates+[item]>
        <#break>
      <#case "ZP2015_UNIT_DATE_TEXT">
        <#lt>  <ead:unitdate>${item.serializedValue}</ead:unitdate>
        <#break>
      <#case "ZP2015_NOTE">
        <#lt>  <ead:didnote localtype="PUBLIC">${item.serializedValue}</ead:didnote>
        <#break>
      <#case "ZP2015_INTERNAL_NOTE">
        <#lt>  <ead:didnote localtype="INTERNAL" audience="internal">${item.serializedValue}</ead:didnote>
        <#break>
      <#case "ZP2015_STORAGE_COND">
        <#lt>  <ead:physdesc>${item.serializedValue}</ead:physdesc>
        <#break>
      <#case "ZP2015_SCALE">
        <#lt>  <ead:materialspec localtype="SCALE">${item.serializedValue}</ead:materialspec>
        <#break>
      <#case "ZP2015_ORIENTATION">
        <#lt>  <ead:materialspec localtype="ORIENTATION">${item.serializedValue}</ead:materialspec>
        <#break>
      <#case "ZP2015_PART">
        <#lt>  <ead:materialspec localtype="VOLUME">${item.serializedValue}</ead:materialspec>
        <#break>
      <#case "ZP2015_ORIGINATOR">
        <#lt>  <ead:origination localtype="ORIGINATOR">
        <@writeAp item.record "ORIGINATOR" />
        <#lt>  </ead:origination>        
        <#break>
      <#case "ZP2015_LANGUAGE">
        <#local languages=languages+[item]>
        <#break>
      <#case "ZP2015_STORAGE_ID">
      <#-- Ukladaci jednotka -->
      <#lt>  <ead:container>${item.serializedValue}</ead:container>
        <#break>
      <#-- Prvky popisu pro charakteristiku JP -->
      <#case "ZP2015_UNIT_TYPE">
        <#if unitTypeMapping?keys?seq_contains(item.specification.code)>
          <#local needsCharakteristikaJP=true>
        </#if>
        <#break>
      <#case "ZP2015_ITEM_MAT">
        <#local needsCharakteristikaJP=true>
        <#break>
      <#case "ZP2015_LEGEND">
        <#local needsCharakteristikaJP=true>
        <#break>
      <#case "ZP2015_PAINTING_CHAR">
        <#local needsCharakteristikaJP=true>
        <#break>
      <#case "ZP2015_CORROBORATION">        
        <#local needsCharakteristikaJP=true>
        <#break>
      <#case "ZP2015_IMPRINT_COUNT">        
        <#local needsCharakteristikaJP=true>
        <#break>
      <#case "ZP2015_IMPRINT_ORDER">        
        <#local needsCharakteristikaJP=true>
        <#break>
      <#case "ZP2015_SIZE">        
        <#local needsCharakteristikaJP=true>
        <#break>
      <#case "ZP2015_SIZE_WIDTH">
        <#local needsCharakteristikaJP=true>
        <#break>
      <#case "ZP2015_SIZE_HEIGHT">
        <#local needsCharakteristikaJP=true>
        <#break>
      <#case "ZP2015_SIZE_DEPTH">
        <#local needsCharakteristikaJP=true>
        <#break>
      <#case "ZP2015_WEIGHT">
        <#lt>  <ead:physdescstructured physdescstructuredtype="spaceoccupied" coverage="whole">
        <#lt>    <ead:quantity>${item.serializedValue}</ead:quantity>
        <#lt>    <ead:unittype>${item.specification.name}</ead:unittype>
        <#lt>  </ead:physdescstructured>
        <#break>
      <#case "ZP2015_AMOUNT">
        <#lt>  <ead:physdescstructured physdescstructuredtype="spaceoccupied" coverage="whole">
        <#lt>    <ead:quantity>${item.serializedValue}</ead:quantity>
        <#switch item.specification.code>
          <#case "ZP2015_AMOUNT_B">
            <#lt>    <ead:unittype>byte</ead:unittype>
            <#break>
          <#case "ZP2015_AMOUNT_PIECES">
            <#lt>    <ead:unittype>pieces</ead:unittype>
            <#break>
          <#case "ZP2015_AMOUNT_SHEETS">
            <#lt>    <ead:unittype>sheets</ead:unittype>
            <#break>
          <#case "ZP2015_AMOUNT_PAGES">
            <#lt>    <ead:unittype>pages</ead:unittype>
            <#break>
        </#switch>
        <#lt>  </ead:physdescstructured>
        <#break>
      <#case "ZP2015_MOVIE_LENGTH">
        <#lt>  <ead:physdescstructured physdescstructuredtype="spaceoccupied" coverage="whole">
        <#lt>    <ead:quantity>${item.serializedValue}</ead:quantity>
        <#lt>    <ead:unittype>s</ead:unittype>
        <#lt>  </ead:physdescstructured>
        <#break>
      <#case "ZP2015_RECORD_LENGTH">
        <#lt>  <ead:physdescstructured physdescstructuredtype="spaceoccupied" coverage="whole">
        <#lt>    <ead:quantity>${item.serializedValue}</ead:quantity>
        <#lt>    <ead:unittype>s</ead:unittype>
        <#lt>  </ead:physdescstructured>
        <#break>
    </#switch>
  </#list>
  <#if (unitPublicTitles?size>0) >
    <#--  Obsah s omezenim pristupnosti -->
    <#list unitPublicTitles as unitPublicTitle>
      <#lt>  <ead:unittitle audience="external">${unitPublicTitle.serializedValue}</ead:unittitle>
    </#list>
    <#if (unitTitles?size>0) >
      <#list unitTitles as unitTitle>
        <#lt>  <ead:unittitle audience="internal">${unitTitle.serializedValue}</ead:unittitle>
      </#list>
    </#if>
  <#elseif (unitTitles?size>0) >
    <#--  Jen samotny obsah -->
    <#list unitTitles as unitTitle>
      <#lt>  <ead:unittitle>${unitTitle.serializedValue}</ead:unittitle>
    </#list>
  </#if>
  <#if (unitDates?size>0) >
    <@writeUnitDates unitDates />
  </#if>
  <#if (needsCharakteristikaJP)>
    <@writeCharakteristika node />
  </#if>
  <#if (languages?size>0) >
    <@writeLangMaterials languages />
  </#if>
  <#-- zápis DAOs -->
  <#if (node.daos?size>0)>
    <@writeDaos node />
  </#if>
</ead:did>
</#macro>

<#-- Zápis single dao object -->
<#macro writeDaos node>
  <#local daotype="derived">  
  <#local hasAipid=false>
  <#local hasStorageid=false>
  
  <#-- Digitální archiválie je pokud nemá storage_id a současně má AIP_ID -->
  <#list node.items as item>
    <#switch item.type.code>
      <#case "ZP2015_AIP_ID">
        <#local hasAipid=true>
        <#break>
      <#case "ZP2015_STORAGE_ID">
        <#local hasStorageid=true>
        <#break>
    </#switch>
  </#list>
  
  <#if (hasAipid && !hasStorageid)>
    <#local daotype="borndigital">
  </#if>

  <#list node.daos as dao>
  <ead:dao daotype="${daotype}" identifier="${dao.code}">
  <#if (dao.label?length>0&&dao.attachment)>
    <#-- Popis jen v případě DAO jako příloha, tj. netvoří přímo jednotku popisu -->
    <ead:descriptivenote><ead:p>${dao.label}</ead:p></ead:descriptivenote>
  </#if>
  </ead:dao>
  </#list>  
</#macro>

<#-- Zápis charakteristiky 3.3 -->
<#macro writeCharakteristika node>
  <#local pocet="">
  <#local druh="">
  <#local dimensions=[]>
  <#local dimensionUnits="mm">  
  <#list node.items as item>
    <#switch item.type.code>
      <#case "ZP2015_UNIT_TYPE">
        <#if unitTypeMapping?keys?seq_contains(item.specification.code)>
          <#local druh=unitTypeMapping[item.specification.code]>
        </#if>
        <#break>
      <#case "ZP2015_UNIT_COUNT">
        <#local pocet=item.serializedValue>
        <#break>
      <#case "ZP2015_LEVEL_TYPE">
        <#switch item.specification.code>
          <#case "ZP2015_LEVEL_FOLDER">
            <#if (druh=="")>
              <#local druh="file">
            </#if>
            <#break>
          <#case "ZP2015_LEVEL_ITEM">
            <#if (druh=="")>
              <#local druh="item">              
            </#if>
            <#local pocet="1">
            <#break>
          <#case "ZP2015_LEVEL_PART">
            <#if (druh=="")>
              <#local druh="itempart">
            </#if>
            <#break>
        </#switch>
        <#break>
      <#case "ZP2015_SIZE_WIDTH">
        <#local dimensions=dimensions+[item]>
        <#break>
      <#case "ZP2015_SIZE_HEIGHT">
        <#local dimensions=dimensions+[item]>
        <#break>
      <#case "ZP2015_SIZE_DEPTH">
        <#local dimensions=dimensions+[item]>
        <#break>
      <#case "ZP2015_SIZE_UNITS">
        <#local dimensionUnits=item.specification.name>
        <#break>
    </#switch>
  </#list>

  <ead:physdescstructured physdescstructuredtype="materialtype" coverage="whole">
    <ead:quantity>${pocet}</ead:quantity>
    <ead:unittype>${druh}</ead:unittype>
  <#list node.items as item>
    <#switch item.type.code>
      <#case "ZP2015_ITEM_MAT">
        <#lt>    <ead:physfacet localtype="TECHNIQUE">${item.serializedValue}</ead:physfacet>
        <#break>
      <#case "ZP2015_LEGEND">
        <#lt>    <ead:physfacet localtype="LEGEND">${item.serializedValue}</ead:physfacet>
        <#break>
      <#case "ZP2015_PAINTING_CHAR">
        <#lt>    <ead:physfacet localtype="IMPRINT_IMAGE">${item.serializedValue}</ead:physfacet>
        <#break>
      <#case "ZP2015_CORROBORATION">        
        <#lt>    <ead:physfacet localtype="CORROBORATIO">${item.serializedValue}</ead:physfacet>  
        <#break>
      <#case "ZP2015_IMPRINT_COUNT">        
        <#lt>    <ead:physfacet localtype="IMPRINT_COUNT">${item.serializedValue}</ead:physfacet>  
        <#break>
      <#case "ZP2015_IMPRINT_ORDER">        
        <#lt>    <ead:physfacet localtype="IMPRINT_ORDER">${item.serializedValue}</ead:physfacet>  
        <#break>
      <#case "ZP2015_SIZE">
        <#lt>    <ead:dimensions>${item.serializedValue}</ead:dimensions>  
        <#break>
    </#switch>
  </#list>
  <#-- Zapis strukturovanych rozmeru -->
  <#if (dimensions?size>0)>
    <#lt>    <ead:dimensions>  
    <#list dimensions as dimension>
      <#switch dimension.type.code>
        <#case "ZP2015_SIZE_WIDTH">
          <#lt>      <ead:dimensions localtype="WIDTH" unit="${dimensionUnits}">${dimension.serializedValue}</ead:dimensions>
          <#break>
        <#case "ZP2015_SIZE_HEIGHT">
          <#lt>      <ead:dimensions localtype="HEIGHT" unit="${dimensionUnits}">${dimension.serializedValue}</ead:dimensions>
          <#break>
        <#case "ZP2015_SIZE_DEPTH">
          <#lt>      <ead:dimensions localtype="DEPTH" unit="${dimensionUnits}">${dimension.serializedValue}</ead:dimensions>
          <#break>
      </#switch>
    </#list>
    <#lt>    </ead:dimensions>
  </#if>
  </ead:physdescstructured>
</#macro>

<#-- Zápis jazyku, vola se jen pokud existuje alespon jeden jazyk -->
<#macro writeLangMaterials items>
  <!-- Jazyky JP -->
  <ead:langmaterial>
  <#list items as langItem>
    <ead:language langcode="${langItem.specification.code[4..]}">${langItem.specification.name}</ead:language>
  </#list>
  </ead:langmaterial>
</#macro>

<#macro writeAp ap localtype>
<#switch ap.type.parentType.code>
  <#case "PERSON">
    <#local tagname="persname">
    <#break>
  <#case "DYNASTY">
    <#local tagname="famname">
    <#break>
  <#case "PARTY_GROUP">
    <#local tagname="corpname">
    <#break>
  <#default>
    <#local tagname="name">
    <#break>
</#switch>
        <ead:${tagname} localtype="${localtype}">          
          <ead:part><ead:ref target="ap${ap.id?c}">${ap.preferredPart.value}</ead:ref></ead:part>
        </ead:${tagname}>
</#macro>

<#-- 3.5. Počet evidenčních jednotek zpřístupněných archivní pomůckou -->
<#macro writePocetEj items>
  <!-- Evidencni jednotky -->
  <#list items?filter(item -> item.type.code=="ZP2015_UNIT_COUNT_TABLE") as item>
    <#list item.table.rows as row>
  <ead:physdescstructured physdescstructuredtype="otherphysdescstructuredtype"
                          otherphysdescstructuredtype="UNIT_TYPE"
                          coverage="part">
    <ead:quantity>${row.values["COUNT"]}</ead:quantity>
    <ead:unittype>${row.values["NAME"]}</ead:unittype>
  </ead:physdescstructured>
    </#list>
  </#list>
</#macro>

<#--#macro writePocetEj items>
<#local ejTables=items?filter(item -> item.type.code=="ZP2015_UNIT_COUNT_TABLE")>
<#list ejTables as ejTable>
  <#list ejTable.table.rows as row>    
  <ead:localcontrol localtype="DECLARED_UNITS">
    <ead:term encodinganalog="${row.values["NAME"]}">${row.values["COUNT"]}</ead:term>    
  </ead:localcontrol>
  <#if row.values["DATE_RANGE"]?? >
  <ead:localcontrol localtype="DECLARED_UNITS_DATES">
    <ead:term encodinganalog="${row.values["NAME"]}">${row.values["DATE_RANGE"]}</ead:term>    
  </ead:localcontrol>
  </#if>
  <#if row.values["LENGTH"]?? >
  <ead:localcontrol localtype="DECLARED_UNITS_LENGTH">
    <ead:term encodinganalog="${row.values["NAME"]}">${row.values["LENGTH"]}</ead:term>    
  </ead:localcontrol>
  </#if>
  </#list>
</#list>    
</#macro>-->

<#macro writePublStmt items>
  <#-- Test if item type exists -->
  <#local processedTypes = ["ZP2015_FINDING_AID_APPROVED_BY", "ZP2015_RELEASE_DATE_PLACE", 
                            "ZP2015_DESCRIPTION_DATE", "ZP2015_FINDING_AID_DATE",
                            "ZP2015_FINDING_AID_EDITOR", "ZP2015_ORIGINATOR_SIMPLE",
                            "ZP2015_ARRANGER", "ZP2015_ARRANGER_TEXT"
                            ] >
  <#local otherItems = items?take_while(item -> !(processedTypes?seq_contains(item.type.code)))  >
  <#if otherItems?size==items?size>
    <#return>
  </#if>
    <!-- Informace o publikaci pomucky -->
    <ead:publicationstmt>
      <#-- 4.1.1. Schvalovatel archivní pomůcky -->
      <#list items?filter(item -> item.type.code=="ZP2015_FINDING_AID_APPROVED_BY") as item>
      <!-- Schvalovatel archivní pomůcky -->
      <ead:p>
        <ead:name localtype="FINDING_AID_APPROVED_BY">
          <ead:part>${item.serializedValue}</ead:part>
        </ead:name>
      </ead:p>
      </#list>
      <#-- 4.1.2. Datum a místo vydání -->
      <#list items?filter(item -> item.type.code=="ZP2015_RELEASE_DATE_PLACE") as item>
      <!-- Datum a misto vydani -->
      <ead:date localtype="RELEASE_DATE_PLACE">${item.serializedValue}</ead:date>
      </#list>
      <#-- 4.1.3. Datum (data) popisu -->
      <#list items?filter(item -> item.type.code=="ZP2015_DESCRIPTION_DATE") as item>
      <!-- Datum popisu -->
      <ead:date localtype="DESCRIPTION_DATE">${item.serializedValue}</ead:date>
      </#list>
      <#-- 4.1.4. Stav archivní pomůckou zpřístupněných archiválií ke dni -->    
      <#list items?filter(item -> item.type.code=="ZP2015_FINDING_AID_DATE") as item>
      <!-- Datum zachyceneho stavu --> 
      <ead:date localtype="FINDING_AID_DATE">${item.serializedValue}</ead:date>
      </#list>
      <#-- 4.1.5. Archivní pomůcku sestavil -->
      <#list items?filter(item -> item.type.code=="ZP2015_FINDING_AID_EDITOR") as item>
      <!-- Sestavil/editor archivni pomucky -->
      <ead:p>
        <ead:name localtype="FINDING_AID_EDITOR">
          <ead:part>${item.serializedValue}</ead:part>
        </ead:name>
      </ead:p>
      </#list>
      <#-- 4.1.6. Původce archiválií -->
      <#list items?filter(item -> item.type.code=="ZP2015_ORIGINATOR_SIMPLE") as structitem>
      <!-- Původce v uvodu archivni pomucky -->
      <#list structitem.value.items?filter(item -> item.type.code=="ZP2015_ORIGINATOR") as item>
      <ead:p>
        <@writeAp item.record "ORIGINATOR" />
      </ead:p>
      </#list>
      </#list>
      <#-- 4.1.7. Zpracovatel archiválií -->
      <#list items?filter(item -> item.type.code=="ZP2015_ARRANGER") as item>
      <!-- Zpracovatel v uvodu archivni pomucky --> 
      <ead:p>
        <@writeAp item.record "ARRANGER" />
      </ead:p>
      </#list>
      <#list items?filter(item -> item.type.code=="ZP2015_ARRANGER_TEXT") as item>
      <!-- Zpracovatel v tirazi archivni pomucky --> 
      <ead:p>
        <ead:name localtype="ARRANGER_BRIEF">
          <ead:part>${item.serializedValue}</ead:part>
        </ead:name>
      </ead:p>
      </#list>
    </ead:publicationstmt>
</#macro>

<#macro writeNoteStmt items>
  <#-- Test if item type exists -->
  <#local processedTypes = ["ZP2015_UNIT_HIST", "ZP2015_UNIT_ARR", 
                            "ZP2015_UNIT_CONTENT", "ZP2015_UNIT_SOURCE",
                            "ZP2015_FUTURE_UNITS",  "ZP2015_UNIT_CURRENT_STATUS",
                            "ZP2015_ARRANGE_RULES"
                            ] >
  <#local otherItems = items?take_while(item -> !(processedTypes?seq_contains(item.type.code)))  >
  <#if otherItems?size==items?size>
    <#return>
  </#if>
    <ead:notestmt>
      <#list items?filter(item -> item.type.code=="ZP2015_UNIT_HIST") as item>
      <!-- Dějiny jednotky popisu --> 
      <ead:controlnote localtype="UNITS_HISTORY"><ead:p>${item.serializedValue}</ead:p></ead:controlnote>
      </#list>      
      <#list items?filter(item -> item.type.code=="ZP2015_UNIT_ARR") as item>
      <!-- Způsob uspořádání jednotky popisu --> 
      <ead:controlnote localtype="UNITS_ARRANGEMENT"><ead:p>${item.serializedValue}</ead:p></ead:controlnote>
      </#list>      
      <#list items?filter(item -> item.type.code=="ZP2015_UNIT_CONTENT") as item>
      <!-- Tematický popis jednotky popisu --> 
      <ead:controlnote localtype="UNITS_CONTENT_SUMMARY"><ead:p>${item.serializedValue}</ead:p></ead:controlnote>
      </#list>      
      <#list items?filter(item -> item.type.code=="ZP2015_UNIT_SOURCE") as item>
      <!-- Přímý zdroj akvizice --> 
      <ead:controlnote localtype="UNITS_SOURCE"><ead:p>${item.serializedValue}</ead:p></ead:controlnote>
      </#list>      
      <#list items?filter(item -> item.type.code=="ZP2015_FUTURE_UNITS") as item>
      <!-- Budoucí přírůstky --> 
      <ead:controlnote localtype="FUTURE_UNITS"><ead:p>${item.serializedValue}</ead:p></ead:controlnote>
      </#list>      
      <#list items?filter(item -> item.type.code=="ZP2015_UNIT_CURRENT_STATUS") as item>
      <!-- Fyz. stav --> 
      <ead:controlnote localtype="CURRENT_UNITS_STATUS"><ead:p>${item.serializedValue}</ead:p></ead:controlnote>
      </#list>      
      <#list items?filter(item -> item.type.code=="ZP2015_ARRANGE_RULES") as item>
      <!-- Pravidla --> 
      <ead:controlnote localtype="ARRANGEMENT_RULES"><ead:p>${item.serializedValue}</ead:p></ead:controlnote>
      </#list>      
    </ead:notestmt>
</#macro>

<#-- <#macro writeOriginator ap>
  <ead:origination localtype="ORIGINATOR">
  <@writeAp ap "ORIGINATOR" />
  </ead:origination>
</#macro>-->

<#-- <#macro writeUklJednotka item>
  <!-- Ukladaci jednotka
  <ead:container>${item.serializedValue}</ead:container>
</#macro>-->

<#-- Zapis dataci -->
<#macro writeUnitDates unitDates>
  <ead:unitdatestructured>
  <#if (unitDates?size>1)>
  <ead:dateset>
  </#if>
  <#list unitDates as unitDate>
  <@writeUnitDate unitDate />
  </#list>
  <#if (unitDates?size>1)>
  </ead:dateset>
  </#if>
  </ead:unitdatestructured>
</#macro>

<#-- Zapis datace -->
<#macro writeUnitDate unitDate>
<#local fromAttr="standarddate">
<#local toAttr="standarddate">
<#local dateRangeLocaltype="">
<#if (unitDate.type.code=="ZP2015_DATE_OTHER")>
  <#if dateOtherMapping?keys?seq_contains(unitDate.specification.code)>
    <#local dateRangeLocaltype=dateOtherMapping[unitDate.specification.code]>
  </#if>
</#if>
<#if unitDate.unitDate.valueFromEstimated>
  <#local fromAttr="notbefore">
</#if>
<#if unitDate.unitDate.valueToEstimated>
  <#local toAttr="notafter">
</#if>
    <ead:daterange altrender="${unitDate.unitDate.format}" <#if dateRangeLocaltype!="">localtype="${dateRangeLocaltype}"</#if> >
      <ead:fromdate ${fromAttr}="${unitDate.unitDate.valueFrom}">${unitDate.valueFrom}</ead:fromdate>
      <ead:todate ${toAttr}="${unitDate.unitDate.valueTo}">${unitDate.valueTo}</ead:todate>
    </ead:daterange>
</#macro>

<#macro writeRelations relations>
  <#-- Role entit, souřadnice, autorské dílo -->
  <ead:relations>
  <#list relations as item>
    <#switch item.type.code>
    <#case "ZP2015_POSITION">
      <#lt>    <ead:relation relationtype = "otherrelationtype"  otherrelationtype="COORDINATES">
      <#lt>      <ead:geogname>
      <#lt>        <ead:part>5.2.6 Souřadnice</ead:part>
      <#lt>        <ead:geographiccoordinates
      <#lt>             coordinatesystem="WGS84">${item.base64Value}</ead:geographiccoordinates>
      <#lt>      </ead:geogname>
      <#lt>    </ead:relation>
      <#break>
    <#case "ZP2015_ITEM_TITLE_REF">
      <#lt>    <ead:relation relationtype="resourcerelation" linktitle="autorské dílo" linkrole="ARTWORK">
      <#lt>      <ead:relationentry>${item.record.preferredPart.value}</ead:relationentry>
      <#lt>      <ead:descriptivenote><ead:p><ead:ptr target="ap${item.record.id?c}" /></ead:p></ead:descriptivenote>      
      <#lt>    </ead:relation>
      <#break>      
    <#case "ZP2015_ENTITY_ROLE">
      <#local relationType="resourcerelation">
      <#switch item.record.type.parentType.code>
        <#case "PERSON">
          <#local relationType="cpfrelation">
          <#break>
        <#case "DYNASTY">
          <#local relationType="cpfrelation">
          <#break>
        <#case "PARTY_GROUP">
          <#local relationType="cpfrelation">
          <#break>
        <#case "EVENT">
          <#local relationType="cpfrelation">
          <#break>
      </#switch>
      <#local linkrole="ENTITY">
      <#local linktitle="související entita">
      <#if (linkroleMapping?keys?seq_contains(item.specification.code)) >
        <#local linkrole=linkroleMapping[item.specification.code]>
      </#if>
      <#if (linktitleMapping?keys?seq_contains(linkrole)) >
        <#local linktitle=linktitleMapping[linkrole]>
      </#if>
      <#lt>    <ead:relation relationtype="${relationType}" linktitle="${linktitle}" linkrole="${linkrole}">
      <#lt>      <ead:relationentry>${item.record.preferredPart.value}</ead:relationentry>
      <#lt>      <ead:descriptivenote><ead:p><ead:ptr target="ap${item.record.id?c}" /></ead:p></ead:descriptivenote>      
      <#lt>    </ead:relation>
      <#break>
    </#switch>
  </#list>
  </ead:relations>
</#macro>
