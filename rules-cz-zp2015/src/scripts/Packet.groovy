package cz.tacr.elza.zp2015.packet.generator

import groovy.transform.Field
import cz.tacr.elza.domain.ArrData
import cz.tacr.elza.domain.ArrStructuredItem
import cz.tacr.elza.domain.RulItemSpec
import cz.tacr.elza.packageimport.xml.SettingStructTypeSettings
import org.apache.commons.lang3.StringUtils

// result is global parameter for storing result
@Field def result = RESULT;

@Field List<ArrStructuredItem> items = ITEMS

@Field SettingStructTypeSettings structTypeSettings = STRUCTURE_TYPE_SETTINGS;

@Field int packetLeadingZeros = 8;

generate()
return;

void generate()
{
    StringBuilder valueBuilder = new StringBuilder();
    StringBuilder sortValueBuilder = new StringBuilder();
    
    // Fixed prefix
    appendValue(valueBuilder, "ZP2015_PACKET_FIXED_PREFIX");
    appendValue(sortValueBuilder, "ZP2015_PACKET_FIXED_PREFIX");    
    
    // User defined prefix
    appendValue(valueBuilder, "ZP2015_PACKET_PREFIX");
    appendValue(sortValueBuilder, "ZP2015_PACKET_PREFIX");
    
    // separator
    if(structTypeSettings!=null&&valueBuilder.length()>0) {
        String pfs = structTypeSettings.getPropertyValue("prefixSeparator");
        if(pfs!=null) {
            valueBuilder.append(pfs);
        }
    }

    // Packet number
    String startNumber = toStringValue("ZP2015_PACKET_START_NUMBER");
    if(StringUtils.isNotBlank(startNumber)) {
        valueBuilder.append(startNumber).append("-");
    }

    // Packet number
    String number = toStringValue("ZP2015_PACKET_NUMBER");
    if(StringUtils.isNotBlank(number)) {
        // append zeroes
        int addZeros = packetLeadingZeros - number.length();
        if (addZeros > 0) {
            sortValueBuilder.append(StringUtils.repeat("0", addZeros));
        }
        valueBuilder.append(number);
        sortValueBuilder.append(number);
    }
    
    // Postfix
    String postfix = toStringValue("ZP2015_PACKET_POSTFIX");
    if(StringUtils.isNotBlank(postfix)) {
        // separator
        if(structTypeSettings!=null&&valueBuilder.length()>0) {
          String pfs = structTypeSettings.getPropertyValue("postfixSeparator");
          if(pfs!=null) {
              valueBuilder.append(pfs);
          }
        }
        valueBuilder.append(postfix);
        sortValueBuilder.append(postfix);
    }
        
    // Prepare complement
    StringBuilder complementBuilder = new StringBuilder();
    appendValue(complementBuilder, "ZP2015_PACKET_TYPE");
    appendValue(complementBuilder, "ZP2015_PACKET_LOCATION");
    appendValue(complementBuilder, "ZP2015_PACKET_DEPOSITORY");
    appendValue(complementBuilder, "ZP2015_PACKET_ROW");
    appendValue(complementBuilder, "ZP2015_PACKET_COLUMN");
    appendValue(complementBuilder, "ZP2015_PACKET_SHELF");
    
    // store result
    result.setValue(valueBuilder.toString().trim());
    result.setSortValue(sortValueBuilder.toString().trim());
    result.setComplement(complementBuilder.toString().trim());
}

String toStringValue(String itemTypeCode) {
    StringBuilder sb = new StringBuilder()
    appendValue(sb, itemTypeCode);
    return sb.toString();
}

void appendValueWithSpecs(StringBuilder sb, String itemTypeCode, List<String> specs) {    
    for (ArrStructuredItem item : items) {
        if (item.getItemType().getCode().equalsIgnoreCase(itemTypeCode)) {
            RulItemSpec spec = item.getItemSpec();
            if(specs.contains(spec.getCode())) {
                if(sb.length()>0) {
                    sb.append(" ");
                }
                sb.append(spec.getName());
                ArrData data = item.getData();
                if(data!=null) {
                    String fullText = data.getFulltextValue();
                    if(fullText!=null) {
                        sb.append(fullText);
                    }
                }
            }
        }
    }
}

void appendValue(StringBuilder sb, String itemTypeCode)
{
    for (ArrStructuredItem item : items) {
        if (item.getItemType().getCode().equalsIgnoreCase(itemTypeCode)) {
            if(sb.length()>0) {
                sb.append(" ");
            }

            RulItemSpec spec = item.getItemSpec();
            if(spec!=null) {
                sb.append(spec.getShortcut());
            }
            ArrData data = item.getData();
            if(data!=null) {
                String fullText = data.getFulltextValue();
                if(fullText!=null) {
                    sb.append(fullText);
                }
            }
        }
    }
}
