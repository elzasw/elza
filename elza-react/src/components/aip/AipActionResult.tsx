import { useIntl } from 'react-intl';
import { DaAipActionItemState, DaAipActionState, DaAipActionVO } from 'elza-api';

import { Icon } from 'components/shared';
import { actionItemStateMessages, actionMessages } from './messages';
import './AipActionResult.scss';

interface Props {
    action: DaAipActionVO;
}

const stateGlyph = (state: DaAipActionItemState) => {
    switch (state) {
        case DaAipActionItemState.Finished:
            return 'fa-check';
        case DaAipActionItemState.Error:
            return 'fa-exclamation-triangle';
        case DaAipActionItemState.Skipped:
            return 'fa-minus-circle';
        case DaAipActionItemState.Running:
            return 'fa-spinner';
        default:
            return 'fa-clock-o';
    }
};

/**
 * Výsledek akce po jednotlivých AIPech.
 *
 * Zobrazuje se po AIPech, ne jako jeden ukazatel průběhu: akce je nad každým AIPem samostatná a
 * jediné číslo by neřeklo, co se s kterým AIPem stalo. Právě u AIPu, se kterým akce nic neudělala,
 * je podstatný důvod.
 */
export function AipActionResult({ action }: Props) {
    const intl = useIntl();
    const items = action.items ?? [];
    const done = items.filter(item => item.state !== DaAipActionItemState.Waiting
                                   && item.state !== DaAipActionItemState.Running).length;
    const errors = items.filter(item => item.state === DaAipActionItemState.Error).length;

    return (
        <div className="aip-action-result">
            <div className="aip-action-summary">
                {action.state === DaAipActionState.Waiting || action.state === DaAipActionState.Running
                    ? <Icon glyph="fa-spinner" className="fa-spin" />
                    : <Icon glyph={errors > 0 ? 'fa-exclamation-triangle' : 'fa-check'} />}
                <span>{intl.formatMessage(actionMessages.summary, { done, total: items.length, errors })}</span>
            </div>

            <table className="aip-action-items">
                <thead>
                    <tr>
                        <th>{intl.formatMessage(actionMessages.aipColumn)}</th>
                        <th>{intl.formatMessage(actionMessages.stateColumn)}</th>
                    </tr>
                </thead>
                <tbody>
                    {items.map(item => (
                        <tr key={item.aipId} className={`item-${item.state.toLowerCase()}`}>
                            <td>{item.aipCode ?? item.aipId}</td>
                            <td>
                                <span className="item-state">
                                    <Icon glyph={stateGlyph(item.state)}
                                          className={item.state === DaAipActionItemState.Running ? 'fa-spin' : undefined} />
                                    {intl.formatMessage(actionItemStateMessages[item.state])}
                                </span>
                                {item.message && <div className="item-message">{item.message}</div>}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}

export type AipActionResultProps = Props;

export default AipActionResult;
