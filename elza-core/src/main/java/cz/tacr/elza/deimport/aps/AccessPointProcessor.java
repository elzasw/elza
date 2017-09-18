package cz.tacr.elza.deimport.aps;

import org.apache.commons.lang3.StringUtils;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import cz.tacr.elza.deimport.DEImportException;
import cz.tacr.elza.deimport.aps.context.RecordImportInfo;
import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.domain.RegCoordinates;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.AccessPointEntry;
import cz.tacr.elza.schema.v2.AccessPointGeoLocation;
import cz.tacr.elza.schema.v2.AccessPointGeoLocations;
import cz.tacr.elza.schema.v2.AccessPointVariantNames;

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
    protected RegRecord createRecord(AccessPointEntry item) {
        RegRecord record = super.createRecord(item);
        record.setRecord(accessPoint.getN());
        record.setCharacteristics(accessPoint.getChr());
        record.setNote(accessPoint.getNote());
        return record;
    }

    @Override
    protected RecordImportInfo addAccessPoint(RegRecord record, String apeId) {
        RecordImportInfo info = super.addAccessPoint(record, apeId);
        info.setFulltext(accessPoint.getN());
        return info;
    }

    @Override
    protected void processSubEntities(RecordImportInfo recordInfo) {
        super.processSubEntities(recordInfo);
        processVariantNames(recordInfo);
        processGeoLocations(recordInfo);
    }

    private void processVariantNames(RecordImportInfo apInfo) {
        AccessPointVariantNames variantNames = accessPoint.getVnms();
        if (variantNames == null) {
            return;
        }
        for (String vn : variantNames.getVnm()) {
            RegVariantRecord variantRecord = new RegVariantRecord();
            variantRecord.setRecord(vn);
            context.addVariantName(variantRecord, apInfo);
        }
    }

    private void processGeoLocations(RecordImportInfo apInfo) {
        AccessPointGeoLocations geoLocations = accessPoint.getGlcs();
        if (geoLocations == null) {
            return;
        }
        for (AccessPointGeoLocation geoLocation : geoLocations.getGlc()) {
            RegCoordinates coordinates = new RegCoordinates();
            coordinates.setValue(convertGeoLocation(geoLocation.getV()));
            coordinates.setDescription(geoLocation.getNote());
            context.addGeoLocation(coordinates, apInfo);
        }
    }

    public static Geometry convertGeoLocation(String value) {
        WKTReader reader = new WKTReader();
        try {
            return reader.read(value);
        } catch (ParseException e) {
            throw new DEImportException("Failed to convert geo location:" + e.getMessage());
        }
    }
}
