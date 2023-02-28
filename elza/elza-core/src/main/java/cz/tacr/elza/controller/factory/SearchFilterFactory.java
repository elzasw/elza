package cz.tacr.elza.controller.factory;

import static cz.tacr.elza.domain.convertor.UnitDateConvertorConsts.DEFAULT_INTERVAL_DELIMITER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.cam.client.controller.vo.QueryAndDef;
import cz.tacr.cam.client.controller.vo.QueryBaseCondDef;
import cz.tacr.cam.client.controller.vo.QueryComparator;
import cz.tacr.cam.client.controller.vo.QueryIndexCondDef;
import cz.tacr.cam.client.controller.vo.QueryParamsDef;
import cz.tacr.cam.client.controller.vo.QueryPartCondDef;
import cz.tacr.cam.client.controller.vo.QueryValueCondDef;
import cz.tacr.cam.schema.cam.FoundEntityInfoXml;
import cz.tacr.cam.schema.cam.HightlightPosXml;
import cz.tacr.cam.schema.cam.QueryResultXml;
import cz.tacr.cam.schema.cam.ResultLookupXml;
import cz.tacr.elza.controller.vo.AeState;
import cz.tacr.elza.controller.vo.ArchiveEntityResultListVO;
import cz.tacr.elza.controller.vo.ArchiveEntityVO;
import cz.tacr.elza.controller.vo.Area;
import cz.tacr.elza.controller.vo.ExtensionFilterVO;
import cz.tacr.elza.controller.vo.HighlightVO;
import cz.tacr.elza.controller.vo.RelationFilterVO;
import cz.tacr.elza.controller.vo.ResultLookupVO;
import cz.tacr.elza.controller.vo.SearchFilterVO;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;

@Service
public class SearchFilterFactory {

    private static final String NM_MAIN = "NM_MAIN";
    private static final String CRE_DATE = "CRE_DATE";
    private static final String EXT_DATE = "EXT_DATE";
    private static final String DISPLAY_NAME = "DISPLAY_NAME";

    @Autowired
    private StaticDataService staticDataService;

    public QueryParamsDef createQueryParamsDef(SearchFilterVO filter) {
        QueryParamsDef queryParamsDef = new QueryParamsDef();
        queryParamsDef.setTypes(createTypeList(filter.getAeTypeIds()));
        queryParamsDef.setState(createStateList(filter.getAeStates()));
        queryParamsDef.setCond(createQueryBaseCondDef(filter));
        queryParamsDef.setCodes(createCodeList(filter));
        queryParamsDef.setUser(filter.getUser());
        return queryParamsDef;
    }

    private List<String> createCodeList(SearchFilterVO filter) {
        List<String> codes = null;
        String code = filter.getArea() == Area.ENTITY_CODE ? filter.getSearch() : filter.getCode();
        if (StringUtils.isNotEmpty(code)) {
            codes = Collections.singletonList(code);
        }
        return codes;
    }

    /**
     * Získání listu jmen AeType ze setu ID
     *
     * @param aeTypeIds Set ID AeType
     * @return List jmen AeType
     */
    private List<String> createTypeList(Collection<Integer> aeTypeIds) {
        StaticDataProvider sdp = staticDataService.getData();
        List<ApType> apTypeList = sdp.getApTypes();
        List<String> types = null;
        if (aeTypeIds != null) {
            Set<String> typesSet = new HashSet<>();
            for (Integer aeTypeId : aeTypeIds) {
                List<ApType> rulAeTypes = findTreeAeTypes(apTypeList, aeTypeId);
                for (ApType rulAeType : rulAeTypes) {
                    typesSet.add(rulAeType.getCode());
                }
            }
            types = new ArrayList<>(typesSet);
        }
        return types;
    }

    public void completeApTypesTreeInFilter(final SearchFilterVO filter) {
        StaticDataProvider sdp = staticDataService.getData();
        List<ApType> apTypeList = sdp.getApTypes();
        if (filter.getAeTypeIds() != null) {
            Set<Integer> typesSet = new HashSet<>();
            for (Integer aeTypeId : filter.getAeTypeIds()) {
                List<ApType> apTypes = findTreeAeTypes(apTypeList, aeTypeId);
                for (ApType apType : apTypes) {
                    typesSet.add(apType.getApTypeId());
                }
            }
            filter.setAeTypeIds(new ArrayList<>(typesSet));
        }
    }

