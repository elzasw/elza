package cz.tacr.elza.cam.v2;

import static cz.tacr.elza.domain.converter.UnitDateConverterConsts.DEFAULT_INTERVAL_DELIMITER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.cam.v2.schema.cam.EntityXml;
import cz.tacr.cam.v2.schema.cam.FoundEntityInfoXml;
import cz.tacr.cam.v2.schema.cam.ItemStringXml;
import cz.tacr.cam.v2.schema.cam.PartXml;
import cz.tacr.cam.v2.schema.cam.ResultLookupXml;
import cz.tacr.cam.v2.schema.cam.HightlightPosXml;
import cz.tacr.cam.v2.client.controller.vo.QueryAndDef;
import cz.tacr.cam.v2.client.controller.vo.QueryBaseCondDef;
import cz.tacr.cam.v2.client.controller.vo.QueryBaseCondDef.CondTypeEnum;
import cz.tacr.cam.v2.client.controller.vo.QueryComparator;
import cz.tacr.cam.v2.client.controller.vo.QueryIndexCondDef;
import cz.tacr.cam.v2.client.controller.vo.QueryPartCondDef;
import cz.tacr.cam.v2.client.controller.vo.QueryPartType;
import cz.tacr.cam.v2.client.controller.vo.QueryValueCondDef;
import cz.tacr.cam.v2.client.controller.vo.QueryParamsDef;
import cz.tacr.cam.v2.schema.cam.QueryResultXml;
import cz.tacr.elza.controller.vo.ArchiveEntityResultListVO;
import cz.tacr.elza.controller.vo.ArchiveEntityVO;
import cz.tacr.elza.controller.vo.ApSearchArea;
import cz.tacr.elza.controller.vo.ApSearchByItemWithValue;
import cz.tacr.elza.controller.vo.HighlightVO;
import cz.tacr.elza.controller.vo.ApSearchByRelation;
import cz.tacr.elza.controller.vo.ResultLookupVO;
import cz.tacr.elza.controller.vo.ApAdvanceSearchFilter;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.converter.UnitDateConverter;

@Service("searchFilterFactoryV2")
public class SearchFilterFactory {

    private static final String NM_MAIN = "NM_MAIN";
    private static final String CRE_DATE = "CRE_DATE";
    private static final String EXT_DATE = "EXT_DATE";
    private static final String DISPLAY_NAME = "DISPLAY_NAME";

    @Autowired
    private StaticDataService staticDataService;

    public QueryParamsDef createQueryParamsDef(ApAdvanceSearchFilter filter) {
        QueryParamsDef queryParamsDef = new QueryParamsDef();
        queryParamsDef.setTypes(createTypeList(filter.getAeTypeIds()));
        queryParamsDef.setCond(createQueryBaseCondDef(filter));
        queryParamsDef.setUserName(filter.getUser());
        //queryParamsDef.setUserRoles();
        //queryParamsDef.setUserUuid();
        return queryParamsDef;
	}

	public ArchiveEntityResultListVO createArchiveEntityVoListResult(QueryResultXml queryResult) {
        ArchiveEntityResultListVO archiveEntityVOListResult = new ArchiveEntityResultListVO();
        archiveEntityVOListResult.setTotal(queryResult.getHeader().getCount().getValue().intValue());
        archiveEntityVOListResult.setData(createArchiveEntityVoList(queryResult.getList().getEntityInfo()));
        return archiveEntityVOListResult;
	}

    /**
     * Empty result, used when an entity requested by its identifier does not exist.
     */
    public ArchiveEntityResultListVO createEmptyResult() {
        ArchiveEntityResultListVO result = new ArchiveEntityResultListVO();
        result.setTotal(0);
        result.setData(new ArrayList<>());
        return result;
    }

    /**
     * Wrap a single entity fetched by its identifier as a one-item search result.
     */
    public ArchiveEntityResultListVO createSingleEntityResult(EntityXml entity) {
        ArchiveEntityResultListVO result = new ArchiveEntityResultListVO();
        List<ArchiveEntityVO> data = new ArrayList<>();
        data.add(createArchiveEntityVO(entity));
        result.setData(data);
        result.setTotal(1);
        return result;
    }

    private ArchiveEntityVO createArchiveEntityVO(EntityXml entity) {
        StaticDataProvider sdp = staticDataService.getData();
        ArchiveEntityVO archiveEntityVO = new ArchiveEntityVO();
        archiveEntityVO.setId((int) entity.getEntityId().getValue());
        archiveEntityVO.setAeTypeId(sdp.getApTypeByCode(entity.getEntityType().getValue()).getApTypeId());
        archiveEntityVO.setName(findDisplayName(entity));
        return archiveEntityVO;
    }

    /**
     * Display name of an entity: the DISPLAY_NAME index of the first PT_NAME part.
     * Indexes are carried in the part's {@code eits} collection as string items
     * whose type holds the index name.
     */
    private String findDisplayName(EntityXml entity) {
        if (entity.getParts() == null) {
            return null;
        }
        for (PartXml part : entity.getParts().getPart()) {
            if (!StaticDataProvider.DEFAULT_PART_TYPE.equals(part.getType().value()) || part.getEits() == null) {
                continue;
            }
            for (Object index : part.getEits().getItems()) {
                if (index instanceof ItemStringXml) {
                    ItemStringXml stringIndex = (ItemStringXml) index;
                    if (DISPLAY_NAME.equals(stringIndex.getType().getValue()) && stringIndex.getValue() != null) {
                        return stringIndex.getValue().getValue();
                    }
                }
            }
        }
        return null;
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
        archiveEntityVO.setDescription(foundEntityInfo.getDescription() == null ? null: foundEntityInfo.getDescription().getValue());
        archiveEntityVO.setAeTypeId(sdp.getApTypeByCode(foundEntityInfo.getEt().getValue()).getApTypeId());
        archiveEntityVO.setResultLookups(createResultLookupVO(foundEntityInfo.getLookup()));
        archiveEntityVO.setId((int) foundEntityInfo.getEntityId().getValue());
        return archiveEntityVO;
    }

