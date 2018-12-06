package cz.tacr.elza.dataexchange.input.aps;

import java.util.Iterator;

import cz.tacr.elza.service.AccessPointDataService;
import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.domain.ApDescription;
import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.domain.SysLanguage;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.AccessPointName;
import cz.tacr.elza.schema.v2.AccessPointNames;

/**
 * Processing access points. Implementation is not thread-safe.
 */
public class AccessPointProcessor extends AccessPointEntryProcessor {

    public AccessPointProcessor(ImportContext context) {
        super(context, false);
    }

    @Override
    public void process(Object item) {
        AccessPoint ap = (AccessPoint) item;
        processEntry(ap.getApe());
        processDesc(ap.getChr());
        processNames(ap.getNms());
        // whole AP processed
        info.onProcessed();
    }

    private void processDesc(String value) {
        if (StringUtils.isEmpty(value)) {
            return;
        }
        ApDescription entity = new ApDescription();
        entity.setCreateChange(context.getCreateChange());
        entity.setDescription(value);
        context.addDescription(entity, info);
    }

    private void processNames(AccessPointNames names) {
        if (names == null || names.getNm().isEmpty()) {
            throw new DEImportException("Preferred AP name not found, apeId=" + entryId);
        }
        Iterator<AccessPointName> it = names.getNm().iterator();
        ApName prefName = createName(it.next());
        prefName.setPreferredName(true);
        context.addName(prefName, info);
        while (it.hasNext()) {
            ApName name = createName(it.next());
            context.addName(name, info);
        }
    }

    private ApName createName(AccessPointName name) {
        if (StringUtils.isEmpty(name.getN())) {
            throw new DEImportException("AP name without value, apeId=" + entryId);
        }
        SysLanguage lang = null;
        if (StringUtils.isNotEmpty(name.getL())) {
            lang = context.getSysLanguageByCode(name.getL());
            if (lang == null) {
                throw new DEImportException("AP name has invalid language apeId=" + entryId + ", code=" + name.getL());
            }
        }
        // create name
        ApName entity = new ApName();
        entity.setComplement(name.getCpl());
        entity.setCreateChange(context.getCreateChange());
        entity.setLanguage(lang);
        entity.setName(name.getN());
        entity.setFullName(AccessPointDataService.generateFullName(name.getN(), name.getCpl()));
        entity.setObjectId(context.nextNameObjectId());
        return entity;
    }
}
