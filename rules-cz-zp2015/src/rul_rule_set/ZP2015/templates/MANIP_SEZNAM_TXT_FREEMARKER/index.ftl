${output.fund.institution.record.preferredPart.value}
${output.fund.name}
${output.fund.internalCode}

<#assign node = output.fund.rootNode>

************************************
1. Dějiny původce archivního souboru
************************************
<#list node.getItems(["ZP2015_ORIGINATOR"]) as originatorObj>
<#assign originator = originatorObj.record>
<#if originator.preferredPart.value??>
<#assign prefName = originator.preferredPart >

Preferovaná forma jména: ${prefName.value}

Datace použití jména od-do: <#if (prefName.getSingleItemValue("NM_USED_FROM"))??>${prefName.getSingleItemValue("NM_USED_FROM")}<#else>Neuvedeno</#if> - <#if (prefName.getSingleItemValue("NM_USED_TO"))??>${prefName.getSingleItemValue("NM_USED_TO")}<#else>Neuvedeno</#if>
</#if>

Variantní/Paralelní formy jména a jejich typy: <#list originator.getParts(['PT_NAME']) as name>
    ${name.getSingleItemValue('NM_MAIN')}
    <#sep>,
</#list>

Dějiny původce:
    <#list originator.getItems(["BIOGRAPHY"]) as originatorBio>
        ${originatorBio.serializedValue}
    </#list>
    <#--<#if (originator.history)??>${originator.history}</#if>-->

<#--Zdroje informací:
    <#if (originator.sourceInformation)??>${originator.sourceInformation}</#if>-->

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


<#list output.getItems(["ZP2015_UNIT_SOURCE"])>
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
<#if node.getSingleItemValue("ZP2015_LEVEL_TYPE") == "Série">
<#list 1..depth as x><#sep>   </#list>${depth} ${node.getSingleItemValue("ZP2015_TITLE")}
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
<#if (node.getSingleItemValue("ZP2015_TITLE"))??>${node.getSingleItemValue("ZP2015_TITLE")} -- </#if><#if (node.getSingleItemValue("ZP2015_UNIT_DATE"))??>${node.getSingleItemValue("ZP2015_UNIT_DATE")} -- </#if>${node.getSingleItemValue("ZP2015_LEVEL_TYPE")}<#if (node.getSingleItemValue("ZP2015_UNIT_TYPE"))??>/${node.getSingleItemValue("ZP2015_UNIT_TYPE")}</#if>
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
