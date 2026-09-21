import { useIntl } from 'react-intl';
import { Link } from 'react-router-dom';
import { Icon } from 'components/shared';
import { LinkedNodeVO, QueueItemState } from 'elza-api';
import { Button } from "react-bootstrap";
import { urlFundNode } from '../../constants';
import { dateToDateTimeString } from '../../shared/utils/commons';
import { queueStateMessages } from './messages';

export const getBoolIcon = (value?: boolean) => {
    return value ? <Icon glyph="fa-check"/> : <Icon glyph="fa-close"/>;
}

interface LinkedNodeLinkProps {
    fundId: number;
    link: LinkedNodeVO;
}

/**
 * The description unit a link leads to, named and reachable - reading which unit the AIP hangs
 * on is only half the answer, the user goes there from here.
 */
export function LinkedNodeLink({ fundId, link }: LinkedNodeLinkProps) {
    return <Link to={urlFundNode(fundId, undefined, link.nodeId)}>{link.name}</Link>;
}

/**
 * Whether the AIP hangs on the archival description, followed by the units it is attached to.
 * Links of the parts of a package are listed in a row of their own, so they are not named here -
 * but they do attach the package, which is what `linkedByParts` says: without it the flag would
 * deny a link the detail shows one line below.
 */
export const getConnectedToJP = (
    linkedNodes: Array<LinkedNodeVO> | null | undefined,
    fundId: number,
    handleDeleteLink: (linkId: number) => void,
    linkedByParts = false,
) => {
    const links = linkedNodes ?? [];
    const iconString = links.length > 0 || linkedByParts ? "fa-check" : "fa-close";

    const nodes = links.map(item =>
        <div key={item.id}>
            <LinkedNodeLink fundId={fundId} link={item} />
            <Button key="deleteLink" variant="action" onClick={() => handleDeleteLink(item.id)}>
                <Icon glyph="fa fa-close" />
            </Button>
        </div>);

    return <div><Icon glyph={iconString}/> {nodes}</div>;
}

interface QueueStateCellProps {
    state?: QueueItemState;
    /** Why the item ended in its state - shown in the tooltip, as the only account of a failure the user can reach. */
    message?: string;
    date?: string;
}

/**
 * Stav fronty; u chybových stavů s ikonou a důvodem selhání v tooltipu, protože jinak se
 * uživatel důvod nedozví - zůstal by jen v protokolu serveru.
 */
export function QueueStateCell({ state, message, date }: QueueStateCellProps) {
    const { formatMessage } = useIntl();
    if (!state) {
        return <>-</>;
    }
    const label = formatMessage(queueStateMessages[state]);
    const failed = state === QueueItemState.ImportError || state === QueueItemState.ExportError;
    const tooltip = [message, date ? dateToDateTimeString(new Date(date)) : null]
        .filter(Boolean).join("\n") || undefined;
    if (!failed) {
        return <span title={tooltip}>{label}</span>;
    }
    return (
        <span className="aip-problem" title={tooltip}>
            <Icon glyph="fa-exclamation-triangle"/>
            {label}
        </span>
    );
}

export type { LinkedNodeLinkProps, QueueStateCellProps };
