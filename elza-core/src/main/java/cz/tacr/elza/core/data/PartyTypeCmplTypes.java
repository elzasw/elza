package cz.tacr.elza.core.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ParComplementType;
import cz.tacr.elza.domain.projection.PartyTypeCmplTypeInfo;
import cz.tacr.elza.repository.PartyTypeComplementTypeRepository;

public class PartyTypeCmplTypes {

    private final PartyType partyType;

    private List<ParComplementType> cmplTypes;

    private Map<String, PartyTypeCmplType> cmplTypeCodeMap;

    PartyTypeCmplTypes(PartyType partyType) {
        this.partyType = partyType;
    }

    public PartyType gePartyType() {
        return partyType;
    }

    public List<ParComplementType> getCmplTypes() {
        return Collections.unmodifiableList(cmplTypes);
    }

    public PartyTypeCmplType getCmplTypeByCode(String code) {
        Validate.notEmpty(code);
        return cmplTypeCodeMap.get(code);
    }

    /**
     * Init all values. Method must be called inside transaction and synchronized.
     */
    void init(Map<String, ParComplementType> complementTypeCodeMap,
            PartyTypeComplementTypeRepository partyTypeCmplTypeRepository) {
        // find complements types for party type
        List<PartyTypeCmplTypeInfo> cmplTypesInfo = partyTypeCmplTypeRepository
                .findInfoByPartyTypeCode(partyType.getCode());

        List<ParComplementType> cmplTypes = new ArrayList<>(cmplTypesInfo.size());
        Map<String, PartyTypeCmplType> codeMap = new HashMap<>(cmplTypesInfo.size());

        // create lookups
        for (PartyTypeCmplTypeInfo info : cmplTypesInfo) {
            ParComplementType cmplType = complementTypeCodeMap.get(info.getComplementTypeCode());
            cmplTypes.add(Validate.notNull(cmplType));
            boolean repeatable = info.getRepeatable() != null ? info.getRepeatable() : false;
            codeMap.put(cmplType.getCode(), new PartyTypeCmplType(cmplType, repeatable));
        }
        // update fields
        this.cmplTypes = Collections.unmodifiableList(cmplTypes);
        this.cmplTypeCodeMap = codeMap;
    }
}
