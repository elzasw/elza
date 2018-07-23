package cz.tacr.elza.controller.factory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import cz.tacr.elza.controller.vo.ap.ApFormVO;
import cz.tacr.elza.controller.vo.ap.ApFragmentTypeVO;
import cz.tacr.elza.controller.vo.ap.ApFragmentVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemVO;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.repository.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.controller.vo.ApAccessPointNameVO;
import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.ApExternalIdVO;
import cz.tacr.elza.controller.vo.ApRecordSimple;
import cz.tacr.elza.controller.vo.ApTypeVO;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;

@Service
public class ApFactory {

    private final ApAccessPointRepository apRepository;

    private final ApNameRepository nameRepository;

    private final ApDescriptionRepository descRepository;

    private final ApExternalIdRepository eidRepository;

    private final ScopeRepository scopeRepository;

    private final StaticDataService staticDataService;

    private final ApFragmentItemRepository fragmentItemRepository;

    @Autowired
    public ApFactory(ApAccessPointRepository apRepository,
                     ApNameRepository nameRepository,
                     ApDescriptionRepository descRepository,
                     ApExternalIdRepository eidRepository,
                     ScopeRepository scopeRepository,
                     StaticDataService staticDataService,
                     final ApFragmentItemRepository fragmentItemRepository) {
        this.apRepository = apRepository;
        this.nameRepository = nameRepository;
        this.descRepository = descRepository;
        this.eidRepository = eidRepository;
        this.scopeRepository = scopeRepository;
        this.staticDataService = staticDataService;
        this.fragmentItemRepository = fragmentItemRepository;
    }

    /**
     * Creates simple value object from AP.
     */
    public ApRecordSimple createVOSimple(ApAccessPoint ap) {
        ApName prefName = nameRepository.findPreferredNameByAccessPoint(ap);
        ApDescription desc = descRepository.findByAccessPoint(ap);
        // create VO
        ApRecordSimple vo = new ApRecordSimple();
        vo.setApTypeId(ap.getApTypeId());
        vo.setId(ap.getAccessPointId());
        vo.setRecord(prefName.getFullName());
        if (desc != null) {
            vo.setCharacteristics(desc.getDescription());
        }
        return vo;
    }

    /**
     * Creates AP from value object.
     * If id is present persist entity must be found.
     * If id is null new AP is created without create change.
     */
    public ApAccessPoint create(ApAccessPointVO apVO) {
        Integer id = apVO.getId();
        if (id != null) {
            ApAccessPoint ap = apRepository.findOne(id);
            return Validate.notNull(ap);
        }
        Validate.isTrue(!apVO.isInvalid());
        // prepare type and scope
        StaticDataProvider staticData = staticDataService.getData();
        ApType type = staticData.getApTypeById(apVO.getTypeId());
        ApScope scope = scopeRepository.findOne(apVO.getScopeId());
        // create new AP
        ApAccessPoint entity = new ApAccessPoint();
        //entity.setAccessPointId(accessPointId);
        entity.setApType(Validate.notNull(type));
        //entity.setCreateChange(createChange);
        //entity.setDeleteChange(deleteChange);
        entity.setScope(Validate.notNull(scope));
        entity.setUuid(apVO.getUuid());
        return entity;
    }

    /**
     * Creates value object from AP. Party Id is not set.
     */
    public ApAccessPointVO createVO(ApAccessPoint ap) {
        StaticDataProvider staticData = staticDataService.getData();
        ApDescription desc = descRepository.findByAccessPoint(ap);
        // prepare names
        List<ApName> names = nameRepository.findByAccessPoint(ap);
        ApName prefName = names.get(0);
        Validate.isTrue(prefName.isPreferredName());
        List<ApAccessPointNameVO> namesVO = transformList(names, n -> ApAccessPointNameVO.newInstance(n, staticData));
        // prepare external ids
        List<ApExternalId> eids = eidRepository.findByAccessPoint(ap);
        List<ApExternalIdVO> eidsVO = transformList(eids, ApExternalIdVO::newInstance);
        // create VO
        ApAccessPointVO vo = new ApAccessPointVO();
        vo.setExternalIds(eidsVO);
        vo.setId(ap.getAccessPointId());
        vo.setInvalid(ap.getDeleteChange() != null);
        vo.setNames(namesVO);
        // vo.setPartyId(partyId);
        vo.setRecord(prefName.getFullName());
        vo.setScopeId(ap.getScopeId());
        vo.setTypeId(ap.getApTypeId());
        vo.setUuid(ap.getUuid());
        if (desc != null) {
            vo.setCharacteristics(desc.getDescription());
        }

        // TODO: remove these properties from client
        //result.setExternalSystem();
        //result.setNote();
        //result.setVersion();
        //result.setParents();
        // TODO: client must read AP type from storage by typeId
        //result.setTypesToRoot();
        //result.setAddRecord();
        // TODO: change to more detail history (undo) overview
        //result.setLastUpdate(findLastUpdate(accessPointData));

        return vo;
    }

