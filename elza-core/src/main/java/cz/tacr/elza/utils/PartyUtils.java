package cz.tacr.elza.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;

import cz.tacr.elza.domain.ParParty;


/**
 * Pomocné metody pro osoby.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
public class PartyUtils {

    /**
     * převede seznam osob na mapu id rejstříkového hesla -> osoba
     *
     * @param partyList seznam osob
     * @return mapa id rejstříkového hesla -> osoba
     */
    public static Map<Integer, ParParty> createRecordPartyMap(final Collection<ParParty> partyList) {
        if (CollectionUtils.isEmpty(partyList)) {
            return Collections.emptyMap();
        }

        Map<Integer, ParParty> result = new HashMap<>();
        partyList.forEach(p -> result.put(p.getRecord().getRecordId(), p));
        return result;
    }

}
