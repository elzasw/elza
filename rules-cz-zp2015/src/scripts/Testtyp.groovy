package cz.tacr.elza.zp2015.aptest.generator

import cz.tacr.elza.domain.ArrData
import cz.tacr.elza.domain.ArrStructuredItem
import cz.tacr.elza.domain.RulItemSpec
import cz.tacr.elza.packageimport.xml.SettingStructTypeSettings
import groovy.transform.Field

// result is global parameter for storing result
@Field def result = RESULT;

@Field List<ArrStructuredItem> items = ITEMS

@Field SettingStructTypeSettings structTypeSettings = STRUCTURE_TYPE_SETTINGS;

@Field int packetLeadingZeros = 8;

generate()
return;

void generate() {
    StringBuilder valueBuilder = new StringBuilder();
    StringBuilder sortValueBuilder = new StringBuilder();

    appendValue(valueBuilder, "BRIEF_DESC");
    appendValue(sortValueBuilder, "BRIEF_DESC");

    appendValue(valueBuilder, "NM_MAIN");
    appendValue(sortValueBuilder, "NM_MAIN");

    // store result
    result.setValue(valueBuilder.toString().trim());
    result.setSortValue(sortValueBuilder.toString().trim());
    result.setComplement("doplněk test");
}

void appendValue(StringBuilder sb, String itemTypeCode) {
    for (ArrStructuredItem item : items) {
        if (item.getItemType().getCode().equalsIgnoreCase(itemTypeCode)) {
            RulItemSpec spec = item.getItemSpec();
            if (spec != null) {
                sb.append(spec.getShortcut());
            }
            ArrData data = item.getData();
            if (data != null) {
                String fullText = data.getFulltextValue();
                if (fullText != null) {
                    sb.append(fullText);
                }
            }
        }
    }
}
