import {
    Button,
    Label,
    Link,
    Text,
    ToggleButton,
    Tooltip,
    makeStyles,
    tokens,
    useId,
} from '@fluentui/react-components';
import { LockClosedRegular, LockOpenRegular } from '@fluentui/react-icons';
import { WebApi } from 'actions';
import * as issuesActions from 'actions/arr/issues';
import { routerNavigate } from 'actions/router';
import * as perms from 'actions/user/Permission';
import { createReferenceMarkStringFromArray } from 'components/arr/ArrUtils';
import { daoMessages } from 'components/arr/daoMessages';
import { nodeMessages } from 'components/arr/nodeMessages';
import { IssueForm, IssueFormValues, IssueSubmitResult } from 'components/form/IssueForm';
import { useFundProtocols } from 'components/form/useFundProtocols';
import { CollapsibleDragWindow } from 'components/shared/dialog/FluentModalDialog';
import { usePermissions, useUserContext } from 'contexts/user';
import { useCallback, useEffect, useRef, useState } from 'react';
import { subscribeIssueCreateWindowReveal } from './issueCreateWindowReveal';
import { MessageDescriptor, defineMessages, useIntl } from 'react-intl';
import { useRouteMatch } from 'react-router-dom';
import { IssueListVO, IssueVO } from 'types';
import { AppState, Node, SubNode } from 'typings/store';
import { useAppSelector, useAppThunkDispatch } from 'utils/hooks';
import { URL_FUND, urlFundNode } from '../../constants';

const messages = defineMessages({
    target: { id: 'arr.issues.createWindow.target', defaultMessage: 'Připomínka k' },
    targetNode: { id: 'arr.issues.createWindow.targetNode', defaultMessage: 'Jednotka popisu' },
    noActiveNode: {
        id: 'arr.issues.createWindow.noActiveNode',
        defaultMessage: 'Není vybrána žádná jednotka popisu',
    },
    noFundOpen: { id: 'arr.issues.createWindow.noFundOpen', defaultMessage: 'Není otevřen žádný archivní soubor' },
    lockedNodeElsewhere: {
        id: 'arr.issues.createWindow.lockedNodeElsewhere',
        defaultMessage: 'Zamčená jednotka popisu je z jiného archivního souboru',
    },
    noOpenProtocol: {
        id: 'arr.issues.createWindow.noOpenProtocol',
        defaultMessage: 'V tomto archivním souboru nebyl nalezen žádný otevřený protokol',
    },
    noPermission: {
        id: 'arr.issues.createWindow.noPermission',
        defaultMessage: 'Nemáte oprávnění přidávat připomínky v tomto archivním souboru',
    },
    lockNode: { id: 'arr.issues.createWindow.lockNode', defaultMessage: 'Zamknout na této jednotce popisu' },
    unlockNode: {
        id: 'arr.issues.createWindow.unlockNode',
        defaultMessage: 'Odemknout a sledovat vybranou jednotku popisu',
    },
});

/** Side and bottom padding of the `CollapsibleDragWindow` body. */
const WINDOW_BODY_PADDING = '20px';

const useStyles = makeStyles({
    content: {
        position: 'relative',
        display: 'flex',
        flexDirection: 'column',
        flexGrow: 1,
        minHeight: 0,
    },
    overlay: {
        position: 'absolute',
        // stretched over the side and bottom padding of the window body, so the blur reaches the window edge
        top: 0,
        right: `-${WINDOW_BODY_PADDING}`,
        bottom: `-${WINDOW_BODY_PADDING}`,
        left: `-${WINDOW_BODY_PADDING}`,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: tokens.spacingVerticalM,
        padding: tokens.spacingHorizontalL,
        textAlign: 'center',
        // 10 % transparent, so the blurred form stays recognisable underneath
        backgroundColor: `color-mix(in srgb, ${tokens.colorNeutralBackground1} 90%, transparent)`,
        backdropFilter: 'blur(4px)',
    },
    target: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalS,
    },
    targetSelection: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'flex-start',
        gap: tokens.spacingVerticalXS,
    },
    targetButtons: {
        display: 'flex',
        gap: tokens.spacingHorizontalXS,
    },
    nodeRow: {
        display: 'flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalS,
        maxWidth: '100%',
        minWidth: 0,
    },
    nodeName: {
        minWidth: 0,
        overflow: 'hidden',
        whiteSpace: 'nowrap',
        textOverflow: 'ellipsis',
    },
    // muted and smaller, as in the node list of the arrangement, so it does not read as part of the name
    referenceMark: {
        color: tokens.colorNeutralForeground3,
        fontSize: tokens.fontSizeBase200,
        whiteSpace: 'nowrap',
    },
    form: {
        // stretched over the side and bottom padding of the window body and the gap below the target, so the
        // saving overlay of the form reaches the window edge like the overlay above; the padding keeps the fields
        // where they were
        margin: `0 -${WINDOW_BODY_PADDING} -${WINDOW_BODY_PADDING}`,
        padding: `${tokens.spacingVerticalM} ${WINDOW_BODY_PADDING} ${WINDOW_BODY_PADDING}`,
        flexGrow: 1,
        minHeight: 0,
        overflowY: 'auto',
    },
});

