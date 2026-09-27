package cz.tacr.elza.service.da;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.archivists.ead3.schema.Archdesc;
import org.archivists.ead3.schema.C;
import org.archivists.ead3.schema.Dsc;
import org.archivists.ead3.schema.Ead;
import org.archivists.ead3.schema.Fileplan;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.RuleSet;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.RulArrangementRule;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.service.GroovyScriptService;
import gov.loc.mets.v1_11.schema.DivType;
import gov.loc.mets.v1_11.schema.MetsType;
import gov.loc.mets.v1_11.schema.StructMapType;

/**
 * Plans the import of a package into the archival description: which divs of its logical
 * structural map become levels, which are attached to the level above them and which are left
 * out, and which items the levels get from the EAD.
 *
 * The decisions are made by the DA_IMPORT script of the rule set of the fund the package is
 * imported to - an arrangement rule of type {@link RulArrangementRule.RuleType#DA_IMPORT}; of
 * several, the one of the highest priority, so a package extending the rules can override it.
 * The script is called once for every div, top-down, with {@code PACKAGE}
 * ({@link DaImportPackage}), {@code LEVEL} ({@link DaImportLevel}) and {@code RESULT}
 * ({@link DaImportResult}).
 */
@Service
public class DaImportPlanner {

    private static final String LOGICAL = "LOGICAL";

    private final StaticDataService staticDataService;
    private final GroovyScriptService groovyScriptService;
    private final ResourcePathResolver resourcePathResolver;

    public DaImportPlanner(StaticDataService staticDataService, GroovyScriptService groovyScriptService,
                           ResourcePathResolver resourcePathResolver) {
        this.staticDataService = staticDataService;
        this.groovyScriptService = groovyScriptService;
        this.resourcePathResolver = resourcePathResolver;
    }

    /**
     * @param ead the inherent archival description of the package; null when it has none - the
     *            divs are then decided on without units
     * @param eadHref path of the EAD inside the package, for the error messages
     * @return the plan; empty when the rule set has no DA_IMPORT script, so packages cannot be
     *         imported into its funds
     * @throws AipProblemException when the EAD is not written the way it can be read
     * @throws SystemException when the script decides wrongly
     */
    @Transactional(Transactional.TxType.MANDATORY)
    public Optional<DaImportPlan> plan(MetsType mets, @Nullable Ead ead, @Nullable String eadHref,
                                       Integer ruleSetId, DaImportPackage importPackage) {
        StaticDataProvider sdp = staticDataService.getData();
        String scriptPath = findScript(sdp.getRuleSetById(ruleSetId));
        if (scriptPath == null) {
            return Optional.empty();
        }
        Walk walk = new Walk(sdp, scriptPath, importPackage, ead, eadHref);
        DaImportPlan plan = new DaImportPlan();
        for (StructMapType structMap : mets.getStructMap()) {
            if (LOGICAL.equals(structMap.getTYPE()) && structMap.getDiv() != null) {
                walk.div(structMap.getDiv(), null, plan, null, false);
            }
        }
        return Optional.of(plan);
    }

    /**
     * Plans what lies below one div - the div that is described already, e.g. matched onto a
     * unit of description by its UUID. The div itself is not planned; the divs below it are the
     * roots of the plan.
     *
     * @param startUuid UUID of the div, as {@link AipNodeUuids#normalize(String)} gives it
     * @return the plan; empty when the rule set has no DA_IMPORT script, or when no div of the
     *         logical structural map has the UUID - it belongs to another part of the package
     * @see #plan(MetsType, Ead, String, Integer, DaImportPackage)
     */
    @Transactional(Transactional.TxType.MANDATORY)
    public Optional<DaImportPlan> planBelow(MetsType mets, @Nullable Ead ead, @Nullable String eadHref,
                                            Integer ruleSetId, DaImportPackage importPackage, String startUuid) {
        StaticDataProvider sdp = staticDataService.getData();
        String scriptPath = findScript(sdp.getRuleSetById(ruleSetId));
        if (scriptPath == null) {
            return Optional.empty();
        }
        Walk walk = new Walk(sdp, scriptPath, importPackage, ead, eadHref);
        for (StructMapType structMap : mets.getStructMap()) {
            if (LOGICAL.equals(structMap.getTYPE()) && structMap.getDiv() != null) {
                Optional<DaImportPlan> plan = walk.below(structMap.getDiv(), null, startUuid);
                if (plan.isPresent()) {
                    return plan;
                }
            }
        }
        return Optional.empty();
    }

