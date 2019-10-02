package scripts

import groovy.transform.Field
import cz.tacr.elza.domain.ArrData
import cz.tacr.elza.domain.ArrStructuredItem
import cz.tacr.elza.domain.RulItemSpec
import org.apache.commons.lang3.StringUtils

// result is global parameter for storing result
@Field def result = RESULT;

@Field List<ArrStructuredItem> items = ITEMS

@Field static int packetLeadingZeros = 8;

generate()
return;

void generate() {
    StringBuilder valueBuilder = new StringBuilder();
    StringBuilder complementBuilder = new StringBuilder();
    StringBuilder sortValueBuilder = new StringBuilder();
    
    // Fixed prefix
    appendValue(valueBuilder, "SRD_PACKET_FIXED_PREFIX");
    appendValue(sortValueBuilder, "SRD_PACKET_FIXED_PREFIX");
    
    // User defined prefix
    appendValue(valueBuilder, "SRD_PACKET_PREFIX");
    appendValue(sortValueBuilder, "SRD_PACKET_PREFIX");

    // Packet number    
    String number = toStringValue("SRD_PACKET_NUMBER");
    if(StringUtils.isNotBlank(number)) {
        // append zeroes
        int addZeros = Packet.packetLeadingZeros - number.length();
        if (addZeros > 0) {
            sortValueBuilder.append(StringUtils.repeat("0", addZeros));
        }
        valueBuilder.append(number);
        sortValueBuilder.append(number);
    }
    
    // Postfix
    appendValue(valueBuilder, "SRD_PACKET_POSTFIX");
    appendValue(sortValueBuilder, "SRD_PACKET_POSTFIX");

    // doplněk na zkoušku
    appendValue(complementBuilder, "SRD_PACKET_DESCRIPTION");
    
    // store result
    result.setValue(valueBuilder.toString().trim());
    result.setSortValue(sortValueBuilder.toString().trim());

    String complement = complementBuilder.toString().trim();
    result.setComplement(StringUtils.isEmpty(complement) ? null : complement);
}

String toStringValue(String itemTypeCode) {
    StringBuilder sb = new StringBuilder()
    appendValue(sb, itemTypeCode);
    return sb.toString();
}

void appendValue(StringBuilder sb, String itemTypeCode)
{
    for (ArrStructuredItem item : items) {
        if (item.getItemType().getCode().equalsIgnoreCase(itemTypeCode)) {
            RulItemSpec spec = item.getItemSpec();
            if(spec!=null) {
                sb.append(spec.getShortcut());
            }
            ArrData data = item.getData();
            if(data!=null) {
                sb.append(data.getFulltextValue());
            }
        }
    }
}
