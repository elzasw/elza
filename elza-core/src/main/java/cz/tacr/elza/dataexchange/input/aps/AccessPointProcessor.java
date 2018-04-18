package cz.tacr.elza.dataexchange.input.aps;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointInfo;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.domain.ApDescription;
import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.AccessPointEntry;
import cz.tacr.elza.schema.v2.AccessPointName;
import cz.tacr.elza.service.vo.ApAccessPointData;
import org.apache.commons.lang3.StringUtils;

import java.util.stream.Stream;

/**
 * Processing access points. Implementation is not thread-safe.
 */
public class AccessPointProcessor extends AccessPointEntryProcessor {

    private AccessPoint accessPoint;

    public AccessPointProcessor(ImportContext context) {
        super(context, false);
    }

    @Override
    public void process(Object item) {
        accessPoint = (AccessPoint) item;
        super.process(accessPoint.getApe());
    }

    @Override
    protected void validateAccessPointEntry(AccessPointEntry item) {
        super.validateAccessPointEntry(item);
        if (StringUtils.isBlank(accessPoint.getN())) {
            throw new DEImportException("AccessPoint name is not set, apeId:" + item.getId());
        }
    }

    @Override
    protected ApAccessPointData createAP(AccessPointEntry item) {
        ApAccessPointData record = super.createAP(item);
        ApName apName = new ApName();
        apName.setName(accessPoint.getN());
        record.setPreferredName(apName);
        ApDescription apDescription = new ApDescription();
        apDescription.setDescription(accessPoint.getChr());
        record.setCharacteristics(apDescription);
        return record;
    }

    @Override
    protected AccessPointInfo addAccessPoint(ApAccessPointData ap, String entryId) {
        AccessPointInfo info = super.addAccessPoint(ap, entryId);
        info.setName(accessPoint.getN());
        return info;
    }

    @Override
    protected void processSubEntities(AccessPointInfo apInfo) {
        super.processSubEntities(apInfo);
        processVariantNames(apInfo);
    }

    private void processVariantNames(AccessPointInfo apInfo) {
        // we skip the first one because it the preferred one
        Stream<AccessPointName> variantNamesStream = accessPoint.getNms().getNm().stream().skip(1);
        variantNamesStream.forEach(apName -> {
            ApName variantName = new ApName();
            variantName.setName(apName.getN());
            variantName.setPreferredName(false);
            context.addVariantName(variantName, apInfo);
        });
    }
}
