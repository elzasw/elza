package scripts

import groovy.transform.Field
import org.apache.commons.lang3.StringUtils

import java.util.regex.Matcher
import java.util.regex.Pattern

/* A typed container value "A12b" becomes the prefix "A", the number 12 and the postfix "b". */

@Field def result = RESULT
@Field String value = VALUE

parse()
return

void parse() {
    Matcher matcher = Pattern.compile("(.*?)(\\d+)(.*)").matcher(value)
    if (matcher.find()) {
        String prefix = matcher.group(1).trim()
        if (StringUtils.isNotEmpty(prefix)) {
            result.addItem("ISADG_CONTAINER_PREFIX", prefix)
        }
        result.addItem("ISADG_CONTAINER_NUMBER", Integer.parseInt(matcher.group(2)))
        String postfix = matcher.group(3).trim()
        if (StringUtils.isNotEmpty(postfix)) {
            result.addItem("ISADG_CONTAINER_POSTFIX", postfix)
        }
    }
}
