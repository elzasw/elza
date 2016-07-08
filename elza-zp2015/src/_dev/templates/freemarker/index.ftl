<#list output.nodes as node>
"${node.getItemsValueByCode("ZP2015_TITLE")}";"${node.getItemsValueByCode("ZP2015_LEVEL_TYPE")}";"${node.getItemsValueByCode("ZP2015_UNIT_ID")}";"${node.getItemsValueByCode("ZP2015_NAD")}";"${node.getItemsValueByCode("ZP2015_UNIT_DATE")}";"${node.depth}"
</#list>