export type IssueTarget = 'fund' | 'node';

interface TargetNode {
    fundId: number;
    /** Closed version the unit was shown in; none for the current version. */
    versionId?: number;
    id: number;
    referenceMark?: string[];
    name: string;
}

const NO_ISSUE_LIST_IDS: number[] = [];

/** Why the form cannot be used right now; shown over the whole form. */
type BlockReason = 'noFundOpen' | 'lockedNodeElsewhere' | 'noOpenProtocol' | 'noPermission';

const blockMessages: Record<BlockReason, MessageDescriptor> = {
    noFundOpen: messages.noFundOpen,
    lockedNodeElsewhere: messages.lockedNodeElsewhere,
    noOpenProtocol: messages.noOpenProtocol,
    noPermission: messages.noPermission,
};

interface BlockConditions {
    openFundId: number | undefined;
    /** Fund of the locked unit of description; none while the comment follows the open fund. */
    lockedFundId: number | undefined;
    /** Among the protocols of the fund the user can see. */
    hasOpenProtocol: boolean;
    canCreateIssue: boolean;
}

function getBlockReason({
    openFundId,
    lockedFundId,
    hasOpenProtocol,
    canCreateIssue,
}: BlockConditions): BlockReason | undefined {
    if (openFundId == null) {
        return 'noFundOpen';
    }
    const isLockedElsewhere = lockedFundId != null && lockedFundId !== openFundId;
    if (isLockedElsewhere) {
        return 'lockedNodeElsewhere';
    }
    if (!hasOpenProtocol) {
        return 'noOpenProtocol';
    }
    if (!canCreateIssue) {
        return 'noPermission';
    }
    return undefined;
}

/**
 * Funds stay cached in the store after the user leaves them, so what is on screen is decided by the URL:
 * any page under fund/{id}/{subpage}. fund/{id} alone is the fund's detail in the list of funds.
 */
const OPEN_FUND_PATH = `${URL_FUND}/:id(\\d+)/:subpage`;
/** A closed version of the fund; the current version has no version in the URL. */
const OPEN_FUND_VERSION_PATH = `${URL_FUND}/:id(\\d+)/v/:versionId(\\d+)`;

function useOpenFund() {
    const fundMatch = useRouteMatch<{ id: string }>(OPEN_FUND_PATH);
    const versionMatch = useRouteMatch<{ versionId: string }>(OPEN_FUND_VERSION_PATH);
    return {
        fundId: fundMatch ? Number(fundMatch.params.id) : undefined,
        versionId: versionMatch ? Number(versionMatch.params.versionId) : undefined,
    };
}

/**
 * Whether a comment can go to a protocol of the fund, as the server checks it: the protocol is open and the user
 * may write to it; an issue admin of the fund may write to all of them.
 */
function useCanAddToProtocol(fundId: number | undefined) {
    const { hasOne } = usePermissions();
    const { userDetail } = useUserContext();
    const isIssueAdmin = fundId != null && hasOne(perms.FUND_ISSUE_ADMIN_ALL, { type: perms.FUND_ISSUE_ADMIN, fundId });
    const writableIssueListIds =
        userDetail.permissionsMap?.[perms.FUND_ISSUE_LIST_WR]?.issueListIds ?? NO_ISSUE_LIST_IDS;

    return useCallback(
        (protocol: IssueListVO) => {
            const canWrite = isIssueAdmin || writableIssueListIds.includes(protocol.id);
            return protocol.open && canWrite;
        },
        [isIssueAdmin, writableIssueListIds]
    );
}

