package cz.tacr.elza.core.tree;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.LevelRepository;

@Service
public class FundTreeProvider {

    private final LevelRepository levelRepository;

    private final FundVersionRepository fundVersionRepository;

    @Autowired
    public FundTreeProvider(LevelRepository levelRepository, FundVersionRepository fundVersionRepository) {
        this.levelRepository = levelRepository;
        this.fundVersionRepository = fundVersionRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public FundTree getFundTree(Integer fundVersionId) {
        Validate.notNull(fundVersionId);

        return createFundTree(fundVersionId);
    }

    private FundTree createFundTree(Integer fundVersionId) {
        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
        if (fundVersion == null) {
            throw new SystemException("Fund version not found", BaseCode.ID_NOT_EXIST).set("fundVersionId", fundVersionId);
        }

        TreeNodeImpl rootNode = new TreeNodeImpl(fundVersion.getRootNodeId());
        rootNode.setPosition(1);

        FundTree fundTree = new FundTree(fundVersion);
        fundTree.init(levelRepository);

        return fundTree;
    }
}