    private List<ApType> findTreeAeTypes(final List<ApType> apTypes, final Integer id) {
        ApType parent = getById(apTypes, id);
        Set<ApType> result = new HashSet<>();
        if (parent != null) {
            result.add(parent);
            for (ApType item : apTypes) {
                if (parent.equals(item.getParentApType())) {
                    result.addAll(findTreeAeTypes(apTypes, item.getApTypeId()));
                }
            }
        }
        return new ArrayList<>(result);
    }

    private ApType getById(final List<ApType> apTypes, final Integer id) {
        for (ApType apType : apTypes) {
            if (apType.getApTypeId().equals(id)) {
                return apType;
            }
        }
        return null;
    }

    /**
     * Převod setu AeStateEnum na list QueryParamsDef.StateEnum
     *
     * @param aeStates Set AeStateEnum
     * @return List QueryParamsDef.StateEnum
     */
    private List<QueryParamsDef.StateEnum> createStateList(Collection<AeState> aeStates) {
        List<QueryParamsDef.StateEnum> stateEnumList = null;
        if (aeStates != null) {
            stateEnumList = new ArrayList<>();
            for (AeState aeStateEnum : aeStates) {
                switch (aeStateEnum) {
                    case APS_NEW:
                        stateEnumList.add(QueryParamsDef.StateEnum.NEW);
                        break;
                    case APS_APPROVED:
                        stateEnumList.add(QueryParamsDef.StateEnum.APPROVED);
                        break;
                    case APS_INVALID:
                        stateEnumList.add(QueryParamsDef.StateEnum.INVALID);
                        break;
                    case APS_REPLACED:
                        stateEnumList.add(QueryParamsDef.StateEnum.REPLACED);
                        break;
                }
            }
        }
        return stateEnumList;
    }

