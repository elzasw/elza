package cz.tacr.elza.dataexchange.input.sections;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItemIndexData;

import java.util.Date;

/**
 * Class with prepared data for fulltext indexing
 *
 */
class DescItemIndexData implements ArrDescItemIndexData {

    private final Integer fundId;

    private final String fulltext;

    private final Integer valueInt;

    private final Double valueDouble;

    private final Long normalizedFrom;

    private final Long normalizedTo;

    private final Date date;

    public DescItemIndexData(Integer fundId, String fulltext, ArrData data) {
        this.fundId = fundId;
        this.fulltext = fulltext;
        this.valueInt = data.getValueInt();
        this.valueDouble = data.getValueDouble();
        this.normalizedFrom = data.getNormalizedFrom();
        this.normalizedTo = data.getNormalizedTo();
        this.date = data.getDate();
    }

    @Override
    public Integer getFundId() {
        return fundId;
    }

    @Override
    public String getFulltextValue() {
        return fulltext;
    }

    @Override
    public Integer getValueInt() {
        return valueInt;
    }

    @Override
    public Double getValueDouble() {
        return valueDouble;
    }

    @Override
    public Long getNormalizedFrom() {
        return normalizedFrom;
    }

    @Override
    public Long getNormalizedTo() {
        return normalizedTo;
    }

    @Override
    public Date getValueDate() {
        return date;
    }
}
