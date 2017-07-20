package cz.tacr.elza.service.importnodes;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.vo.NodeTypeOperation;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.importnodes.vo.File;
import cz.tacr.elza.service.importnodes.vo.ImportParams;
import cz.tacr.elza.service.importnodes.vo.ImportSource;
import cz.tacr.elza.service.importnodes.vo.Node;
import cz.tacr.elza.service.importnodes.vo.Packet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * Obsluha importního procesu zdroje do AS.
 *
 * @since 19.07.2017
 */
@Component
@Scope("prototype")
public class ImportProcess {

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    /**
     * Zdroj dat pro import.
     */
    private ImportSource source;

    /**
     * Parametry importu.
     */
    private ImportParams params;

    /**
     * Cílová verze AS.
     */
    private ArrFundVersion targetFundVersion;

    /**
     * Cílový uzel importu.
     */
    private ArrNode targetNode;

    private Map<File, ArrFile> fileMap = new HashMap<>();
    private Map<Packet, ArrPacket> packetMap = new HashMap<>();

    public ImportProcess() {

    }

    /**
     * Inicializace importního procesu.
     *
     * @param source            zdroj dat importu
     * @param params            parametry importu
     * @param targetFundVersion cílová verze AS
     * @param targetNode        cílový uzel importu
     */
    public void init(final ImportSource source,
                     final ImportParams params,
                     final ArrFundVersion targetFundVersion,
                     final ArrNode targetNode) {
        this.source = source;
        this.params = params;
        this.targetFundVersion = targetFundVersion;
        this.targetNode = targetNode;
    }

    /**
     * Spuštění importu.
     */
    public void run() {

        ArrChange change = arrangementService.createChange(ArrChange.Type.IMPORT);

        List<ArrLevel> levels = new ArrayList<>();
        Stack<Data> stack = new Stack<>();
        while (source.hasNext()) {
            Node node = source.getNext((deep) -> {
                switch (deep) {
                    case UP:
                        stack.pop();
                        stack.peek().incPosition();
                        break;
                    case DOWN:
                        stack.push(new Data(1, stack.peek().getPrevNode()));
                        break;
                    case NONE:
                        stack.peek().incPosition();
                        break;
                    case RESET:
                        Integer position = levelRepository.findMaxPositionUnderParent(targetNode);
                        stack.push(new Data(position == null ? 1 : position + 1, targetNode));
                        break;
                }
            });

            Data peek = stack.peek();

            ArrLevel level = arrangementService.createLevelSimple(change, peek.getParentNode(), peek.getPosition(), targetFundVersion.getFund());
            levels.add(level);

            peek.setPrevNode(level.getNode());
        }

        List<Integer> nodeIds = levels.stream().map(level -> level.getNode().getNodeId()).collect(Collectors.toList());

        ruleService.conformityInfo(targetFundVersion.getFundVersionId(), nodeIds,
                NodeTypeOperation.CREATE_NODE, null, null, null);

        levelTreeCacheService.invalidateFundVersion(targetFundVersion);
        levelRepository.save(levels);
    }

    /**
     * Pomocná data pro vytváření nové struktury.
     */
    private class Data {

        /**
         * Pozice v úrovni.
         */
        private int position;

        /**
         * Předchozí zpracovaný uzel.
         */
        private ArrNode prevNode;

        /**
         * Rodičovský uzel úrovně.
         */
        private ArrNode parentNode;

        public Data(final int position, final ArrNode parentNode) {
            this.position = position;
            this.parentNode = parentNode;
        }

        public int getPosition() {
            return position;
        }

        public void incPosition() {
            position++;
        }

        public ArrNode getParentNode() {
            return parentNode;
        }

        public ArrNode getPrevNode() {
            return prevNode;
        }

        public void setPrevNode(final ArrNode prevNode) {
            this.prevNode = prevNode;
        }
    }

}
