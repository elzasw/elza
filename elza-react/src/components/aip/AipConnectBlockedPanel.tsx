import { useState } from 'react';
import { useIntl } from 'react-intl';
import { AipConnectBlockedVO } from 'elza-api';

import { Icon } from 'components/shared';
import { connectCheckMessages } from './messages';
import './AipConnectBlockedPanel.scss';

interface Props {
    blocked: AipConnectBlockedVO[];
}

/**
 * Co brání napojení, ještě než ho uživatel potvrdí.
 *
 * Napojení takový AIP odmítne, takže by se to jinak uživatel dozvěděl až po stisku tlačítka - a u
 * výběru padesáti AIPů by nevěděl, kterého se to týká.
 */
export function AipConnectBlockedPanel({ blocked }: Props) {
    const intl = useIntl();
    const [expanded, setExpanded] = useState(false);

    if (blocked.length === 0) {
        return null;
    }

    return (
        <div className="aip-connect-blocked">
            <div className="blocked-summary">
                <Icon glyph="fa-exclamation-triangle" />
                <span>{intl.formatMessage(connectCheckMessages.blocked, { count: blocked.length })}</span>
                <button type="button" className="link-like" onClick={() => setExpanded(!expanded)}>
                    {intl.formatMessage(expanded ? connectCheckMessages.hideDetail
                                                 : connectCheckMessages.showDetail)}
                </button>
            </div>
            {expanded && (
                <ul className="blocked-list">
                    {blocked.map(item => (
                        <li key={item.aipId}>
                            <span className="blocked-aip">{item.aipCode ?? item.aipId}</span>
                            <span className="blocked-reason">{item.reason}</span>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
}

export type AipConnectBlockedPanelProps = Props;

export default AipConnectBlockedPanel;
