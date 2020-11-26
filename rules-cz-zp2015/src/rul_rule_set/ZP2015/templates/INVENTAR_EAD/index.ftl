<#ftl output_format="XML"><ead:ead xmlns:ead="http://ead3.archivists.org/schema/">

<#-- Zapis jazyku, vola se jen pokud existuje alespon jeden jazyk -->
<#macro writeLangMaterials items>
  <!-- Jazyky JP -->
  <ead:langmaterial>
  <#list items?filter(langItem -> langItem.type.code=="ZP2015_LANGUAGE") as langItem>
  <ead:language langcode="${langItem.specification.code}">${langItem.specification.name}</ead:language>
  </#list>
  </ead:langmaterial>
</#macro>

<#macro writeAp ap localtype>
<#switch ap.type.parentType>
  <#case "PERSON">
    <#local tagname="persname">
    <#break>
  <#case "DYNASTY">
    <#local tagname="famname">
    <#break>
  <#case "GROUP_PARTY">
    <#local tagname="corpname">
    <#break>
  <#default>
    <#local tagname="name">
    <#break>
</#switch>
    <ead:${tagname} localtype="${localtype}" identifier="${ap.uuid}">
      <ead:part>${ap.preferredPart.value}</ead:part>
    </ead:${tagname}>
</#macro>

<#macro writePocetEj items>
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
</#macro>

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
    <#list items?filter(item -> item.type.code=="ZP2015_FINDING_AID_APPROVED_BY") as item>
    <!-- Schválil archivni pomucku -->
    <ead:p><ead:name localtype="FINDING_AID_APPROVED_BY"><ead:part>${item.serializedValue}</ead:part></ead:name></ead:p>
    </#list>    
    <#list items?filter(item -> item.type.code=="ZP2015_RELEASE_DATE_PLACE") as item>
    <!-- Datum a misto vydani  --> 
    <ead:date localtype="RELEASE_DATE_PLACE">${item.serializedValue}</ead:date>
    </#list>    
    <#list items?filter(item -> item.type.code=="ZP2015_DESCRIPTION_DATE") as item>
    <!-- Datum popisu --> 
    <ead:date localtype="DESCRIPTION_DATE">${item.serializedValue}</ead:date>
    </#list>    
    <#list items?filter(item -> item.type.code=="ZP2015_FINDING_AID_DATE") as item>
    <!-- Datum zachyceneho stavu --> 
    <ead:date localtype="FINDING_AID_DATE">${item.serializedValue}</ead:date>
    </#list>
    
    <#-- Sestavil -->
    <#list items?filter(item -> item.type.code=="ZP2015_FINDING_AID_EDITOR") as item>
    <!-- Sestavil/editor archivni pomucky -->
    <ead:p><ead:name localtype="FINDING_AID_EDITOR"><ead:part>${item.serializedValue}</ead:part></ead:name></ead:p>
    </#list>
    
    <#-- Puvodce - strukt.typ -->
    <#list items?filter(item -> item.type.code=="ZP2015_ORIGINATOR_SIMPLE") as structitem>
    <!-- Původce v uvodu archivni pomucky -->
    <#list structitem.value.items?filter(item -> item.type.code=="ZP2015_ORIGINATOR") as item>
    <ead:p>
      <@writeAp item.record "ORIGINATOR" />
      <#list structitem.value.items?filter(item -> item.type.code!="ZP2015_ORIGINATOR") as otheritem>
        <ead:quote localtype="${otheritem.type.code}">${otheritem.serializedValue}</ead:quote>
      </#list>
    </ead:p>
    </#list>
    </#list>
    
    <#-- Zpracovatel -->
    <#list items?filter(item -> item.type.code=="ZP2015_ARRANGER") as item>
    <!-- Zpracovatel v uvodu archivni pomucky --> 
    <ead:p><@writeAp item.record "ARRANGER" /></ead:p>
    </#list>
    <#-- Zpracovatel (textove) -->
    <#list items?filter(item -> item.type.code=="ZP2015_ARRANGER_TEXT") as item>
    <!-- Zpracovatel v tirazi archivni pomucky --> 
    <ead:p><ead:name localtype="ARRANGER_BRIEF"><ead:part>${item.serializedValue}</ead:part></ead:name></ead:p>
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

