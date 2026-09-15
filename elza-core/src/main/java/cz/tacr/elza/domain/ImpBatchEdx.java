package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * EDX2 import batch: parameters for matching (or creating) funds while the payload is parsed.
 */
@Entity(name = "imp_batch_edx")
@DiscriminatorValue("EDX2")
public class ImpBatchEdx extends ImpBatch {

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM, nullable = false)
    private FundImportStrategy fundImportStrategy;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM)
    private FundPairKey fundPairKey;

    @Column(nullable = false)
    private boolean ignoreRootNodes;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApScope.class)
    @JoinColumn(name = "scope_id")
    private ApScope scope;

    public FundImportStrategy getFundImportStrategy() { return fundImportStrategy; }
    public void setFundImportStrategy(FundImportStrategy fundImportStrategy) { this.fundImportStrategy = fundImportStrategy; }

    public FundPairKey getFundPairKey() { return fundPairKey; }
    public void setFundPairKey(FundPairKey fundPairKey) { this.fundPairKey = fundPairKey; }

    public boolean isIgnoreRootNodes() { return ignoreRootNodes; }
    public void setIgnoreRootNodes(boolean ignoreRootNodes) { this.ignoreRootNodes = ignoreRootNodes; }

    public ApScope getScope() { return scope; }
    public void setScope(ApScope scope) { this.scope = scope; }
}
