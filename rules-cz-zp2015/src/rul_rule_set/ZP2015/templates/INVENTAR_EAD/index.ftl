<#ftl output_format="XML"><ead:ead xmlns:ead="http://ead3.archivists.org/schema/">
<ead:control>
  <ead:recordid><#if output.internalCode?has_content>${output.internalCode}<#else>null</#if></ead:recordid>
  <!-- Archivní soubor, encodinganalog obsahuje identifikátor archivního souboru  -->
  <ead:filedesc encodinganalog="${output.fund.rootNode.getSingleItemValue("ZP2015_NAD")}">
    <ead:titlestmt>
     <!-- Povinný název archivního souboru -->
     <ead:titleproper>${output.fund.name}</ead:titleproper>
    </ead:titlestmt>
  </ead:filedesc>
  <ead:maintenancestatus value="new" />
  <ead:publicationstatus value="inprocess" />
  <ead:maintenanceagency>
    <!-- Identifikátor z číselníku archivů -->
    <ead:agencycode>${output.fund.institution.code}</ead:agencycode>
    <!-- Jméno archivu -->
    <ead:agencyname>${output.fund.institution.partyGroup.record.prefName.name}</ead:agencyname>
  </ead:maintenanceagency>
  <ead:maintenancehistory>
    <ead:maintenanceevent>
      <ead:eventtype value="created"></ead:eventtype>
      <ead:eventdatetime></ead:eventdatetime>
      <!-- Typ vytvoření popisu machine|human -->
      <ead:agenttype value="machine"></ead:agenttype>
      <!-- Jméno agenta -->
      <ead:agent>ELZA</ead:agent>
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

<#macro writeParty party>
<#switch party.partyType>
  <#case "PERSON">
    <ead:persname id="p${party.partyId}">
      <ead:part>${party.name.fullName}</ead:part>
    </ead:persname>
    <#break>
  <#case "DYNASTY">
    <ead:famname id="p${party.partyId}">
      <ead:part>${party.name.fullName}</ead:part>
    </ead:famname>
    <#break>
  <#case "GROUP_PARTY">
    <ead:corpname id="p${party.partyId}">
      <ead:part>${party.name.fullName}</ead:part>
    </ead:corpname>
    <#break>
  <#case "EVENT">
    <ead:name id="p${party.partyId}">
      <ead:part>${party.name.fullName}</ead:part>
    </ead:name>
    <#break>
</#switch>
</#macro>

<#macro writeNode node>
<ead:did>
<#list node.items as item>
<#switch item.type.code>
<#case "ZP2015_UNIT_ID">
  <ead:unitid localtype="ReferencniOznaceni">${item.serializedValue}</ead:unitid>
  <#break>
<#case "ZP2015_TITLE">
  <ead:abstract>${item.serializedValue}</ead:abstract>
  <#break>
<#case "ZP2015_UNIT_DATE">
  <#assign structDate=item.unitDate>
  <ead:unitdatestructured>
    <ead:daterange>
      <ead:fromdate standarddate="${structDate.valueFrom}">${structDate.valueFrom}</ead:fromdate>
      <ead:todate standarddate="${structDate.valueTo}">${structDate.valueTo}</ead:todate>
    </ead:daterange>
  </ead:unitdatestructured>
  <#break>
<#case "ZP2015_ORIGINATOR">
  <ead:origination>
    <@writeParty item.party />
  </ead:origination>
  <#break>
</#switch>
</#list>
</ead:did>
<#list node.items as item>
<#switch item.type.code>
<#case "ZP2015_UNIT_HIST">
  <ead:custodhist><ead:p>${item.serializedValue}</ead:p></ead:custodhist>
  <#break>
</#switch>
</#list>
</#macro>

<#list output.createFlatNodeIterator() as node>
<#if node.depth == 1>
<ead:archdesc level="file">
<@writeNode node />
<ead:dsc>
<#assign endtags=endtags+["</ead:dsc></ead:archdesc>"]>
<#else>
  <@writeTags node.depth />
  <#-- Write level type -->
  <#switch node.getSingleItem("ZP2015_LEVEL_TYPE").specification.code>
  <#case "ZP2015_LEVEL_SERIES">
    <ead:c level="series">
    <#break>
  <#case "ZP2015_LEVEL_FOLDER">
    <ead:c level="subseries">
    <#break>
  <#case "ZP2015_LEVEL_ITEM">
    <ead:c level="item">
    <#break>
  <#case "ZP2015_LEVEL_PART">
    <ead:c>
    <#break>
  <#default>
  	<ead:c>
  </#switch>
<@writeNode node />
  <#-- ${node.depth} -->
  <#assign endtags=endtags+["</ead:c>"]>
</#if>
</#list>
<#-- Write closing tags -->
<@writeTags 1 />
</ead:ead>