    public ApAccessPointNameVO createVO(ApName name) {
        StaticDataProvider staticData = staticDataService.getData();
        return ApAccessPointNameVO.newInstance(name, staticData);
    }

    public ApFragmentTypeVO createVO(ApFragmentType type) {
        return new ApFragmentTypeVO(type.getFragmentTypeId(), type.getCode(), type.getName());
    }

    public ApFragmentVO createVO(final ApFragment fragment) {
        ApFragmentVO vo = ApFragmentVO.newInstance(fragment);
        vo.setForm(createFormVO(fragment));
        return vo;
    }

    private ApFormVO createFormVO(final ApFragment fragment) {
        ApFormVO form = new ApFormVO();
        List<ApFragmentItem> fragmentItems = fragmentItemRepository.findValidItemsByFragment(fragment);
        List<ApItemVO> items = new ArrayList<>(fragmentItems.size());
        form.setItems(items);

        for (ApFragmentItem fragmentItem : fragmentItems) {
            //items.add(createItem(fragmentItem));
        }

        // TODO

        return form;
    }

    /**
     * Creates types with theirs parent hierarchy up to root.
     *
     * @return List of root nodes which contains given types on proper parent path.
     */
    public List<ApTypeVO> createTypesWithHierarchy(Collection<ApType> types) {
        if (CollectionUtils.isEmpty(types)) {
            return Collections.emptyList();
        }

        StaticDataProvider staticData = staticDataService.getData();
        Map<Integer, ApTypeVO> typeIdVOMap = new HashMap<>();
        List<ApTypeVO> rootsVO = new ArrayList<>();

        for (ApType type : types) {
            createTypeHierarchy(type, typeIdVOMap, rootsVO, staticData);
        }

        rootsVO.sort(Comparator.comparing(ApTypeVO::getId));
        return rootsVO;
    }

    /**
     * Creates type with his parent hierarchy up to root.
     */
    private ApTypeVO createTypeHierarchy(ApType type,
                                         Map<Integer, ApTypeVO> typeIdVOMap,
                                         List<ApTypeVO> rootsVO,
                                         StaticDataProvider staticData) {
        ApTypeVO typeVO = typeIdVOMap.get(type.getApTypeId());
        if (typeVO != null) {
            return typeVO;
        }

        typeVO = ApTypeVO.newInstnace(type, staticData);
        typeIdVOMap.put(typeVO.getId(), typeVO);

        if (type.getParentApTypeId() != null) {
            ApType parent = staticData.getApTypeById(type.getParentApTypeId());
            ApTypeVO parentVO = createTypeHierarchy(parent, typeIdVOMap, rootsVO, staticData);
            parentVO.addChild(typeVO);
            // TODO: parent names is needed/used on client?
            typeVO.addParent(parentVO.getName());
            typeVO.addParents(parentVO.getParents());
        } else {
            rootsVO.add(typeVO);
        }
        return typeVO;
    }

    public static <S, T> List<T> transformList(List<S> src, Function<S, T> transform) {
        if (src == null) {
            return null;
        }
        int size = src.size();
        if (size == 0) {
            return Collections.emptyList();
        }
        List<T> target = new ArrayList<>(src.size());
        for (S srcItem : src) {
            T targetItem = transform.apply(srcItem);
            target.add(targetItem);
        }
        return target;
    }
}
