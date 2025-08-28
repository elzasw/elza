package cz.tacr.elza.domain;

import java.util.Date;

import org.locationtech.jts.geom.Geometry;

/**
 * Lucene index data for description item. Can be externalized when node or data reference is not
 * available (e.g. detached hibernate proxy).
 */
public interface ArrDescItemIndexData {

    Integer getFundId();

    String getFulltextValue();

    Integer getValueInt();

    Double getValueDouble();

    Long getNormalizedFrom();

    Long getNormalizedTo();

    Date getValueDate();

    Geometry getValueGeometry();

    Boolean isValue();
}
