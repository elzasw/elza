package cz.tacr.elza.core.fund;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.LevelRepository;

import static cz.tacr.elza.repository.ExceptionThrow.version;

@Service
public class FundTreeProvider {

    private final LevelRepository levelRepository;

    private final FundVersionRepository fundVersionRepository;

    @Autowired
    public FundTreeProvider(LevelRepository levelRepository, FundVersionRepository fundVersionRepository) {
        this.levelRepository = levelRepository;
        this.fundVersionRepository = fundVersionRepository;
    }

    @Transactional(TxType.MANDATORY)
    public FundTree getFundTree(Integer fundVersionId) {
        Validate.notNull(fundVersionId);

        return createFundTree(fundVersionId);
    }

    private FundTree createFundTree(Integer fundVersionId) {
        ArrFundVersion fundVersion = fundVersionRepository.findById(fundVersionId)
                .orElseThrow(version(fundVersionId));

        FundTree fundTree = new FundTree(fundVersion);
        fundTree.init(levelRepository);

        return fundTree;
    }
}
