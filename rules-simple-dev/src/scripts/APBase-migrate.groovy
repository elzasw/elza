package scripts

import cz.tacr.elza.service.vo.AccessPointMigrate
import cz.tacr.elza.service.vo.NameMigrate

AccessPointMigrate ap = AP
return process(ap)

static AccessPointMigrate process(AccessPointMigrate ap) {

    if (ap.getDescription() != null) {
        ap.addItem("AP_DESCRIPTION", null, ap.getDescription())
    }

    processNames(ap.getNames());

    return ap
}

static void processNames(List<NameMigrate> names) {
    for (int i = 0; i < names.size(); i++) {
        NameMigrate name = names.get(i)
        name.addItem("AP_NAME", null, name.getName());
        if (name.getComplement() != null) {
            name.addItem("AP_COMPLEMENT", null, name.getComplement())
        }
    }
}
