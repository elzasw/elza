${output.fund.institution.partyGroup.name}
${output.fund.name}
${output.fund.internalCode}

************************************
1. Dějiny původce archivního souboru
************************************
<#assign node = output.getNode(output.fund.rootNodeId) >
<#list node.getItems(["ZP2015_ORIGINATOR"]) as originatorObj>
<#assign originator = originatorObj.getParty()>
<#if (originator.preferredName.serialize())??>
<#assign prefName = originator.preferredName >

Preferovaná forma jména: ${prefName.serialize()}

Datace použití jména od-do: <#if (prefName.validFrom.serialize())??>${prefName.validFrom.serialize()}<#else>Neuvedeno</#if> - <#if (prefName.validTo.serialize())??>${prefName.validTo.serialize()}<#else>Neuvedeno</#if>
</#if>

Variantní/Paralelní formy jména a jejich typy: <#list originator.getNames() as name>${name.serialize()} (${name.serialize()})<#sep>, </#list>

Dějiny původce:
    <#if (originator.history)??>${originator.history}</#if>

Zdroje informací:
    <#if (originator.sourceInformation)??>${originator.sourceInformation}</#if>

</#list>

****************************
2. Dějiny archivního souboru
****************************

<#list output.getItems(["ZP2015_UNIT_HIST"])>
Dějiny jednotek popisu:

<#items as item>
${item.serializedValue}
</#items>
</#list>


<#list output.getNodeItems(["ZP2015_UNIT_SOURCE"])>
Přímý zdroj akvizice:

<#items as item>
${item.serializedValue}
</#items>
</#list>

**********************************************
3. Archivní charakteristika archivního souboru
**********************************************

<#list output.nodesDFS as node>
<#assign depth=node.depth-1>
<#if node.getItemsValueByCode("ZP2015_LEVEL_TYPE") == "Série">
<#list 1..depth as x><#sep>   </#list>${depth} ${node.getItemsValueByCode("ZP2015_TITLE")}
</#if>
</#list>

*************************************
4. Tématický popis archivního souboru
*************************************

**********************************************************************
5. Záznam o uspořádání archivního souboru a sestavení archivní pomůcky
**********************************************************************

<#list output.getNodesDFS() as node>

<#if node.depth == 1>========================================================<#elseif node.depth == 2>--------------------------------------------------------</#if>
<#if (node.getItemsValueByCode("ZP2015_TITLE"))??>${node.getItemsValueByCode("ZP2015_TITLE")} -- </#if><#if (node.getItemsValueByCode("ZP2015_UNIT_DATE"))??>${node.getItemsValueByCode("ZP2015_UNIT_DATE")} -- </#if>${node.getItemsValueByCode("ZP2015_LEVEL_TYPE")}<#if (node.getItemsValueByCode("ZP2015_UNIT_TYPE"))??>/${node.getItemsValueByCode("ZP2015_UNIT_TYPE")}</#if>
<#if node.depth == 1>========================================================<#elseif node.depth == 4>""""""""""""""""""""""""""""""""""""""""""""""""""""""""<#else>--------------------------------------------------------</#if>
    <#list node.items>
    <#items as item>
    <#if item.type.code != "ZP2015_TITLE" && item.type.code != "ZP2015_UNIT_DATE" && item.type.code != "ZP2015_LEVEL_TYPE" && item.type.code != "ZP2015_UNIT_TYPE">

    ${item.type.name}<#if (item.serializedValue)??> : ${item.serializedValue}</#if>
    </#if>
    </#items>
    </#list>
    <#if (node.records)??>
    <#list node.records as record>
    </#list>
    </#if>
</#list>
