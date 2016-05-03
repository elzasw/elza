package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulPacketType;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 03.02.2016
 */
public interface DataRepositoryCustom {

    List<ArrData> findDescItemsByNodeIds(Set<Integer> nodeIds,
                                         Set<RulDescItemType> descItemTypes,
                                         ArrFundVersion version);

    List<ArrData> findByDataIdsAndVersionFetchSpecification(Set<Integer> nodeIds,
            Set<RulDescItemType> descItemTypes,
            ArrFundVersion version);


    /**
     * Najde seznam hodnot atributů obsahujících hledaný text.
     * @param <T>
     * @param nodes seznam uzlů, ve kterých hledáme
     * @param descItemType typ atributu
     * @param specifications seznam specifikací (pokud se jedná o typ atributu se specifikací)
     *@param text hledaný text  @return seznam hodnot atributů
     */
    <T extends ArrData> List<T> findByNodesContainingText(Collection<ArrNode> nodes, RulDescItemType descItemType,
                                                          final Set<RulDescItemSpec> specifications, String text);


    /**
     * Provede načtení unikátních hodnot atributů typu obal.
     *
     * @param version       id verze stromu
     * @param descItemType  typ atributu
     * @param dataTypeClass třída hodnot atributu
     * @param packetTypes   filtr typů obalů
     * @param withoutType   příznak zda se mají hledat hodnoty bez typu
     * @param fulltext      fulltext
     * @param max           maximální počet hodnot
     * @return seznam unikátních hodnot
     */
    List<String> findUniquePacketValuesInVersion(ArrFundVersion version,
                                                 RulDescItemType descItemType,
                                                 Class<? extends ArrData> dataTypeClass,
                                                 Set<RulPacketType> packetTypes,
                                                 boolean withoutType, @Nullable String fulltext,
                                                 int max);


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
                                               RulDescItemType descItemType,
                                               Class<? extends ArrData> dataTypeClass,
                                               @Nullable Set<RulDescItemSpec> specs,
                                               boolean withoutSpec, @Nullable String fulltext,
                                               int max);
}
