import { useIntl } from 'react-intl';
import { Icon } from 'components/shared';
import { LinkedNodeVO, QueueItemState } from 'elza-api';
import { Button } from "react-bootstrap";
import { serverContextPath } from "../../api";
import { dateToDateTimeString } from '../../shared/utils/commons';
import { queueStateMessages } from './messages';

export const getBoolIcon = (value?: boolean) => {
    return value ? <Icon glyph="fa-check"/> : <Icon glyph="fa-close"/>;
}

export const getConnectedToJP = (
    linkedNodes: Array<LinkedNodeVO> | null | undefined,
    fundId: number,
    handleDeleteLink: (linkId: number) => void,
) => {
    let iconString = "fa-close";
    let nodes;

    if (linkedNodes && linkedNodes.length > 0) {
        iconString = "fa-check";

        nodes = linkedNodes.map(item =>
            <div key={item.id}>
                <a href={`${serverContextPath}/fund/${fundId}/node/${item.nodeId}`}>{item.name}</a>
                <Button key="deleteLink" variant="action" onClick={() => handleDeleteLink(item.id)}>
                    <Icon glyph="fa fa-close" />
                </Button>
            </div>)
    }

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

export type { QueueStateCellProps };
