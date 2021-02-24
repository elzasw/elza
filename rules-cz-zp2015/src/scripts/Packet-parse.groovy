package cz.tacr.elza.zp2015.packet.parse

import groovy.transform.Field
import org.apache.commons.lang3.StringUtils

import java.util.regex.Matcher
import java.util.regex.Pattern

@Field def result = RESULT
@Field String value = VALUE

parse()
return;

void parse() {
    Pattern pattern = Pattern.compile("(.*?)(\\d+)(.*)");
    Matcher matcher = pattern.matcher(value);
    if (matcher.find()) {
        String prefix = matcher.group(1).trim()
        if (StringUtils.isNotEmpty(prefix)) {
            result.addItem("ZP2015_PACKET_PREFIX", prefix)
        }

        String numberPart = matcher.group(2);
        if(StringUtils.isNotEmpty(numberPart)) {
            Integer packetNumber = Integer.parseInt(numberPart)
            result.addItem("ZP2015_PACKET_NUMBER", packetNumber)
        }

        String postfix = matcher.group(3).trim()
        if (StringUtils.isNotEmpty(postfix)) {
            result.addItem("ZP2015_PACKET_POSTFIX", postfix)
        }
    } else {
        if (StringUtils.isNotEmpty(value)) {
            result.addItem("ZP2015_PACKET_PREFIX", value)
        }
    }
}
