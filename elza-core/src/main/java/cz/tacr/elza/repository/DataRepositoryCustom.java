package cz.tacr.elza.repository;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;

public interface DataRepositoryCustom {

    /**
     * Provede načtení unikátních hodnot atributů.
     *
     * @param version       id verze stromu
     * @param descItemType  typ atributu
     * @param dataTypeClass třída hodnot atributu
     * @param specs         pokud je typ se specifikací, obsahuje filtr specifikací
     * @param withoutSpec   příznak zda se mají hledat hodnoty bez specifikace
     * @param fulltext      fulltext
     * @param max           maximální počet hodnot
     * @return seznam unikátních hodnot
     */
    List<String> findUniqueSpecValuesInVersion(ArrFundVersion version,
                                               RulItemType descItemType,
                                               Class<? extends ArrData> dataTypeClass,
                                               @Nullable Set<RulItemSpec> specs,
                                               boolean withoutSpec, @Nullable String fulltext,
                                               int max);

    /**
     * Provede načtení unikátních specifikací hodnot atributů.
     *
     * @param version       id verze stromu
     * @param descItemType  typ atributu
     *
     * @param nodeIds       seznam ident. JP pro omezení specifikací
     * @return seznam unikátních hodnot
     */
    List<Integer> findUniqueSpecIdsInVersion(ArrFundVersion version,
                                             RulItemType descItemType,
                                             List<Integer> nodeIds);

}