function getChildNode(parentNode: Node, nodeId: number) {
    const childNodes = parentNode.childNodes as SubNode[] | undefined;
    return childNodes?.find((childNode) => childNode.id === nodeId);
}

function getActiveFund(arrRegion: AppState['arrRegion']) {
    return arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : undefined;
}

/** Node tab shown in the given fund; none while another fund is active. */
function selectActiveParent(arrRegion: AppState['arrRegion'], fundId: number | undefined) {
    const activeFund = getActiveFund(arrRegion);
    const isFundActive = fundId != null && activeFund?.id === fundId;
    if (!activeFund || !isFundActive) {
        return undefined;
    }
    const { activeIndex, nodes } = activeFund.nodes;
    return activeIndex != null ? nodes[activeIndex] : undefined;
}

/** Unit of description currently selected in the arrangement of the given fund (and its closed version). */
function useActiveNode(fundId: number | undefined, versionId: number | undefined): TargetNode | undefined {
    const intl = useIntl();
    const activeParent = useAppSelector(({ arrRegion }) => selectActiveParent(arrRegion, fundId));
    const nodeId = activeParent?.selectedSubNodeId;
    if (fundId == null || !activeParent || nodeId == null) {
        return undefined;
    }
    const childNode = getChildNode(activeParent, nodeId);
    // the node list shows accordionLeft as the title of a unit, with the same fallback
    const name =
        childNode?.accordionLeft || intl.formatMessage(nodeMessages.accordionTitleLeftNameUndefined, { 0: nodeId });
    return { fundId, versionId, id: nodeId, referenceMark: childNode?.referenceMark, name };
}

/** Reference mark and the name of a unit; the name opens the unit in the arrangement. */
function TargetNodeLink({ node }: { node: TargetNode }) {
    const styles = useStyles();
    const dispatch = useAppThunkDispatch();

    function showNode() {
        dispatch(routerNavigate(urlFundNode(node.fundId, node.versionId, node.id)));
    }

    return (
        <>
            <Text className={styles.referenceMark}>{createReferenceMarkStringFromArray(node.referenceMark ?? [])}</Text>
            <Link className={styles.nodeName} title={node.name} onClick={showNode}>
                {node.name}
            </Link>
        </>
    );
}

interface Props {
    initialTarget: IssueTarget;
    initialValues: IssueFormValues;
    onClose: () => void;
}

/**
 * Non-modal window for adding a lecturing comment; the arrangement stays usable while it is open.
 * The comment follows the fund open on screen and, for a unit of description, the unit selected in
 * the arrangement, until the user locks the unit.
 */
