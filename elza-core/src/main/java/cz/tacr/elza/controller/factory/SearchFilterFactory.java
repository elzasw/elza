package cz.tacr.elza.controller.factory;

import cz.tacr.cam._2019.*;
import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SearchFilterFactory {

    @Autowired
    private StaticDataService staticDataService;

    public ArchiveEntityResultListVO createArchiveEntityVoListResult(QueryResult queryResult) {
        ArchiveEntityResultListVO archiveEntityVOListResult = new ArchiveEntityResultListVO();
        archiveEntityVOListResult.setTotal(queryResult.getRi().getCnt().intValue());
        archiveEntityVOListResult.setData(createArchiveEntityVoList(queryResult.getList().getFei()));
        return archiveEntityVOListResult;
    }

    private List<ArchiveEntityVO> createArchiveEntityVoList(List<FoundEntityInfo> foundEntityInfoList) {
        List<ArchiveEntityVO> archiveEntityVOList = new ArrayList<>();
        for (FoundEntityInfo foundEntityInfo : foundEntityInfoList) {
            archiveEntityVOList.add(createArchiveEntityVO(foundEntityInfo));
        }
        return archiveEntityVOList;
    }

    private ArchiveEntityVO createArchiveEntityVO(FoundEntityInfo foundEntityInfo) {
        StaticDataProvider sdp = staticDataService.getData();
        ArchiveEntityVO archiveEntityVO = new ArchiveEntityVO();
        archiveEntityVO.setName(foundEntityInfo.getName());
        archiveEntityVO.setDescription(foundEntityInfo.getDsc());
        archiveEntityVO.setAeTypeId(sdp.getApTypeByCode(foundEntityInfo.getEt()).getApTypeId());
        archiveEntityVO.setResultLookups(createResultLookupVO(foundEntityInfo.getRl()));
        archiveEntityVO.setId(foundEntityInfo.getEid().intValue());
        return archiveEntityVO;
    }

    private ResultLookupVO createResultLookupVO(ResultLookup resultLookup) {
        ResultLookupVO resultLookupVO = new ResultLookupVO();
        resultLookupVO.setValue(resultLookup.getV());
        resultLookupVO.setHighlights(createHighlightList(resultLookup.getHp()));
        resultLookupVO.setPartTypeCode((resultLookup.getPt().value()));
        return resultLookupVO;
    }

    private List<HighlightVO> createHighlightList(List<HightlightPos> highlightPosList) {
        List<HighlightVO> highlightList = new ArrayList<>();
        for (HightlightPos highlightPos : highlightPosList) {
            highlightList.add(createHighlight(highlightPos));
        }
        return highlightList;
    }

    private HighlightVO createHighlight(HightlightPos highlightPos) {
        HighlightVO highlight = new HighlightVO();
        highlight.setFrom(highlightPos.getSpos().longValue());
        highlight.setTo(highlightPos.getEpos().longValue());
        return highlight;
    }

}
