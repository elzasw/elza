package scripts

/*
 * Maps an element of <did> of the inherent archival description of an AIP to an item type
 * of a digital entity.
 *
 * Input:
 *   CLASS_NAME - simple name of the class of the element, e.g. Unitdatestructured
 *   LOCAL_TYPE - local type of the element (of <daterange> for a date), e.g. CONTENT; null if none
 *
 * Result: null when the element is not taken over, the code of the item type, or
 * [itemType: <code>, itemSpec: <code>] for an item type with specification.
 */

/**
 * Local types of the other dates of a unit of description, see
 * https://stands.nacr.cz/ead/current/rozsireny-popis/jina-datace.html
 */
@groovy.transform.Field
static final Map<String, String> DATE_OTHER_SPECS = [
        "CONTENT"               : "ZP2015_DATE_OF_CONTENT",
        "DECLARED"              : "ZP2015_DATE_DECLARED",
        "ORIGIN"                : "ZP2015_DATE_ORIG",
        "COPY"                  : "ZP2015_DATE_OF_COPY",
        "SEALING"               : "ZP2015_DATE_SEALING",
        "ACT_PUBLISHING"        : "ZP2015_DATE_ACT_PUBLISHING",
        "INSERT"                : "ZP2015_DATE_INSERT",
        "MOLD_CREATION"         : "ZP2015_DATE_MOLD_CREATION",
        "USAGE"                 : "ZP2015_DATE_USAGE",
        "PUBLISHING"            : "ZP2015_DATE_PUBLISHING",
        "MAP_UPDATE"            : "ZP2015_DATE_MAP_UPDATE",
        "CAPTURING"             : "ZP2015_DATE_CAPTURING",
        "RECORDING"             : "ZP2015_DATE_RECORDING",
        "AWARDING"              : "ZP2015_DATE_AWARDING",
        "AWARD_CER"             : "ZP2015_DATE_AWARD_CER",
        "WITHDRAWAL"            : "ZP2015_DATE_WITHDRAWAL",
        "LEGALLY_EFFECTIVE_FROM": "ZP2015_DATE_LEGALLY_EFFECTIVE_FROM",
        "VALID_FROM"            : "ZP2015_DATE_VALID_FROM",
        "LEGALLY_EFFECTIVE_TO"  : "ZP2015_DATE_LEGALLY_EFFECTIVE_TO",
        "VALID_TO"              : "ZP2015_DATE_VALID_TO",
        "DISPATCH"              : "ZP2015_DATE_DISPATCH",
        "DELIVERY"              : "ZP2015_DATE_DELIVERY",
        "PROCESSING"            : "ZP2015_DATE_PROCESSING",
        "CLOSING"               : "ZP2015_DATE_CLOSING"
]

return generate(CLASS_NAME, binding.hasVariable("LOCAL_TYPE") ? LOCAL_TYPE : null)

static Object generate(final String className, final String localType) {
    switch (className) {
        case "Abstract":
            return "ZP2015_CONTENT"
        case "Unittitle":
            if (localType == "FORMAL_TITLE") {
                return "ZP2015_FORMAL_TITLE"
            }
            return localType == null ? "ZP2015_CONTENT" : null
        case "Unitdatestructured":
            if (localType == null) {
                return "ZP2015_UNIT_DATE"
            }
            // an unknown kind of date is not taken over rather than misread
            String spec = DATE_OTHER_SPECS[localType]
            return spec == null ? null : [itemType: "ZP2015_DATE_OTHER", itemSpec: spec]
        default:
            return null
    }
}
