package scripts

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
        String prefix = matcher.group(1)
        if (StringUtils.isNotEmpty(prefix)) {
            result.addItem("SRD_PACKET_PREFIX", prefix)
        }

        Integer packetNumber = Integer.parseInt(matcher.group(2))
        result.addItem("SRD_PACKET_NUMBER", packetNumber)

        String postfix = matcher.group(3)
        if (StringUtils.isNotEmpty(postfix)) {
            result.addItem("SRD_PACKET_POSTFIX", postfix)
        }
    }
}
