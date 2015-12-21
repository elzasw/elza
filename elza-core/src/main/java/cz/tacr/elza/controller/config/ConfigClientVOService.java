package cz.tacr.elza.controller.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import cz.tacr.elza.controller.vo.RegRecordVO;
import cz.tacr.elza.controller.vo.RegRegisterTypeVO;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;


/**
 * Tovární třída pro vytváření VO objektů a jejich seznamů.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
@Service
public class ConfigClientVOService {

    @Autowired
    @Qualifier("configVOMapper")
    private MapperFactory mapperFactory;

    /**
     * Vytvoření seznamu RegRecordVo.
     *
     * @param records        seznam rejstříkových hesel
     * @param recordPartyMap mapa id rejstříkových hesel na osobu
     * @return seznam rejstříkových hesel
     */
    public List<RegRecordVO> createRegRecords(final List<RegRecord> records,
                                              final Map<Integer, ParParty> recordPartyMap) {
        List<RegRecordVO> result = new ArrayList<>(records.size());
        for (RegRecord record : records) {
            ParParty recordParty = recordPartyMap.get(record.getRecordId());
            result.add(createRegRecord(record, recordParty == null ? null : recordParty.getPartyId()));
        }

        return result;
    }

    /**
     * Vytvoří rejstříkové heslo.
     *
     * @param regRecord rejstříkové heslo
     * @param partyId   id osoby
     * @return rejstříkové heslo
     */
    public RegRecordVO createRegRecord(final RegRecord regRecord, @Nullable final Integer partyId) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        RegRecordVO result = mapper.map(regRecord, RegRecordVO.class);
        result.setPartyId(partyId);

        return result;
    }


    /**
     * Vytváří stromovou strukturu pro všechny typy rejstříků.
     *
     * @param allTypes všechny typy rejstříků
     * @return seznam kořenových typů rejstříků
     */
    public List<RegRegisterTypeVO> createRegisterTypesTree(final List<RegRegisterType> allTypes) {
        if (CollectionUtils.isEmpty(allTypes)) {
            return Collections.EMPTY_LIST;
        }

        Map<Integer, RegRegisterTypeVO> typeMap = new HashMap<>();
        List<RegRegisterTypeVO> roots = new LinkedList<>();
        for (RegRegisterType registerType : allTypes) {
            RegRegisterTypeVO registerTypeVO = createRegisterTypeTree(registerType, typeMap);
            if (registerTypeVO.getParentRegisterTypeId() == null) {
                roots.add(registerTypeVO);
            }
        }

        return roots;
    }

    /**
     * Vytvoří typ rejstříkového hesla a vloží jeje do mapy všech hesel.
     *
     * @param registerType typ hesla
     * @param typeMap      mapa všech typů (id typu ->typ)
     * @return typ rejstříkového hesla
     */
    private RegRegisterTypeVO createRegisterTypeTree(final RegRegisterType registerType,
                                                     final Map<Integer, RegRegisterTypeVO> typeMap) {
        MapperFacade mapper = mapperFactory.getMapperFacade();

        RegRegisterTypeVO result = typeMap.get(registerType.getRegisterTypeId());
        if (result != null) {
            return result;
        }

        result = mapper.map(registerType, RegRegisterTypeVO.class);
        typeMap.put(result.getRegisterTypeId(), result);
        if (registerType.getParentRegisterType() != null) {
            RegRegisterTypeVO parent = createRegisterTypeTree(registerType.getParentRegisterType(), typeMap);
            parent.addChild(result);
        }

        return result;
    }

}