    /**
     * The divs directly below a div as levels without items - for funds whose rules cannot
     * import packages. Without rules nothing can be read from the EAD; the levels only carry the
     * parts of the package attached to them.
     *
     * @param startUuid UUID of the div; null for the top of the logical structural map
     */
    public DaImportPlan planDivsBelow(MetsType mets, @Nullable String startUuid) {
        DaImportPlan plan = new DaImportPlan();
        for (StructMapType structMap : mets.getStructMap()) {
            if (!LOGICAL.equals(structMap.getTYPE()) || structMap.getDiv() == null) {
                continue;
            }
            List<DivType> divs = startUuid == null ? List.of(structMap.getDiv())
                    : childrenOf(structMap.getDiv(), startUuid);
            for (DivType div : divs) {
                plan.addRoot(new DaImportPlan.Node(div.getID(), StringUtils.trimToNull(div.getLABEL()),
                        DaImportResult.Decision.LEVEL, List.of(), List.of()));
            }
        }
        return plan;
    }

    private static List<DivType> childrenOf(DivType div, String uuid) {
        if (uuid.equals(AipNodeUuids.normalize(div.getID()))) {
            return div.getDiv();
        }
        for (DivType child : div.getDiv()) {
            List<DivType> found = childrenOf(child, uuid);
            if (!found.isEmpty()) {
                return found;
            }
        }
        return List.of();
    }

    @Nullable
    /** Whether the rule set can import packages at all - it has a DA_IMPORT script. */
    public boolean canImport(Integer ruleSetId) {
        return !staticDataService.getData().getRuleSetById(ruleSetId)
                .getRulesByType(RulArrangementRule.RuleType.DA_IMPORT).isEmpty();
    }

    /**
     * The rule set and the rules package it comes from, e.g. "ZP2015, balíček ZP2015 verze 347" -
     * for messages; an old package is the usual reason why a rule is missing.
     */
    public String ruleSetLabel(Integer ruleSetId) {
        RulRuleSet ruleSet = staticDataService.getData().getRuleSetById(ruleSetId).getEntity();
        RulPackage rulPackage = ruleSet.getPackage();
        return rulPackage == null ? ruleSet.getCode()
                : ruleSet.getCode() + ", balíček " + rulPackage.getCode() + " verze " + rulPackage.getVersion();
    }

    private String findScript(RuleSet ruleSet) {
        List<RulArrangementRule> rules = ruleSet.getRulesByType(RulArrangementRule.RuleType.DA_IMPORT);
        if (rules.isEmpty()) {
            return null;
        }
        // sorted by priority, the last one wins
        return resourcePathResolver.getDroolFile(rules.get(rules.size() - 1)).toString();
    }

    /** One walk through the logical structural map. */
    private class Walk {

        private final StaticDataProvider sdp;
        private final String scriptPath;
        private final DaImportPackage importPackage;
        private final String eadHref;
        private final Map<String, C> units = new HashMap<>();
        private final Map<String, Fileplan> fileplans = new HashMap<>();

        Walk(StaticDataProvider sdp, String scriptPath, DaImportPackage importPackage, @Nullable Ead ead,
             @Nullable String eadHref) {
            this.sdp = sdp;
            this.scriptPath = scriptPath;
            this.importPackage = importPackage;
            this.eadHref = eadHref;
            if (ead != null && ead.getArchdesc() != null) {
                index(ead.getArchdesc());
            }
        }

        private void index(Archdesc archdesc) {
            for (Object o : archdesc.getAccessrestrictOrAccrualsOrAcqinfo()) {
                if (o instanceof Dsc dsc) {
                    dsc.getC().forEach(this::index);
                } else if (o instanceof Fileplan fileplan && fileplan.getId() != null) {
                    fileplans.put(fileplan.getId(), fileplan);
                }
            }
        }

