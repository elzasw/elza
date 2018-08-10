package scripts

import cz.tacr.elza.service.AccessPointDataService
import cz.tacr.elza.service.vo.AccessPoint
import cz.tacr.elza.service.vo.Name
import cz.tacr.elza.service.vo.SimpleItem

AccessPoint ap = AP
return process(ap)

static AccessPoint process(AccessPoint ap) {
    StringBuilder description = new StringBuilder()

    description.append(processAp(ap))

    ap.setResult(description.toString())
    return ap
}

static String processAp(AccessPoint ap) {
    StringBuilder sb = new StringBuilder()

    Name preferredName = findPreferred(ap)
    processPreferredName(sb, preferredName)
    processBody(sb, ap)

    List<Name> names = findOther(ap)
    processNames(sb, names)

    return sb.toString()
}

static void processBody(StringBuilder sb, AccessPoint ap) {
    sb.append(" (")
    sb.append("UUID: ")
    sb.append(ap.getUuid())
    addValues(sb, true, ap.getItems(), "AP_DESCRIPTION")
    sb.append(")")
}

static void addValues(StringBuilder sb, boolean firstComma, List<SimpleItem> items, String... itemTypeCodes) {
    boolean next = firstComma
    if (itemTypeCodes != null && itemTypeCodes.size() > 0) {
        for (String itemTypeCode : itemTypeCodes) {
            String value = findValue(items, itemTypeCode)
            if (value != null) {
                if (!next) {
                    next = true
                } else {
                    sb.append(", ")
                }
                sb.append(value)
            }
        }
    }
}

static void processPreferredName(StringBuilder sb, Name name) {
    if (name == null) {
        sb.append("{PREFFERED_NAME_NOT_FOUND}")
    } else {
        processNameResult(name)
        addValues(sb, false, name.getItems(), "AP_NAME", "AP_COMPLEMENT")
    }
}

static void processNames(StringBuilder sb, List<Name> names) {
    for (int i = 0; i < names.size(); i++) {
        Name name = names.get(i)
        processNameResult(name)
        if (i == 0) {
            sb.append(", ")
        }
        addValues(sb, false, name.getItems(), "AP_NAME", "AP_COMPLEMENT")
        if (i + 1 < names.size()) {
            sb.append("; ")
        }
    }
}

static void processNameResult(Name name) {
    List<SimpleItem> items = name.getItems()
    String resultName = findValue(items, "AP_NAME")
    String resultComplement = findValue(items, "AP_COMPLEMENT")
    String resultFullName = AccessPointDataService.generateFullName(resultName, resultComplement)
    name.setResult(resultName, resultComplement, resultFullName)
}

static Name findPreferred(AccessPoint ap) {
    for (Name name : ap.getNames()) {
        if (name.isPreferredName()) {
            return name
        }
    }
    return null
}

static List<Name> findOther(AccessPoint ap) {
    List<Name> result = new ArrayList<>()
    for (Name name : ap.getNames()) {
        if (!name.isPreferredName()) {
            result.add(name)
        }
    }
    return result
}

static String findValue(List<SimpleItem> items, String itemTypeCode) {
    int add = 0
    StringBuilder result = new StringBuilder()
    for (SimpleItem item : items) {
        if (item.getType().equalsIgnoreCase(itemTypeCode)) {
            if (add > 0) {
                result.append(" ")
            }
            result.append(item.getValue())
            add++
        }
    }
    return add > 0 ? result.toString() : null
}
