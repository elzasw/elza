import { useRef } from 'react';
import { Modal } from 'react-bootstrap';
import { FormattedMessage, useIntl } from 'react-intl';
import { FormApi } from 'final-form';
import { Api } from 'api';
import { globalMessages } from 'components/shared/lang/messages';
import { modalDialogHide } from 'actions/global/modalDialog';
import { useAppThunkDispatch } from 'utils/hooks';
import { Button } from '../ui';
import { fundFormMessages } from './fundFormMessages';
import { PersistentSortFields, PersistentSortForm, PersistentSortSettings } from './PersistentSortForm';

interface Props {
    versionId: number;
    /** Rule set of the fund, whose item types are offered. */
    ruleSetId: number;
    /** Unit whose children are sorted. */
    node: { id: number; name: string };
    onClose?: () => void;
}

export type PersistentSortDialogProps = Props;

/**
 * Persistent sort of the children of one unit, from the context menu of the tree. The server runs
 * the persistent sort of the rule set of the fund, whatever it is called.
 */
export function PersistentSortDialog({ versionId, ruleSetId, node, onClose }: Props) {
    const intl = useIntl();
    const dispatch = useAppThunkDispatch();
    const formApi = useRef<FormApi<PersistentSortFields> | null>(null);

    function handleSubmit(settings: PersistentSortSettings) {
        dispatch(modalDialogHide());
        return Api.funds.bulkActionQueuePersistentSort(versionId, { ...settings, nodeIds: [node.id] });
    }

    return (
        <div>
            <Modal.Body>
                <span>
                    <label>{intl.formatMessage(fundFormMessages.historyTitleNodeChanges) + ':'} &nbsp;</label>
                    {node.name}
                </span>
                <PersistentSortForm ruleSetId={ruleSetId} onSubmit={handleSubmit} formApiRef={formApi} />
            </Modal.Body>
            <Modal.Footer>
                <Button type="submit" variant="outline-secondary" onClick={() => formApi.current?.submit()}>
                    <FormattedMessage {...globalMessages.run} />
                </Button>
                <Button variant="link" onClick={onClose}>
                    <FormattedMessage {...globalMessages.cancel} />
                </Button>
            </Modal.Footer>
        </div>
    );
}
