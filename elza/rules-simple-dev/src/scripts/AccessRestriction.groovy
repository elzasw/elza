package scripts

import groovy.transform.Field
import cz.tacr.elza.domain.ArrData
import cz.tacr.elza.domain.ArrStructuredItem
import cz.tacr.elza.domain.RulItemSpec
import org.apache.commons.lang3.StringUtils

// result is global parameter for storing result
@Field def result = RESULT;

@Field List<ArrStructuredItem> items = ITEMS

generate()
return;

void generate() {
    StringBuilder valueBuilder = new StringBuilder();
    StringBuilder complementBuilder = new StringBuilder();
    StringBuilder sortValueBuilder = new StringBuilder();
    
    appendValue(valueBuilder, "SRD_ACCESS_RESTRICTION_NAME");
    appendValue(sortValueBuilder, "SRD_ACCESS_RESTRICTION_NAME");
    appendValue(valueBuilder, "SRD_ACCESS_RESTRICTION_DESCRIPTION");
    appendValue(sortValueBuilder, "SRD_ACCESS_RESTRICTION_DESCRIPTION");

    // store result
    result.setValue(valueBuilder.toString().trim());
    result.setSortValue(sortValueBuilder.toString().trim());

    String complement = complementBuilder.toString().trim();
    result.setComplement(StringUtils.isEmpty(complement) ? null : complement);
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
