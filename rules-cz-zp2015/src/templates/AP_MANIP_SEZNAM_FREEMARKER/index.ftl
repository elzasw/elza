<#list output.getNodesDFS() as node>
"${node.getNode().getItemsValueByCode("ZP2015_TITLE")}";"${node.getNode().getItemsValueByCode("ZP2015_LEVEL_TYPE")}";"${node.getNode().getItemsValueByCode("ZP2015_UNIT_ID")}";"${node.getNode().getItemsValueByCode("ZP2015_NAD")}";"${node.getNode().getItemsValueByCode("ZP2015_UNIT_DATE")}";"${node.depth}"
</#list>