    /**
     * Vytvoření podmínky z parametrů filtru
     *
     * @param filter GlobalSearchFilterVO
     * @return podmínka QueryBaseCondDef
     */
    private QueryBaseCondDef createQueryBaseCondDef(SearchFilterVO filter) {
        QueryBaseCondDef queryBaseCondDef = null;
        String search = filter.getSearch();
        Area area = filter.getArea();
        if (area != Area.ENTITY_CODE) {
            List<QueryBaseCondDef> andCondDefList = new ArrayList<>();

            if (StringUtils.isNotEmpty(search)) {
                List<String> keyWords = getKeyWordsFromSearch(search);
                for (String keyWord : keyWords) {
                    QueryBaseCondDef cond;
                    if (filter.getOnlyMainPart() && !area.equals(Area.ALL_PARTS)) {
                        cond = createQueryValueCondDef(NM_MAIN, null, QueryComparator.CONTAIN, keyWord);
                    } else {
                        cond = createQueryIndexCondDef(keyWord);
                    }
                    switch (area) {
                        case PREFER_NAMES: {
                            andCondDefList.add(createQueryPartCondDef(cond, QueryPartCondDef.PartTypeEnum.PREF_NAME));
                            break;
                        }
                        case ALL_PARTS: {
                            andCondDefList.add(cond);
                            break;
                        }
                        case ALL_NAMES: {
                            andCondDefList.add(createQueryPartCondDef(cond, QueryPartCondDef.PartTypeEnum.NAME));
                            break;
                        }
                        default:
                            throw new NotImplementedException("Neimplementovaný stav oblasti: " + area);
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(filter.getExtFilters())) {
                for (ExtensionFilterVO ext : filter.getExtFilters()) {
                    QueryValueCondDef valueCondDef = createQueryValueCondDef(ext.getItemTypeId(), ext.getItemSpecId(), QueryComparator.CONTAIN, String.valueOf(ext.getValue()));
                    andCondDefList.add(createQueryPartCondDef(valueCondDef, QueryPartCondDef.PartTypeEnum.fromValue(ext.getPartTypeCode())));
                }
            }
            if (CollectionUtils.isNotEmpty(filter.getRelFilters())) {
                for (RelationFilterVO rel : filter.getRelFilters()) {
                    if (rel.getCode() != null) {
                        andCondDefList.add(createQueryValueCondDef(rel.getRelTypeId(), rel.getRelSpecId(), QueryComparator.EQ, String.valueOf(rel.getCode())));
                    }
                }
            }
            if (StringUtils.isNotEmpty(filter.getCreation())) {
                ArrDataUnitdate aeDataUnitdate = UnitDateConvertor.convertToUnitDate(filter.getCreation(), new ArrDataUnitdate());
                String intervalCreation = aeDataUnitdate.getValueFrom() + DEFAULT_INTERVAL_DELIMITER + aeDataUnitdate.getValueTo();
                QueryValueCondDef valueCondDef = createQueryValueCondDef(CRE_DATE, null, QueryComparator.CONTAIN, intervalCreation);
                andCondDefList.add(createQueryPartCondDef(valueCondDef, QueryPartCondDef.PartTypeEnum.CRE));
            }
            if (StringUtils.isNotEmpty(filter.getExtinction())) {
                ArrDataUnitdate aeDataUnitdate = UnitDateConvertor.convertToUnitDate(filter.getExtinction(), new ArrDataUnitdate());
                String intervalExtinction = aeDataUnitdate.getValueFrom() + DEFAULT_INTERVAL_DELIMITER + aeDataUnitdate.getValueTo();
                QueryValueCondDef valueCondDef = createQueryValueCondDef(EXT_DATE, null, QueryComparator.CONTAIN, intervalExtinction);
                andCondDefList.add(createQueryPartCondDef(valueCondDef, QueryPartCondDef.PartTypeEnum.EXT));
            }
            if (andCondDefList.size() == 1) {
                queryBaseCondDef = andCondDefList.get(0);
            } else if (andCondDefList.size() > 1){
                queryBaseCondDef = createQueryAndDef(andCondDefList);
            }
        }

        return queryBaseCondDef;
    }

    private QueryAndDef createQueryAndDef(List<QueryBaseCondDef> andCondDefList) {
        QueryAndDef queryAndDef = new QueryAndDef();
        queryAndDef.setCondType("and");
        queryAndDef.setConds(andCondDefList);
        return queryAndDef;
    }

    private QueryPartCondDef createQueryPartCondDef(QueryBaseCondDef queryBaseCondDef, QueryPartCondDef.PartTypeEnum partTypeEnum) {
        QueryPartCondDef queryPartCondDef = new QueryPartCondDef();
        queryPartCondDef.setPartType(partTypeEnum);
        queryPartCondDef.setCondType("part");
        queryPartCondDef.setCond(queryBaseCondDef);
        return queryPartCondDef;
    }

    private QueryIndexCondDef createQueryIndexCondDef(String value) {
        QueryIndexCondDef queryIndexCondDef = new QueryIndexCondDef();
        queryIndexCondDef.setName(DISPLAY_NAME);
        queryIndexCondDef.setComparator(QueryComparator.CONTAIN);
        queryIndexCondDef.setCondType("index");
        queryIndexCondDef.setValue(value);
        return queryIndexCondDef;
    }

    private QueryValueCondDef createQueryValueCondDef(String itemTypeCode, String itemSpecCode, QueryComparator queryComparator, String value) {
        List<String> itemTypes = null;
        List<String> itemSpecs = null;

        if (itemTypeCode != null) {
            itemTypes = new ArrayList<>();
            itemTypes.add(itemTypeCode);
        }

        if (itemSpecCode != null) {
            itemSpecs = new ArrayList<>();
            itemSpecs.add(itemSpecCode);
        }

        return createQueryValueCondDef(itemTypes, itemSpecs, queryComparator, value);
    }

    private QueryValueCondDef createQueryValueCondDef(Integer itemTypeId, Integer itemSpecId, QueryComparator queryComparator, String value) {
        StaticDataProvider sdp = staticDataService.getData();
        List<String> itemTypes = null;
        List<String> itemSpecs = null;

        if (itemTypeId != null) {
            ItemType itemType = sdp.getItemTypeById(itemTypeId);
            itemTypes = new ArrayList<>();
            itemTypes.add(itemType.getCode());
        }

        if (itemSpecId != null) {
            RulItemSpec itemSpec = sdp.getItemSpecById(itemSpecId);
            itemSpecs = new ArrayList<>();
            itemSpecs.add(itemSpec.getCode());
        }

        return createQueryValueCondDef(itemTypes, itemSpecs, queryComparator, value);
    }

    private QueryValueCondDef createQueryValueCondDef(List<String> itemTypes, List<String> itemSpecs, QueryComparator queryComparator, String value) {
        QueryValueCondDef queryValueCondDef = new QueryValueCondDef();
        queryValueCondDef.setItemTypes(itemTypes);
        queryValueCondDef.setItemSpecs(itemSpecs);
        queryValueCondDef.setComparator(queryComparator);
        queryValueCondDef.setValue(value);
        queryValueCondDef.setCondType("value");
        return queryValueCondDef;
    }

    private List<String> getKeyWordsFromSearch(String search) {
        List<String> keyWords = new ArrayList<>();
        Pattern pattern = Pattern.compile("[^\\s,;\"]+|\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(search);
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                keyWords.add(matcher.group(1));
            } else {
                keyWords.add(matcher.group());
            }
        }
        return keyWords;
    }

    public ArchiveEntityResultListVO createArchiveEntityVoListResult(QueryResultXml queryResult) {
        ArchiveEntityResultListVO archiveEntityVOListResult = new ArchiveEntityResultListVO();
        archiveEntityVOListResult.setTotal(queryResult.getRi().getCnt().getValue().intValue());
        archiveEntityVOListResult.setData(createArchiveEntityVoList(queryResult.getList().getList()));
        return archiveEntityVOListResult;
    }

    private List<ArchiveEntityVO> createArchiveEntityVoList(List<FoundEntityInfoXml> foundEntityInfoList) {
        List<ArchiveEntityVO> archiveEntityVOList = new ArrayList<>();
        for (FoundEntityInfoXml foundEntityInfo : foundEntityInfoList) {
            archiveEntityVOList.add(createArchiveEntityVO(foundEntityInfo));
        }
        return archiveEntityVOList;
    }

    private ArchiveEntityVO createArchiveEntityVO(FoundEntityInfoXml foundEntityInfo) {
        StaticDataProvider sdp = staticDataService.getData();
        ArchiveEntityVO archiveEntityVO = new ArchiveEntityVO();
        archiveEntityVO.setName(foundEntityInfo.getName().getValue());
        archiveEntityVO.setDescription(foundEntityInfo.getDsc() == null ? null: foundEntityInfo.getDsc().getValue());
        archiveEntityVO.setAeTypeId(sdp.getApTypeByCode(foundEntityInfo.getEt().getValue()).getApTypeId());
        archiveEntityVO.setResultLookups(createResultLookupVO(foundEntityInfo.getRl()));
        archiveEntityVO.setId((int) foundEntityInfo.getEid().getValue());
        return archiveEntityVO;
    }

    private ResultLookupVO createResultLookupVO(ResultLookupXml resultLookup) {
        ResultLookupVO resultLookupVO = new ResultLookupVO();
        resultLookupVO.setValue(resultLookup.getV().getValue());
        resultLookupVO.setHighlights(createHighlightList(resultLookup.getHightlights()));
        resultLookupVO.setPartTypeCode(resultLookup.getPt() != null ? resultLookup.getPt().value() : null);
        return resultLookupVO;
    }

    private List<HighlightVO> createHighlightList(List<HightlightPosXml> highlightPosList) {
        List<HighlightVO> highlightList = new ArrayList<>();
        for (HightlightPosXml highlightPos : highlightPosList) {
            highlightList.add(createHighlight(highlightPos));
        }
        return highlightList;
    }

    private HighlightVO createHighlight(HightlightPosXml highlightPos) {
        HighlightVO highlight = new HighlightVO();
        highlight.setFrom(highlightPos.getSpos());
        highlight.setTo(highlightPos.getEpos());
        return highlight;
    }

    public ArchiveEntityResultListVO createArchiveEntityResultListVO(List<ApState> apList, Integer totalElements,
                                                                     Map<Integer, ApIndex> nameMap) {
        List<ArchiveEntityVO> dataList;
        if (CollectionUtils.isNotEmpty(apList)) {
            dataList = new ArrayList<>(apList.size());

            for (ApState aps : apList) {
                ApIndex nameIndex = nameMap.getOrDefault(aps.getAccessPointId(), null);
                dataList.add(createArchiveEntityVO(aps, nameIndex));
            }
        } else {
            dataList = Collections.emptyList();
        }

        ArchiveEntityResultListVO archiveEntityVOListResult = new ArchiveEntityResultListVO();
        archiveEntityVOListResult.setTotal(totalElements);
        archiveEntityVOListResult.setData(dataList);
        return archiveEntityVOListResult;
    }

    private ArchiveEntityVO createArchiveEntityVO(ApState apState, ApIndex nameIndex) {
        ArchiveEntityVO archiveEntityVO = new ArchiveEntityVO();
        archiveEntityVO.setId(apState.getAccessPointId());
        archiveEntityVO.setName(nameIndex != null ? nameIndex.getValue() : null);
        archiveEntityVO.setAeTypeId(apState.getApTypeId());
        return archiveEntityVO;
    }

}