<ead:control>
  <ead:recordid><#if output.internalCode?has_content>${output.internalCode}<#else>${output.name}</#if></ead:recordid>
  <!-- Archivní soubor, encodinganalog obsahuje identifikátor archivního souboru  -->
  <ead:filedesc <#if output.fund.fundNumber?has_content>encodinganalog="${output.fund.fundNumber?c}"</#if>  >
    <ead:titlestmt>
     <!-- Povinný název archivního souboru -->
     <ead:titleproper>${output.fund.name}</ead:titleproper>
     <!-- Název archivní pomůcky -->
     <ead:subtitle>${output.name}</ead:subtitle>
    </ead:titlestmt>    
  <@writePublStmt output.items />
  <@writeNoteStmt output.items />
  
  </ead:filedesc>
  <ead:maintenancestatus value="new" />
  <ead:publicationstatus value="inprocess" />
  <ead:maintenanceagency>
    <!-- Identifikátor z číselníku archivů -->
    <ead:agencycode localtype="PEvA">${output.fund.institution.code}</ead:agencycode>
    <!-- Jméno archivu -->
    <ead:agencyname>${output.fund.institution.record.preferredPart.value}</ead:agencyname>
  </ead:maintenanceagency>
    
  <#-- Typ pomůcky - ZP2015_OUTPUT_TYPE -->  
  <#list output.items?filter(item -> item.type.code=="ZP2015_OUTPUT_TYPE") as item>
  <!-- Druh pomůcky -->
  <ead:localcontrol localtype="FINDING_AID_TYPE">
    <ead:term>${item.specification.name}</ead:term>
  </ead:localcontrol>
  </#list>
  
  <#-- Pocet JP -->
  <#list output.items?filter(item -> item.type.code=="ZP2015_UNIT_COUNT_SUM") as item>
  <!-- Součet JP --> 
  <ead:localcontrol localtype="TOTAL_UNIT_COUNT">
    <ead:term>${item.serializedValue}</ead:term>
  </ead:localcontrol>
  </#list>
  <!-- Počet EJ -->
  <@writePocetEj output.items />

  <#-- Rozsah archiválií - ZP2015_UNITS_AMOUNT -->
  <#list output.items?filter(item -> item.type.code=="ZP2015_UNITS_AMOUNT") as item>
  <!-- Rozsah zpristupnenych archivalii --> 
  <ead:localcontrol localtype="UNITS_AMOUNT">
    <ead:term>${item.serializedValue}</ead:term>
  </ead:localcontrol>
  </#list>
      
  <#-- Časové rozmezí v tiráži -->
  <#list output.items?filter(item -> item.type.code=="ZP2015_DATE_RANGE") as item>
  <!-- Časové rozmezí v tiráži --> 
  <ead:localcontrol localtype="DATE_RANGE">
    <ead:term>${item.serializedValue}</ead:term>
  </ead:localcontrol>
  </#list>
  
  <#--  Deklarovane jazyky -->
  <#list output.items?filter(item -> item.type.code=="ZP2015_LANGUAGE") as item>
  <ead:localcontrol localtype="LANGUAGE">
    <ead:term>${item.specification.code}</ead:term>
  </ead:localcontrol>
  </#list>

    <ead:maintenancehistory>
    <ead:maintenanceevent>
      <ead:eventtype value="created"></ead:eventtype>
      <ead:eventdatetime standarddatetime="${output.changeDateTime}">${output.changeDateTime}</ead:eventdatetime>
      <!-- Typ vytvoření popisu machine|human -->
      <ead:agenttype value="machine"></ead:agenttype>
      <!-- Jméno agenta -->
      <ead:agent>ELZA</ead:agent>
      <ead:eventdescription>Finding aid created.</ead:eventdescription>
    </ead:maintenanceevent>
  </ead:maintenancehistory>  
</ead:control>
<#assign endtags=[]>

<#macro writeTags levelindex>
<#-- <prewritetags ${levelindex}, size=${endtags?size} endtags=<#list endtags as t>${t}, </#list>> --> 
<#-- Check to close previouse tag -->
<#if (levelindex<=endtags?size)>
  <#list endtags[(endtags?size)-1..levelindex-1] as tag>${tag?no_esc}</#list>  
  <#assign endtags = endtags[0..<(levelindex-1)]>
</#if>
<#--<postwritetags ${levelindex}, endtags=<#list endtags as t>${t}, </#list>> -->
</#macro>

<#macro writeOriginator ap>
  <ead:origination localtype="ORIGINATOR">
  <@writeAp ap "ORIGINATOR" />
  </ead:origination>
</#macro>

<#-- Write single dao object -->
<#macro writeDao node dao>
<ead:dao daotype="unknown" identifier="${dao.code}">
</ead:dao>
</#macro>


<#macro writeUklJednotka item>
  <!-- Ukladaci jednotka -->
  <ead:container>${item.serializedValue}</ead:container>
</#macro>

<#-- Zapis datace -->
<#macro writeUnitDate unitDate>
<#local fromAttr="standarddate">
<#local toAttr="standarddate">
<#if unitDate.unitDate.valueFromEstimated>
  <#local fromAttr="notbefore">
</#if>
<#if unitDate.unitDate.valueToEstimated>
  <#local toAttr="notafter">
</#if>
  <ead:unitdatestructured>
    <ead:daterange>
      <ead:fromdate ${fromAttr}="${unitDate.unitDate.valueFrom}">${unitDate.valueFrom}</ead:fromdate>
      <ead:todate ${toAttr}="${unitDate.unitDate.valueTo}">${unitDate.valueTo}</ead:todate>
    </ead:daterange>
  </ead:unitdatestructured>
</#macro>

