import { useEffect, useRef } from 'react';
import { Modal } from 'react-bootstrap';
import { useIntl } from 'react-intl';
import { DaAipActionItemState, DaAipActionVO } from 'elza-api';

import { Button } from '../ui';
import { AipActionResult } from './AipActionResult';
import { useAipAction } from './useAipAction';
import { actionMessages } from './messages';

interface Props {
    /** Akce vrácená serverem při jejím vyžádání; dál se aktualizuje přes websocket. */
    initialAction: DaAipActionVO;
    /** Zavolá se po dokončení akce, aby se překreslil seznam AIPů. */
    onFinished?: () => void;
    /** Oznámení dokončení uživateli, který akci vyvolal. */
    onNotify?: (errors: number, total: number) => void;
    onClose?: () => void;
}

/**
 * Průběh a výsledek akce nad AIPy.
 *
 * Dialog zůstává otevřený i po odeslání požadavku: akce se provádí na pozadí, po jednotlivých
 * AIPech, a teprve tady se uživatel dozví, co se s kterým AIPem stalo. Zavřít ho lze kdykoli -
 * dokončení akce se oznámí zvlášť.
 */
export function AipActionProgressDialog({ initialAction, onFinished, onNotify, onClose }: Props) {
    const intl = useIntl();
    const { action, finished } = useAipAction(initialAction);
    const alreadyNotified = useRef(false);

    useEffect(() => {
        if (!finished || !action || alreadyNotified.current) {
            return;
        }
        alreadyNotified.current = true;
        const items = action.items ?? [];
        onNotify?.(items.filter(item => item.state === DaAipActionItemState.Error).length, items.length);
        onFinished?.();
    }, [finished, action, onFinished, onNotify]);

    return (
        <>
            <Modal.Body>
                {action && <AipActionResult action={action} />}
            </Modal.Body>
            <Modal.Footer>
                <Button onClick={onClose} variant="outline-secondary">
                    {intl.formatMessage(actionMessages.close)}
                </Button>
            </Modal.Footer>
        </>
    );
}

export type AipActionProgressDialogProps = Props;

export default AipActionProgressDialog;
