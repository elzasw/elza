<#if (output.fund.institution.record)??>${output.fund.institution.record.preferredPart.value!""}<#else>Neuvedeno</#if>
${output.fund.name!""}
${output.fund.internalCode!""}

<#assign node = output.fund.rootNode>

************************************
1. Dějiny původce archivního souboru
************************************
<#list node.getItems(["SRD_ORIGINATOR"]) as originatorObj>
<#if (originatorObj.record)??>
<#assign originator = originatorObj.record>
<#if (originator.preferredPart)??>
<#assign prefName = originator.preferredPart>

Preferovaná forma jména: ${prefName.value!""}

</#if>

Variantní/Paralelní formy jména a jejich typy: <#list originator.parts as part>${part.value!""}<#sep>, </#list>

</#if>
</#list>

****************************
2. Dějiny archivního souboru
****************************

<#list output.getItems(["SRD_UNIT_HIST"])>
Dějiny jednotek popisu:

<#items as item>
${item.serializedValue!""}
</#items>
</#list>


<#list output.getItems(["SRD_UNIT_SOURCE"])>
Přímý zdroj akvizice:

<#items as item>
${item.serializedValue!""}
</#items>
</#list>

**********************************************
3. Archivní charakteristika archivního souboru
**********************************************

<#list output.createFlatNodeIterator() as node>
<#assign depth=node.depth-1>
<#if (node.getSingleItemValue("SRD_LEVEL_TYPE")!"") == "Série">
<#list 1..depth as x><#sep>   </#list>${depth} ${node.getSingleItemValue("SRD_TITLE")!""}
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
<#if (node.getSingleItemValue("SRD_TITLE"))??>${node.getSingleItemValue("SRD_TITLE")} -- </#if><#if (node.getSingleItemValue("SRD_UNIT_DATE"))??>${node.getSingleItemValue("SRD_UNIT_DATE")} -- </#if>${node.getSingleItemValue("SRD_LEVEL_TYPE")!""}<#if (node.getSingleItemValue("SRD_UNIT_TYPE"))??>/${node.getSingleItemValue("SRD_UNIT_TYPE")}</#if>
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
