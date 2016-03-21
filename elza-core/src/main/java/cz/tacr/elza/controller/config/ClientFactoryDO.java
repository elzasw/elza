package cz.tacr.elza.controller.config;

import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemVO;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.repository.*;
import cz.tacr.elza.xmlimport.v1.utils.XmlImportConfig;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


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

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private PacketTypeRepository packetTypeRepository;

    @Autowired
    private RegRecordRepository regRecordRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

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
    public ParParty createParty(final ParPartyVO partyVO) {
        if (partyVO == null) {
            return null;
        }

        MapperFacade mapper = mapperFactory.getMapperFacade();
        ParParty party = mapper.map(partyVO, ParParty.class);

        if (CollectionUtils.isNotEmpty(partyVO.getPartyNames())) {
            List<ParPartyName> partyNames = new ArrayList<>(partyVO.getPartyNames().size());
            for (ParPartyNameVO partyName : partyVO.getPartyNames()) {
                ParPartyName partyNameDo = mapper.map(partyName, ParPartyName.class);
                if (partyName.isPrefferedName()) {
                    party.setPreferredName(partyNameDo);
                }
                partyNames.add(partyNameDo);
            }
            party.setPartyNames(partyNames);
        }

        return party;
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

    public ArrPacket createPacket(final ArrPacketVO packetVO, final Integer fundId) {
        Assert.notNull(fundId);
        Assert.notNull(packetVO);

        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrPacket packet = mapper.map(packetVO, ArrPacket.class);

        ArrFund fund = fundRepository.findOne(fundId);
        Assert.notNull(fund, "Archivní pomůcka neexistuje (ID=" + fundId + ")");

        if (packetVO.getPacketTypeId() != null) {
            RulPacketType packetType = packetTypeRepository.findOne(packetVO.getPacketTypeId());
            Assert.notNull(packetType, "Typ obalu neexistuje (ID=" + packetVO.getPacketTypeId() + ")");
            packet.setPacketType(packetType);
        }

        packet.setFund(fund);

        return packet;
    }

    /**
     * Vytvoří třídu rejstříku.
     *
     * @param scopeVO třída rejstříku
     * @return třída rejstříku
     */
    public RegScope createScope(final RegScopeVO scopeVO) {
        Assert.notNull(scopeVO);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(scopeVO, RegScope.class);
    }


    /**
     * Vytvoří seznam rejstříků
     *
     * @param scopeVOs seznam VO rejstříků
     * @return seznam DO
     */
    public List<RegScope> createScopeList(@Nullable final Collection<RegScopeVO> scopeVOs) {
        if (scopeVOs == null) {
            return new ArrayList<>();
        }

        List<RegScope> result = new ArrayList<>(scopeVOs.size());

        for (RegScopeVO scopeVO : scopeVOs) {
            result.add(createScope(scopeVO));
        }

        return result;
    }

    /**
     * Vytvoří DO archivní pomůcky
     *
     * @param fundVO VO archivní pomůcka
     * @return DO
     */
    public ArrFund createFund(ArrFundVO fundVO) {
        Assert.notNull(fundVO);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrFund fund = mapper.map(fundVO, ArrFund.class);
        ParInstitution institution = institutionRepository.findOne(fundVO.getInstitutionId());
        fund.setInstitution(institution);
        return fund;
    }

    public ArrNodeRegister createRegisterLink(final ArrNodeRegisterVO nodeRegisterVO) {
        Assert.notNull(nodeRegisterVO);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrNodeRegister nodeRegister = mapper.map(nodeRegisterVO, ArrNodeRegister.class);

        if (nodeRegisterVO.getValue() != null) {
            nodeRegister.setRecord(regRecordRepository.findOne(nodeRegisterVO.getValue()));
        }

        return nodeRegister;
    }

    public XmlImportConfig createXmlImportConfig(final XmlImportConfigVO configVO) {
        Assert.notNull(configVO);

        MapperFacade mapper = mapperFactory.getMapperFacade();

        return mapper.map(configVO, XmlImportConfig.class);
    }
}
