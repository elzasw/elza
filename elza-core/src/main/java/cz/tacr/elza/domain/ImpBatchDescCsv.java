package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * CSV import batch: adds description items into existing nodes. The target fund is resolved
 * from each row's node UUID at import time, so the batch itself carries no fund reference.
 */
@Entity(name = "imp_batch_desc_csv")
@DiscriminatorValue("ADD_DESC_ITEMS_CSV")
public class ImpBatchDescCsv extends ImpBatch {

    @Column(length = StringLength.LENGTH_ENUM)
    private String separator;

    @Column(length = StringLength.LENGTH_ENUM)
    private String encoding;

    public String getSeparator() { return separator; }
    public void setSeparator(String separator) { this.separator = separator; }

    public String getEncoding() { return encoding; }
    public void setEncoding(String encoding) { this.encoding = encoding; }
}
