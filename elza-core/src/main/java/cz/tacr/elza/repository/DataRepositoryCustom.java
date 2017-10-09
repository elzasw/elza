package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import cz.tacr.elza.domain.*;
import cz.tacr.elza.domain.RulItemSpec;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 03.02.2016
 */
public interface DataRepositoryCustom {

    @Deprecated
    List<ArrData> findDescItemsByNodeIds(Set<Integer> nodeIds,
                                         Set<RulItemType> descItemTypes,
                                         ArrFundVersion version);

    @Deprecated
    List<ArrData> findDescItemsByNodeIds(Set<Integer> nodeIds,
                                         Set<RulItemType> itemTypes,
                                         Integer changeId);

    @Deprecated
    List<ArrData> findByDataIdsAndVersionFetchSpecification(Set<Integer> nodeIds,
            Set<RulItemType> descItemTypes,
            ArrFundVersion version);

    List<ArrData> findByDataIdsAndVersionFetchSpecification(Set<Integer> nodeIds,
                                                            Set<RulItemType> itemTypes,
                                                            Integer changeId);

    /**
     * Najde seznam hodnot atributů obsahujících hledaný text.
     * @param <T>
     * @param nodes seznam uzlů, ve kterých hledáme
     * @param descItemType typ atributu
     * @param specifications seznam specifikací (pokud se jedná o typ atributu se specifikací)
     *@param text hledaný text  @return seznam hodnot atributů
     */
    @Deprecated
    <T extends ArrData> List<T> findByNodesContainingText(Collection<ArrNode> nodes, RulItemType descItemType,
                                                          final Set<RulItemSpec> specifications, String text);


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
                                                 RulItemType descItemType,
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
                                               RulItemType descItemType,
                                               Class<? extends ArrData> dataTypeClass,
                                               @Nullable Set<RulItemSpec> specs,
                                               boolean withoutSpec, @Nullable String fulltext,
                                               int max);
}
