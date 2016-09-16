<#list output.getNodesDFS() as node>
"${node.getNode().getItemsValueByCode("SRD_TITLE")}";"${node.getNode().getItemsValueByCode("SRD_LEVEL_TYPE")}";"${node.getNode().getItemsValueByCode("SRD_UNIT_ID")}";"${node.getNode().getItemsValueByCode("SRD_NAD")}";"${node.getNode().getItemsValueByCode("SRD_UNIT_DATE")}";"${node.depth}"
</#list>
