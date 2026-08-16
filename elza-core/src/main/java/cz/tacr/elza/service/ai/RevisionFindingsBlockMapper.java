package cz.tacr.elza.service.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import cz.tacr.elza.controller.vo.AiDisplayBlockVO;
import cz.tacr.elza.controller.vo.AiFollowUpAction;
import cz.tacr.elza.controller.vo.AiMarkdownBlockVO;

/**
 * Renders an {@code elza.revisionFindings} result block (the advisory findings
 * of {@code elza.revision}, tasks/elza-revision.md §4) as readable markdown
 * display blocks — <b>one block per finding</b>, each carrying a "prepare the
 * fix" follow-up action: clicking it submits an {@code elza.enhanceDescription}
 * exchange into the same conversation with a seeded instruction naming the
 * finding (the provider chains the thread via {@code parentTaskId} and replays
 * the findings into the fix task's material — the revision→fix handoff,
 * elza-ai-provider {@code doc/enhance-description-proposal.md} §9). The
 * structured triage UI ("AI doporučení" cards) and the accept-side
 * {@code WfIssue} conversion come later, reading the same typed block.
 *
 * <p>Labels are Czech: the pilot targets Czech archives and the findings'
 * human-readable texts arrive in the run's configured language ({@code cs});
 * localizing the few label words separately from the finding texts would only
 * produce mixed-language output. Category/severity/action codes are open sets —
 * an unknown code is shown raw rather than dropped.
 */
@Component
public class RevisionFindingsBlockMapper implements AiBlockMapper {

    /** Task type of the "prepare the fix" follow-up exchange. */
    static final String ENHANCE_TASK_TYPE = "elza.enhanceDescription";

    private static final Map<String, String> CATEGORY_LABELS = Map.of(
            "hidden_data", "Skryté strukturované údaje",
            "note_misuse", "Nevhodné použití poznámky",
            "wrong_level", "Nesprávná úroveň popisu",
            "duplicate", "Duplicitní údaj",
            "formulation", "Formulace",
            "contradiction", "Rozpor",
            "inconsistency", "Nekonzistence",
            "access_point_candidate", "Kandidát na přístupový bod");

    private static final Map<String, String> SEVERITY_LABELS = Map.of(
            "high", "vysoká",
            "medium", "střední",
            "low", "nízká");

    private static final Map<String, String> ACTION_LABELS = Map.of(
            "move", "přesunout",
            "split", "rozdělit",
            "verify", "ověřit",
            "reformulate", "přeformulovat",
            "keep", "ponechat");

    @Override
    public Set<String> objectTypes() {
        return Set.of("elza.revisionFindings");
    }

    @Override
    public List<AiDisplayBlockVO> map(final JsonNode data) {
        JsonNode findings = data.path("findings");
        if (!findings.isArray() || findings.isEmpty()) {
            return List.of(new AiMarkdownBlockVO()
                    .content("Nebyly nalezeny podložené sémantické ani formulační nesrovnalosti."));
        }

        // Node ids are shown only when the findings span several levels (a
        // branch run); a single-unit check would just repeat the same number.
        boolean multiNode = findings.findValuesAsText("nodeId").stream().distinct().count() > 1;

        List<AiDisplayBlockVO> blocks = new ArrayList<>();
        int order = 0;
        for (JsonNode finding : findings) {
            order++;
            StringBuilder md = new StringBuilder();
            String category = label(CATEGORY_LABELS, finding.path("category").asText(""));
            md.append("### ").append(order).append(". ").append(category);
            String severity = label(SEVERITY_LABELS, finding.path("severity").asText(""));
            if (!severity.isBlank()) {
                md.append(" — závažnost ").append(severity);
            }
            if (multiNode && finding.path("nodeId").isNumber()) {
                md.append(" (JP ").append(finding.path("nodeId").asInt()).append(')');
            }
            md.append('\n');

            String excerpt = finding.path("excerpt").asText("");
            if (!excerpt.isBlank()) {
                // The verbatim evidence, as a quote; multiline excerpts stay a quote.
                md.append("> ").append(excerpt.replace("\n", "\n> ")).append("\n\n");
            }
            String explanation = finding.path("explanation").asText("");
            if (!explanation.isBlank()) {
                md.append(explanation).append('\n');
            }

            String sourceItemType = finding.path("sourceItemType").asText("");
            if (!sourceItemType.isBlank()) {
                md.append("- Zdrojový prvek: `").append(sourceItemType).append("`\n");
            }
            String action = label(ACTION_LABELS, finding.path("action").asText(""));
            String targetItemType = finding.path("targetItemType").asText("");
            if (!action.isBlank() || !targetItemType.isBlank()) {
                md.append("- Doporučení: ");
                md.append(action.isBlank() ? "—" : action);
                if (!targetItemType.isBlank()) {
                    md.append(" do prvku `").append(targetItemType).append('`');
                }
                md.append('\n');
            }
            String proposedText = finding.path("proposedText").asText("");
            if (!proposedText.isBlank()) {
                md.append("- Návrh formulace: ").append(proposedText).append('\n');
            }
            if (finding.path("confidence").isNumber()) {
                md.append("- Jistota: ").append(finding.path("confidence").asInt()).append(" %\n");
            }

            AiMarkdownBlockVO block = new AiMarkdownBlockVO().content(md.toString());
            block.addFollowUpsItem(new AiFollowUpAction()
                    .label("Připravit opravu")
                    .taskType(ENHANCE_TASK_TYPE)
                    .userInstructions(fixInstruction(order, category, excerpt)));
            blocks.add(block);
        }
        return blocks;
    }

    /**
     * The seeded instruction of a finding's "prepare the fix" follow-up. The
     * fix task receives the complete findings replayed through the conversation
     * chain, so the seed only needs to pin WHICH finding — by number, category
     * and (when present) the verbatim excerpt.
     */
    private static String fixInstruction(final int order, final String category, final String excerpt) {
        StringBuilder instruction = new StringBuilder("Připrav opravu zjištění č. ").append(order);
        if (!category.isBlank()) {
            instruction.append(" (").append(category).append(')');
        }
        if (!excerpt.isBlank()) {
            instruction.append(": „").append(excerpt).append('“');
        }
        instruction.append('.');
        return instruction.toString();
    }

    /** The label for a code; an unknown code (the sets are open) is shown raw. */
    private static String label(final Map<String, String> labels, final String code) {
        if (code.isBlank()) {
            return "";
        }
        return labels.getOrDefault(code, code);
    }
}
