import { Modal } from 'react-bootstrap';
import { useIntl } from 'react-intl';
import { DaAipActionVO } from 'elza-api';

import { Button } from '../ui';
import { AipActionResult } from './AipActionResult';
import { useAipAction } from './useAipAction';
import { actionMessages } from './messages';

interface Props {
    /** Akce vrácená serverem při jejím vyžádání; dál se aktualizuje přes websocket. */
    initialAction: DaAipActionVO;
    onClose?: () => void;
}

/**
 * Průběh a výsledek akce nad AIPy.
 *
 * Dialog zůstává otevřený i po odeslání požadavku: akce se provádí na pozadí, po jednotlivých
 * AIPech, a teprve tady se uživatel dozví, co se s kterým AIPem stalo. Zavřít ho lze kdykoli -
 * dokončení akce pak oznámí ten, kdo ji spustil (viz runAipAction).
 */
export function AipActionProgressDialog({ initialAction, onClose }: Props) {
    const intl = useIntl();
    const { action } = useAipAction(initialAction);

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
