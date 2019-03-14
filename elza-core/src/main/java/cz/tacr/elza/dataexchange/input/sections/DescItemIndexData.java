package cz.tacr.elza.dataexchange.input.sections;

import java.util.Date;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItemIndexData;

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
        if (data != null) {
            this.valueInt = data.getValueInt();
            this.valueDouble = data.getValueDouble();
            this.normalizedFrom = data.getNormalizedFrom();
            this.normalizedTo = data.getNormalizedTo();
            this.date = data.getDate();
        } else {
            this.valueInt = null;
            this.valueDouble = null;
            this.normalizedFrom = null;
            this.normalizedTo = null;
            this.date = null;
        }
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