    private ResultLookupVO createResultLookupVO(ResultLookupXml resultLookup) {
        ResultLookupVO resultLookupVO = new ResultLookupVO();
        resultLookupVO.setValue(resultLookup.getValue().getValue());
        resultLookupVO.setHighlights(createHighlightList(resultLookup.getHighlightPos()));
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

    /**
     * Vytvoření podmínky z parametrů filtru
     *
     * @param filter GlobalSearchFilterVO
     * @return podmínka QueryBaseCondDef
     */
    private QueryBaseCondDef createQueryBaseCondDef(ApAdvanceSearchFilter filter) {
        QueryBaseCondDef queryBaseCondDef = null;
        String search = filter.getSearch();
        ApSearchArea area = filter.getArea();
        if (area != ApSearchArea.ENTITY_CODE) {
            List<QueryBaseCondDef> andCondDefList = new ArrayList<>();

            if (StringUtils.isNotEmpty(search)) {
                List<String> keyWords = getKeyWordsFromSearch(search);
                for (String keyWord : keyWords) {
                    QueryBaseCondDef cond;
                    if (filter.getOnlyMainPart() && !area.equals(ApSearchArea.ALL_PARTS)) {
                        cond = createQueryValueCondDef(NM_MAIN, null, QueryComparator.CT_CONTAINS, keyWord);
                    } else {
                        cond = createQueryIndexCondDef(keyWord);
                    }
                    switch (area) {
                        case PREFER_NAMES: {
                            andCondDefList.add(createQueryPartCondDef(cond, QueryPartType.PT_PREF_NAME));
                            break;
                        }
                        case ALL_PARTS: {
                            andCondDefList.add(cond);
                            break;
                        }
                        case ALL_NAMES: {
                            andCondDefList.add(createQueryPartCondDef(cond, QueryPartType.PT_NAME));
                            break;
                        }
                        default:
                            throw new NotImplementedException("Neimplementovaný stav oblasti: " + area);
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(filter.getExtFilters())) {
                for (ApSearchByItemWithValue ext : filter.getExtFilters()) {
                    QueryValueCondDef valueCondDef = createQueryValueCondDef(ext.getItemTypeId(), ext.getItemSpecId(), QueryComparator.CT_CONTAINS, String.valueOf(ext.getValue()));
                    andCondDefList.add(createQueryPartCondDef(valueCondDef, QueryPartType.fromValue(ext.getPartTypeCode())));
                }
            }
            if (CollectionUtils.isNotEmpty(filter.getRelFilters())) {
                for (ApSearchByRelation rel : filter.getRelFilters()) {
                    if (rel.getCode() != null) {
                        andCondDefList.add(createQueryValueCondDef(rel.getRelTypeId(), rel.getRelSpecId(), QueryComparator.CT_EQ, String.valueOf(rel.getCode())));
                    }
                }
            }
            if (StringUtils.isNotEmpty(filter.getCreation())) {
                ArrDataUnitdate aeDataUnitdate = UnitDateConverter.convertToUnitDate(filter.getCreation(), new ArrDataUnitdate());
                String intervalCreation = aeDataUnitdate.getValueFrom() + DEFAULT_INTERVAL_DELIMITER + aeDataUnitdate.getValueTo();
                QueryValueCondDef valueCondDef = createQueryValueCondDef(CRE_DATE, null, QueryComparator.CT_CONTAINS, intervalCreation);
                andCondDefList.add(createQueryPartCondDef(valueCondDef, QueryPartType.PT_CRE));
            }
            if (StringUtils.isNotEmpty(filter.getExtinction())) {
                ArrDataUnitdate aeDataUnitdate = UnitDateConverter.convertToUnitDate(filter.getExtinction(), new ArrDataUnitdate());
                String intervalExtinction = aeDataUnitdate.getValueFrom() + DEFAULT_INTERVAL_DELIMITER + aeDataUnitdate.getValueTo();
                QueryValueCondDef valueCondDef = createQueryValueCondDef(EXT_DATE, null, QueryComparator.CT_CONTAINS, intervalExtinction);
                andCondDefList.add(createQueryPartCondDef(valueCondDef, QueryPartType.PT_EXT));
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
        queryAndDef.setCondType(CondTypeEnum.AND);
        queryAndDef.setConds(andCondDefList);
        return queryAndDef;
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
        queryValueCondDef.setCondType(CondTypeEnum.VALUE);
        return queryValueCondDef;
    }

    private QueryIndexCondDef createQueryIndexCondDef(String value) {
        QueryIndexCondDef queryIndexCondDef = new QueryIndexCondDef();
        queryIndexCondDef.setName(DISPLAY_NAME);
        queryIndexCondDef.setComparator(QueryComparator.CT_CONTAINS);
        queryIndexCondDef.setCondType(CondTypeEnum.INDEX);
        queryIndexCondDef.setValue(value);
        return queryIndexCondDef;
    }

    private QueryPartCondDef createQueryPartCondDef(QueryBaseCondDef queryBaseCondDef, QueryPartType partTypeEnum) {
        QueryPartCondDef queryPartCondDef = new QueryPartCondDef();
        queryPartCondDef.setPartType(partTypeEnum);
        queryPartCondDef.setCondType(CondTypeEnum.PART);
        queryPartCondDef.setCond(queryBaseCondDef);
        return queryPartCondDef;
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
}
