package cz.tacr.elza.controller.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.controller.vo.ParDynastyEditVO;
import cz.tacr.elza.controller.vo.ParEventEditVO;
import cz.tacr.elza.controller.vo.ParPartyEditVO;
import cz.tacr.elza.controller.vo.ParPartyGroupEditVO;
import cz.tacr.elza.controller.vo.ParPartyNameEditVO;
import cz.tacr.elza.controller.vo.ParPartyTimeRangeEditVO;
import cz.tacr.elza.controller.vo.ParPersonEditVO;
import cz.tacr.elza.controller.vo.ParRelationEntityVO;
import cz.tacr.elza.controller.vo.ParRelationVO;
import cz.tacr.elza.controller.vo.RegRecordVO;
import cz.tacr.elza.controller.vo.RegVariantRecordVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemVO;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ParDynasty;
import cz.tacr.elza.domain.ParEvent;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyTimeRange;
import cz.tacr.elza.domain.ParPerson;
import cz.tacr.elza.domain.ParRelation;
import cz.tacr.elza.domain.ParRelationEntity;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;


/**
 * Továrna na vytváření DO objektů z VO objektů.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 07.01.2016
 */
@Service
public class ClientFactoryDO {

    @Autowired
    @Qualifier("configVOMapper")
    private MapperFactory mapperFactory;

    @Autowired
    private DescItemTypeRepository descItemTypeRepository;

    @Autowired
    private DescItemSpecRepository descItemSpecRepository;

    /**
     * Vytvoří node z VO.
     * @param nodeVO vo node
     * @return DO node
     */
    public ArrNode createNode(final ArrNodeVO nodeVO){
        Assert.notNull(nodeVO);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(nodeVO, ArrNode.class);
    }

    /**
     * Vytvoří seznam DO z VO.
     * @param nodeVoList VO seznam nodů
     * @return DO seznam nodů
     */
    public List<ArrNode> createNodes(final Collection<ArrNodeVO> nodeVoList){
        Assert.notNull(nodeVoList);

        List<ArrNode> result = new ArrayList<>(nodeVoList.size());
        for (ArrNodeVO arrNodeVO : nodeVoList) {
            result.add(createNode(arrNodeVO));
        }

        return result;
    }

    /**
     * Vytvoří objekt osoby z předaného VO.
     *
     * @param partyVO VO osoby
     * @return objekt osoby
     */
    public ParParty createParty(final ParPartyEditVO partyVO) {
        if (partyVO == null) {
            return null;
        }

        MapperFacade mapper = mapperFactory.getMapperFacade();

        if (partyVO instanceof ParDynastyEditVO) {
            return mapper.map(partyVO, ParDynasty.class);
        }
        if (partyVO instanceof ParPersonEditVO) {
            return mapper.map(partyVO, ParPerson.class);
        }
        if (partyVO instanceof ParEventEditVO) {
            return mapper.map(partyVO, ParEvent.class);
        }
        if (partyVO instanceof ParPartyGroupEditVO) {
            return mapper.map(partyVO, ParPartyGroup.class);
        }

        return mapper.map(partyVO, ParParty.class);
    }

    public void updateParty(final ParPartyEditVO partyVO, final ParParty origParty) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        mapper.map(partyVO, origParty);
    }

    /**
     * Vytvoří objekt jména osoby. Jsou načteny i detailní informace.
     *
     * @param partyNameVOSave jméno osoby VO
     * @return vo jména osoba
     */
    public ParPartyName createParPartyName(final ParPartyNameEditVO partyNameVOSave) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(partyNameVOSave, ParPartyName.class);
    }

    /**
     * Vytvoří objekt působnosti osoby.
     *
     * @param parPartyTimeRangeEditVO působnost VO
     * @return do působnost
     */
    public ParPartyTimeRange createParPartyTimeRange(final ParPartyTimeRangeEditVO parPartyTimeRangeEditVO) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(parPartyTimeRangeEditVO, ParPartyTimeRange.class);
    }

    /**
     * Vytvoření rejstříkového hesla.
     *
     * @param regRecordVO VO rejstříkové heslo
     * @return DO rejstříkové heslo
     */
    public RegRecord createRegRecord(final RegRecordVO regRecordVO) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(regRecordVO, RegRecord.class);
    }

    /**
     * Vytvoření variantního rejstříkového hesla.
     *
     * @param regVariantRecord VO variantní rejstříkové heslo
     * @return DO variantní rejstříkové heslo
     */
    public RegVariantRecord createRegVariantRecord(final RegVariantRecordVO regVariantRecord) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(regVariantRecord, RegVariantRecord.class);
    }

    /**
     * Vytvoření hodnoty atributu.
     *
     * @param descItemVO     VO hodnoty atributu
     * @param descItemTypeId identiifkátor typu hodnoty atributu
     * @return
     */
    public ArrDescItem createDescItem(final ArrDescItemVO descItemVO, final Integer descItemTypeId) {
        MapperFacade mapper = mapperFactory.getMapperFacade();

        ArrDescItem descItem = mapper.map(descItemVO, ArrDescItem.class);

        RulDescItemType descItemType = descItemTypeRepository.findOne(descItemTypeId);
        if (descItemType == null) {
            throw new IllegalStateException("Typ s ID=" + descItemVO.getDescItemSpecId() + " neexistuje");
        }
        descItem.setDescItemType(descItemType);

        if (descItemVO.getDescItemSpecId() != null) {
            RulDescItemSpec descItemSpec = descItemSpecRepository.findOne(descItemVO.getDescItemSpecId());
            if (descItemSpec == null) {
                throw new IllegalStateException("Specifikace s ID=" + descItemVO.getDescItemSpecId() + " neexistuje");
            }
            descItem.setDescItemSpec(descItemSpec);
        }

        return descItem;
    }

    /**
     * Vytvoří DO objektu vztahu.
     *
     * @param relationVO VO objekt vztahu
     * @return DO objekt vztahu
     */
    public ParRelation createRelation(final ParRelationVO relationVO) {
        MapperFacade mapper = mapperFactory.getMapperFacade();

        ParRelation relation = mapper.map(relationVO, ParRelation.class);
        return relation;
    }

    /**
     * Vytvoří seznam DO relation entities z VO objektů.
     *
     * @param relationEntities seznam VO relation entities
     * @return seznam DO
     */
    public List<ParRelationEntity> createRelationEntities(@Nullable final Collection<ParRelationEntityVO> relationEntities) {
        if (relationEntities == null) {
            return null;
        }

        MapperFacade mapper = mapperFactory.getMapperFacade();

        List<ParRelationEntity> result = new ArrayList<>(relationEntities.size());

        for (ParRelationEntityVO relationEntity : relationEntities) {
            result.add(mapper.map(relationEntity, ParRelationEntity.class));
        }

        return result;
    }

    public ArrDescItem createDescItem(final ArrDescItemVO descItemVO) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrDescItem descItem = mapper.map(descItemVO, ArrDescItem.class);

        if (descItemVO.getDescItemSpecId() != null) {
            RulDescItemSpec descItemSpec = descItemSpecRepository.findOne(descItemVO.getDescItemSpecId());
            if (descItemSpec == null) {
                throw new IllegalStateException("Specifikace s ID=" + descItemVO.getDescItemSpecId() + " neexistuje");
            }
            descItem.setDescItemSpec(descItemSpec);
        }

        return descItem;
    }
}