        private void index(C c) {
            if (c.getId() != null) {
                units.put(c.getId(), c);
            }
            for (Object o : c.getTheadAndC()) {
                if (o instanceof C child) {
                    index(child);
                }
            }
        }

        /**
         * Finds the div of the UUID and plans the divs below it; the divs on the way to it are
         * not planned.
         */
        Optional<DaImportPlan> below(DivType div, @Nullable DaImportLevel parentLevel, String startUuid) {
            DaImportLevel level = level(div, parentLevel);
            if (startUuid.equals(AipNodeUuids.normalize(div.getID()))) {
                DaImportPlan plan = new DaImportPlan();
                for (DivType child : div.getDiv()) {
                    div(child, level, plan, null, false);
                }
                return Optional.of(plan);
            }
            for (DivType child : div.getDiv()) {
                Optional<DaImportPlan> plan = below(child, level, startUuid);
                if (plan.isPresent()) {
                    return plan;
                }
            }
            return Optional.empty();
        }

        /**
         * @param parentNode planned node the div hangs on; null for the root of the plan
         * @param underAttachment whether an ancestor of the div is an attachment - nothing below an
         *            attachment can be a level
         */
        void div(DivType div, @Nullable DaImportLevel parentLevel, DaImportPlan plan,
                 @Nullable DaImportPlan.Node parentNode, boolean underAttachment) {
            DaImportLevel level = level(div, parentLevel);
            DaImportResult result = new DaImportResult(sdp, level, eadHref);
            groovyScriptService.processDaImport(importPackage, level, result, scriptPath);

            DaImportResult.Decision decision = result.getDecision();
            if (decision == null) {
                throw scriptError(level, "nerozhodl, co se s ním stane (level, attach nebo skip)");
            }
            DaImportPlan.Node childParent = parentNode;
            boolean childUnderAttachment = underAttachment;
            switch (decision) {
            case SKIP -> {
                // the divs below take its place
            }
            case LEVEL -> {
                if (underAttachment) {
                    throw scriptError(level, "je určen jako úroveň, ale je součástí přílohy");
                }
                DaImportPlan.Node node = node(level, result);
                hang(plan, parentNode, node);
                childParent = node;
            }
            case ATTACH -> {
                if (!result.getItems().isEmpty()) {
                    throw scriptError(level, "je určen jako příloha, ale má prvky popisu");
                }
                hang(plan, parentNode, node(level, result));
                // what is below an attachment is attached to the same level
                childUnderAttachment = true;
            }
            }
            for (DivType child : div.getDiv()) {
                div(child, level, plan, childParent, childUnderAttachment);
            }
        }

        private DaImportLevel level(DivType div, @Nullable DaImportLevel parentLevel) {
            String id = div.getID();
            C unit = units.get(id);
            List<DaImportElement> elements = new ArrayList<>();
            if (unit != null && unit.getDid() != null) {
                for (Object element : unit.getDid().getMDid()) {
                    if (DidElementConverters.isSupported(element) && !DidElementConverters.isInherited(element)) {
                        elements.add(new DaImportElement(element));
                    }
                }
            }
            return new DaImportLevel(id, StringUtils.trimToNull(div.getTYPE()), StringUtils.trimToNull(div.getLABEL()),
                    parentLevel, fileplans.containsKey(id),
                    unit != null ? unit.getLevel() : null, unit != null ? unit.getOtherlevel() : null, elements);
        }

        private DaImportPlan.Node node(DaImportLevel level, DaImportResult result) {
            return new DaImportPlan.Node(level.getId(), level.getLabel(), result.getDecision(), result.getMatchBy(),
                    result.getItems());
        }

        private void hang(DaImportPlan plan, @Nullable DaImportPlan.Node parentNode, DaImportPlan.Node node) {
            if (parentNode == null) {
                plan.addRoot(node);
            } else {
                parentNode.addChild(node);
            }
        }

        private SystemException scriptError(DaImportLevel level, String reason) {
            return new SystemException("Skript DA_IMPORT: " + level + " " + reason, BaseCode.INVALID_STATE);
        }
    }
}