export function IssueCreateWindow({ initialTarget, initialValues, onClose }: Props) {
    const intl = useIntl();
    const styles = useStyles();
    const dispatch = useAppThunkDispatch();
    const [target, setTarget] = useState(initialTarget);
    const [lockedNode, setLockedNode] = useState<TargetNode>();
    const { fundId: openFundId, versionId: openVersionId } = useOpenFund();
    const activeNode = useActiveNode(openFundId, openVersionId);
    const fundProtocols = useFundProtocols(openFundId);
    const canAddToProtocol = useCanAddToProtocol(openFundId);
    const targetLabelId = useId('issue-target');
    const contentRef = useRef<HTMLDivElement>(null);
    const [revealRequest, setRevealRequest] = useState(0);

    // a repeated "add comment" (e.g. for a unit while the window is set to the fund) comes here
    useEffect(() => {
        function reveal(requestedTarget: IssueTarget) {
            setTarget(requestedTarget);
            setRevealRequest((request) => request + 1);
        }
        return subscribeIssueCreateWindowReveal(reveal);
    }, []);

    const isNodeTarget = target === 'node';
    const isLocked = lockedNode != null;
    const targetNode = lockedNode ?? activeNode;
    const isTargetNodeMissing = isNodeTarget && targetNode == null;
    const canLock = isLocked || activeNode != null;
    // until the protocols of the fund load, the form cannot be submitted anyway
    const hasOpenProtocol = fundProtocols?.some(({ open }) => open) ?? true;
    const hasAddableProtocol = fundProtocols?.some(canAddToProtocol) ?? true;
    const blockReason = getBlockReason({
        openFundId,
        lockedFundId: isNodeTarget ? lockedNode?.fundId : undefined,
        hasOpenProtocol,
        canCreateIssue: hasAddableProtocol,
    });
    const isBlocked = blockReason != null;

    // the form stays mounted under the overlay (typed text and the lock survive), but must not be reachable
    useEffect(() => {
        contentRef.current?.toggleAttribute('inert', isBlocked);
    }, [isBlocked]);

    function selectFundTarget() {
        setTarget('fund');
    }

    function selectNodeTarget() {
        setTarget('node');
    }

    function toggleLock() {
        setLockedNode(isLocked ? undefined : activeNode);
    }

    function unlock() {
        setLockedNode(undefined);
    }

    function addIssue(values: IssueFormValues): Promise<IssueVO> {
        const nodeId = isNodeTarget ? targetNode?.id : undefined;
        return WebApi.addIssue({ ...values, nodeId });
    }

    function handleIssueAdded(issue: IssueVO, { addAnother }: IssueSubmitResult) {
        dispatch(issuesActions.list.invalidate(issue.issueListId));
        dispatch(issuesActions.detail.invalidate(issue.id));
        if (!addAnother) {
            onClose();
        }
    }

    const title = intl.formatMessage(isNodeTarget ? daoMessages.issuesAddNodeTitle : daoMessages.issuesAddArrTitle);
    const lockLabel = intl.formatMessage(isLocked ? messages.unlockNode : messages.lockNode);
    const isLockedElsewhere = blockReason === 'lockedNodeElsewhere';

    return (
        <CollapsibleDragWindow
            title={title}
            onClose={onClose}
            initialWidth={500}
            initialHeight={480}
            autoHeight
            revealRequest={revealRequest}
        >
            <div className={styles.content}>
                <div ref={contentRef} className={styles.content}>
                    <div className={styles.target}>
                        <div className={styles.targetSelection}>
                            <Label id={targetLabelId}>{intl.formatMessage(messages.target)}</Label>
                            <div role="group" aria-labelledby={targetLabelId} className={styles.targetButtons}>
                                <ToggleButton
                                    checked={!isNodeTarget}
                                    appearance={isNodeTarget ? 'secondary' : 'primary'}
                                    onClick={selectFundTarget}
                                >
                                    {intl.formatMessage(daoMessages.issuesAddArr)}
                                </ToggleButton>
                                <ToggleButton
                                    checked={isNodeTarget}
                                    appearance={isNodeTarget ? 'primary' : 'secondary'}
                                    onClick={selectNodeTarget}
                                >
                                    {intl.formatMessage(messages.targetNode)}
                                </ToggleButton>
                            </div>
                        </div>
                        {isNodeTarget && (
                            <div className={styles.nodeRow}>
                                <Tooltip content={lockLabel} relationship="label">
                                    <ToggleButton
                                        appearance="subtle"
                                        checked={isLocked}
                                        disabled={!canLock}
                                        icon={isLocked ? <LockClosedRegular /> : <LockOpenRegular />}
                                        onClick={toggleLock}
                                    />
                                </Tooltip>
                                {targetNode ? (
                                    <TargetNodeLink node={targetNode} />
                                ) : (
                                    <Text>{intl.formatMessage(messages.noActiveNode)}</Text>
                                )}
                            </div>
                        )}
                    </div>
                    <IssueForm
                        className={styles.form}
                        initialValues={initialValues}
                        onSubmit={addIssue}
                        onSubmitSuccess={handleIssueAdded}
                        submitDisabled={isTargetNodeMissing}
                        canAddAnother
                        fundId={openFundId}
                        canAddToProtocol={canAddToProtocol}
                        autoFocus
                    />
                </div>
                {/* always rendered: a live region announces reliably only when it exists before its text */}
                <div role="status" className={blockReason ? styles.overlay : undefined}>
                    {blockReason && (
                        <>
                            <Text size={400} align="center">
                                {intl.formatMessage(blockMessages[blockReason])}
                            </Text>
                            {isLockedElsewhere && lockedNode && (
                                <>
                                    <div className={styles.nodeRow}>
                                        <TargetNodeLink node={lockedNode} />
                                    </div>
                                    <Button icon={<LockOpenRegular />} onClick={unlock}>
                                        {intl.formatMessage(messages.unlockNode)}
                                    </Button>
                                </>
                            )}
                        </>
                    )}
                </div>
            </div>
        </CollapsibleDragWindow>
    );
}

export type IssueCreateWindowProps = Props;
