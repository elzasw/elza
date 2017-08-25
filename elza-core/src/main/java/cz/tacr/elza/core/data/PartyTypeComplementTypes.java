package cz.tacr.elza.core.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ParComplementType;
import cz.tacr.elza.domain.projection.PartyTypeComplementTypeInfo;
import cz.tacr.elza.repository.PartyTypeComplementTypeRepository;

public class PartyTypeComplementTypes {

    private final PartyType partyType;

    private List<ParComplementType> complementTypes;

    private Map<String, PartyTypeComplementType> complementTypeCodeMap;

    PartyTypeComplementTypes(PartyType partyType) {
        this.partyType = partyType;
    }

    public PartyType gePartyType() {
        return partyType;
    }

    public List<ParComplementType> getComplementTypes() {
        return complementTypes;
    }

    public ParComplementType getTypeByCode(String code) {
        Validate.notEmpty(code);
        PartyTypeComplementType type = complementTypeCodeMap.get(code);
        return type != null ? type.entity : null;
    }

    public Boolean getRepeatableByCode(String code) {
        Validate.notEmpty(code);
        PartyTypeComplementType type = complementTypeCodeMap.get(code);
        return type != null ? type.repeatable : null;
    }

    /**
     * Init all values. Method must be called inside transaction and synchronized.
     */
    void init(Map<String, ParComplementType> complementTypeCodeMap,
              PartyTypeComplementTypeRepository partyTypeComplementTypeRepository) {
        // find cmpl types for party type
        List<PartyTypeComplementTypeInfo> cmplTypesInfo = partyTypeComplementTypeRepository
                .findInfoByPartyTypeCode(partyType.getCode());

        List<ParComplementType> cmplTypes = new ArrayList<>(cmplTypesInfo.size());
        Map<String, PartyTypeComplementType> codeMap = new HashMap<>(cmplTypesInfo.size());

        // create lookups
        for (PartyTypeComplementTypeInfo info : cmplTypesInfo) {
            ParComplementType cmplType = complementTypeCodeMap.get(info.getComplementTypeCode());
            cmplTypes.add(Validate.notNull(cmplType));
            boolean repeatable = info.getRepeatable() != null ? info.getRepeatable() : false;
            codeMap.put(cmplType.getCode(), new PartyTypeComplementType(cmplType, repeatable));
        }
        // update fields
        this.complementTypes = Collections.unmodifiableList(cmplTypes);
        this.complementTypeCodeMap = codeMap;
    }

    private static class PartyTypeComplementType {

        final ParComplementType entity;

        final boolean repeatable;

        PartyTypeComplementType(ParComplementType entity, boolean repeatable) {
            this.entity = entity;
            this.repeatable = repeatable;
        }
    }
}