<#macro writeRelations items>
  <!-- Role entit -->
  <ead:relations>
  <#list items as item>
    <#if item.type.code=="ZP2015_ENTITY_ROLE">
      <ead:relation relationtype="resourcerelation" encodinganalog="${item.record.uuid}" linkrole="${item.specification.code}">
        <ead:relationentry>${item.record.preferredPart.value}</ead:relationentry>
      </ead:relation>
    </#if>
  </#list>
  </ead:relations>
</#macro>

<#-- Write did -->
<#macro writeDid node>
<#local languagesProcessed=0>
<ead:did>
<#list node.items as item>
<#switch item.type.code>
<#case "ZP2015_UNIT_ID">
  <ead:unitid localtype="ReferencniOznaceni">${item.serializedValue}</ead:unitid>
  <#break>
<#case "ZP2015_TITLE">
  <ead:unittitle>${item.serializedValue}</ead:unittitle>
  <#break>
<#case "ZP2015_UNIT_DATE">
  <@writeUnitDate item />
  <#break>
<#case "ZP2015_ORIGINATOR">
  <@writeOriginator item.record />
  <#break>
<#case "ZP2015_UNIT_CURRENT_STATUS">
  <ead:physdesc>${item.serializedValue}</ead:physdesc>
  <#break>
<#case "ZP2015_LANGUAGE">
  <#if languagesProcessed==0>
    <@writeLangMaterials node.items />
    <#local languagesProcessed=1>
  </#if>
  <#break>
<#case "ZP2015_STORAGE_ID">
  <@writeUklJednotka item />
  <#break>
</#switch>
</#list>
<#-- Write DAOs -->
<#if (node.daos?size==1)>
 <@writeDao node node.daos?first /> 
</#if>
<#if (node.daos?size>1)>  
 <ead:daoset>
 <#list node.daos as dao>
   <@writeDao node dao />
 </#list>
 </ead:daoset>
</#if>
</ead:did>
</#macro>

<#-- Zápis jednoho uzlu -->
<#macro writeNode node>
<@writeDid node />
<#if !node.nodeId.published>
  <ead:otherfindaid localtype="MightExist"><ead:p>Pro úroveň popisu existují nebo vzniknou další archivní pomůcky.</ead:p></ead:otherfindaid>
</#if>
<#-- Elements outside did -->
<#local relationsProcessed=0>
<#list node.items as item>
<#switch item.type.code>
<#case "ZP2015_ENTITY_ROLE">
  <#if relationsProcessed==0>
    <@writeRelations node.items />
    <#local relationsProcessed=1>
  </#if>
  <#break>
<#case "ZP2015_UNIT_HIST">
  <ead:custodhist><ead:p>${item.serializedValue}</ead:p></ead:custodhist>
  <#break>
<#case "ZP2015_UNIT_ARR">
  <ead:arrangement><ead:p>${item.serializedValue}</ead:p></ead:arrangement>
  <#break>
<#case "ZP2015_UNIT_CONTENT">
  <ead:scopecontent><ead:p>${item.serializedValue}</ead:p></ead:scopecontent>
  <#break>
<#case "ZP2015_UNIT_SOURCE">
  <ead:acqinfo><ead:p>${item.serializedValue}</ead:p></ead:acqinfo>
  <#break>
<#case "ZP2015_FUTURE_UNITS">
  <ead:accruals><ead:p>${item.serializedValue}</ead:p></ead:accruals>
  <#break>
</#switch>
</#list>
</#macro>

<#macro writeNodes nodes>
<#list nodes as node>
  <@writeTags node.depth />
  <#local tagname="ead:c">
  <#if node.depth==1>
    <#local tagname="ead:archdesc">    
  </#if>
  <#-- Write level type -->
  <#local level="otherlevel">
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
    <#local level="subseries">
    <#break>
  <#case "ZP2015_LEVEL_ITEM">
    <#local level="item">
    <#break>
  <#case "ZP2015_LEVEL_PART">
    <#local otherlevel="itempart">    
    <${tagname} level="otherlevel" otherlevel="itempart">
    <#break>  	
  </#switch>
  <${tagname} level="${level}" <#rt>
    <#if level=="otherlevel">otherlevel="${otherlevel}"</#if> <#t>
    base="https://archdesc.nacr.cz/dids/${node.uuid}" <#t>
  ><#lt>
<@writeNode node />

<#-- Closing part -->
<#-- ${node.depth} -->
<#if node.depth==1 && nodes?api.hasNext() >
  <ead:dsc>
  <#assign endtags=endtags+["</ead:dsc></"+tagname+">"]>
<#else>
  <#assign endtags=endtags+["</"+tagname+">"]>
</#if>
  
</#list>
<#-- Write closing tags -->
<@writeTags 1 />
</#macro>

<#-- <ead:archdesc level="otherlevel" otherlevel="findingaidroot">
  <@writeNode output />
  <ead:dsc>
  <@writeNodes output.createFlatNodeIterator() /> 
  </ead:dsc>
</ead:archdesc> -->

<@writeNodes output.createFlatNodeIterator() />

</ead:ead>
