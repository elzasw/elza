${output.fund.institution.partyGroup.preferredName.formatWithAllDetails()}
${output.fund.name}
${output.fund.internalCode}

<#assign node = output.fund.rootNode>

************************************
1. Dějiny původce archivního souboru
************************************
<#list node.getItems(["SRD_ORIGINATOR"]) as originatorObj>
<#assign originator = originatorObj.party>
<#if originator.preferredName.formatWithAllDetails()??>
<#assign prefName = originator.preferredName >

Preferovaná forma jména: ${prefName.formatWithAllDetails()}

Datace použití jména od-do: <#if prefName.validFrom.valueText??>${prefName.validFrom.valueText}<#else>Neuvedeno</#if> - <#if (prefName.validTo.valueText)??>${prefName.validTo.valueText}<#else>Neuvedeno</#if>
</#if>

Variantní/Paralelní formy jména a jejich typy: <#list originator.names as name>${name.formatWithAllDetails()}<#sep>, </#list>

Dějiny původce:
    <#if (originator.history)??>${originator.history}</#if>

Zdroje informací:
    <#if (originator.sourceInformation)??>${originator.sourceInformation}</#if>

</#list>

****************************
2. Dějiny archivního souboru
****************************

<#list output.getItems(["SRD_UNIT_HIST"])>
Dějiny jednotek popisu:

<#items as item>
${item.serializedValue}
</#items>
</#list>


<#list output.getItems(["SRD_UNIT_SOURCE"])>
Přímý zdroj akvizice:

<#items as item>
${item.serializedValue}
</#items>
</#list>

**********************************************
3. Archivní charakteristika archivního souboru
**********************************************

<#list output.createFlatNodeIterator() as node>
<#assign depth=node.depth-1>
<#if node.getSingleItemValue("SRD_LEVEL_TYPE") == "Série">
<#list 1..depth as x><#sep>   </#list>${depth} ${node.getSingleItemValue("SRD_TITLE")}
</#if>
</#list>

*************************************
4. Tematický popis archivního souboru
*************************************

**********************************************************************
5. Záznam o uspořádání archivního souboru a sestavení archivní pomůcky
**********************************************************************

<#list output.createFlatNodeIterator() as node>

<#if node.depth == 1>========================================================<#elseif node.depth == 2>--------------------------------------------------------</#if>
<#if (node.getSingleItemValue("SRD_TITLE"))??>${node.getSingleItemValue("SRD_TITLE")} -- </#if><#if (node.getSingleItemValue("SRD_UNIT_DATE"))??>${node.getSingleItemValue("SRD_UNIT_DATE")} -- </#if>${node.getSingleItemValue("SRD_LEVEL_TYPE")}<#if (node.getSingleItemValue("SRD_UNIT_TYPE"))??>/${node.getSingleItemValue("SRD_UNIT_TYPE")}</#if>
<#if node.depth == 1>========================================================<#elseif node.depth == 4>""""""""""""""""""""""""""""""""""""""""""""""""""""""""<#else>--------------------------------------------------------</#if>
    <#list node.items>
    <#items as item>
    <#if item.type.code != "SRD_TITLE" && item.type.code != "SRD_UNIT_DATE" && item.type.code != "SRD_LEVEL_TYPE" && item.type.code != "SRD_UNIT_TYPE">

    ${item.type.name}<#if (item.serializedValue)??> : ${item.serializedValue}</#if>
    </#if>
    </#items>
    </#list>
    <#if (node.records)??>
    <#list node.records as record>
    </#list>
    </#if>
</#list>

