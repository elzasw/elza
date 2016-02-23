package cz.tacr.elza.xmlimport;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.math.RandomUtils;

import cz.tacr.elza.domain.ParComplementType;
import cz.tacr.elza.domain.ParPartyNameFormType;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParRelationRoleType;
import cz.tacr.elza.domain.ParRelationType;
import cz.tacr.elza.domain.RegExternalSource;
import cz.tacr.elza.domain.RegRegisterType;

/**
 * Nastavení generátoru dat pro testy xml imprtu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 23. 11. 2015
 */
public class XmlDataGeneratorConfig {

    /** Prvky nastavení. */
    private int recordCount;

    private int variantRecordCount;

    private int partyCount;

    private int childrenCount;

    private int treeDepth;

    private int descItemsCount;

    private boolean valid;

    private int eventCount;

    private int partyGroupIdCount;

    private int partyNameComplementsCount;

    private int packetCount;
    /** Konec nastavení. */

    private List<ParPartyType> partyTypes;

    private List<ParPartyNameFormType> partyNameFormTypes;

    private List<ParComplementType> complementTypes;

    private List<ParRelationType> relationTypes;

    private List<ParRelationRoleType> relationRoleTypes;

    private List<RegRegisterType> registerTypes;

    private List<RegExternalSource> externalSources;

    public XmlDataGeneratorConfig(final int recordCount, final int variantRecordCount, final int partyCount,
                                  final int childrenCount, final int treeDepth, final int descItemsCount, final boolean valid,
                                  final int eventCount, final int partyGroupIdCount,
                                  final int partyNameComplementsCount, final int packetCount) {
        this.recordCount = recordCount;
        this.variantRecordCount = variantRecordCount;
        this.partyCount = partyCount;
        this.childrenCount = childrenCount;
        this.treeDepth = treeDepth;
        this.descItemsCount = descItemsCount;
        this.valid = valid;
        this.eventCount = eventCount;
        this.partyGroupIdCount = partyGroupIdCount;
        this.partyNameComplementsCount = partyNameComplementsCount;
        this.packetCount = packetCount;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(int recordCount) {
        this.recordCount = recordCount;
    }

    public int getVariantRecordCount() {
        return variantRecordCount;
    }

    public void setVariantRecordCount(int variantRecordCount) {
        this.variantRecordCount = variantRecordCount;
    }

    public int getPartyCount() {
        return partyCount;
    }

    public void setPartyCount(int partyCount) {
        this.partyCount = partyCount;
    }

    public int getChildrenCount() {
        return childrenCount;
    }

    public void setChildrenCount(int childrenCount) {
        this.childrenCount = childrenCount;
    }

    public int getTreeDepth() {
        return treeDepth;
    }

    public void setTreeDepth(int treeDepth) {
        this.treeDepth = treeDepth;
    }

    public int getDescItemsCount() {
        return descItemsCount;
    }

    public void setDescItemsCount(int descItemsCount) {
        this.descItemsCount = descItemsCount;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public int getEventCount() {
        return eventCount;
    }

    public void setEventCount(int eventCount) {
        this.eventCount = eventCount;
    }

    public int getPartyGroupIdCount() {
        return partyGroupIdCount;
    }

    public void setPartyGroupIdCount(int partyGroupIdCount) {
        this.partyGroupIdCount = partyGroupIdCount;
    }

    public int getPartyNameComplementsCount() {
        return partyNameComplementsCount;
    }

    public void setPartyNameComplementsCount(int partyNameComplementsCount) {
        this.partyNameComplementsCount = partyNameComplementsCount;
    }

    public int getPacketCount() {
        return packetCount;
    }

    public String getRandomPartyTypeCode() {
        if (CollectionUtils.isEmpty(partyTypes)) {
            return "partyTypeCode " + RandomUtils.nextInt();
        }

        return partyTypes.get(RandomUtils.nextInt(partyTypes.size())).getCode();
    }

    public void setPartyTypes(List<ParPartyType> partyTypes) {
        this.partyTypes = partyTypes;
    }

    public String getRandomPartyNameFormTypeCode() {
        if (CollectionUtils.isEmpty(partyNameFormTypes)) {
            return "partyNameFormTypeCode " + RandomUtils.nextInt();
        }

        return partyNameFormTypes.get(RandomUtils.nextInt(partyNameFormTypes.size())).getCode();
    }

    public void setPartyNameFormTypes(List<ParPartyNameFormType> partyNameFormTypes) {
        this.partyNameFormTypes = partyNameFormTypes;
    }

    public String getRandomComplementTypeCode() {
        if (CollectionUtils.isEmpty(complementTypes)) {
            return "partyNameComplementTypeCode " + RandomUtils.nextInt();
        }

        return complementTypes.get(RandomUtils.nextInt(complementTypes.size())).getCode();
    }

    public void setComplementTypes(List<ParComplementType> complementTypes) {
        this.complementTypes = complementTypes;
    }

    public ParRelationType getRandomRelationType() {
        if (CollectionUtils.isEmpty(relationTypes)) {
            return null;
        }

        return relationTypes.get(RandomUtils.nextInt(relationTypes.size()));
    }

    public void setRelationTypes(List<ParRelationType> relationTypes) {
        this.relationTypes = relationTypes;
    }

    public String getRandomRelationRoleTypeCode() {
        if (CollectionUtils.isEmpty(relationRoleTypes)) {
            return "roleTypeCode " + RandomUtils.nextInt();
        }

        return relationRoleTypes.get(RandomUtils.nextInt(relationRoleTypes.size())).getCode();
    }

    public void setRelationRoleTypes(List<ParRelationRoleType> relationRoleTypes) {
        this.relationRoleTypes = relationRoleTypes;
    }

    public String getRandomRegisterTypeCode() {
        if (CollectionUtils.isEmpty(registerTypes)) {
            return "registerTypeCode " + RandomUtils.nextInt();
        }

        return registerTypes.get(RandomUtils.nextInt(registerTypes.size())).getCode();
    }

    public void setRegisterTypes(List<RegRegisterType> registerTypes) {
        this.registerTypes = registerTypes;
    }

    public String getRandomExternalSourceCode() {
        if (CollectionUtils.isEmpty(externalSources)) {
            return "externalSourceCode " + RandomUtils.nextInt();
        }

        return externalSources.get(RandomUtils.nextInt(externalSources.size())).getCode();
    }

    public void setExternalSources(List<RegExternalSource> externalSources) {
        this.externalSources = externalSources;
    }

}
