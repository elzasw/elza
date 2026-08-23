/** Dialog zobrazení digitálních entit připojených k jednotce popisu. */
import { useEffect, useRef, useState } from 'react';
import { Form, Modal } from 'react-bootstrap';
import { defineMessages, useIntl } from 'react-intl';
import { modalDialogHide } from 'actions/global/modalDialog';
import { useAppSelector, useAppThunkDispatch } from 'utils/hooks';
import { ArrDaoVO } from 'typings/dao';
import { Button } from '../ui';
import ArrDaos from './ArrDaos.jsx';

const messages = defineMessages({
    close: {
        id: 'nodeDaos.action.close',
        defaultMessage: 'Zavřít',
    },
});

interface Props {
    nodeId: number;
    readMode: boolean;
    /** DAO, které se má rovnou otevřít na detailu; jinak se detail zobrazí až po výběru. */
    daoId?: number;
    /** Doplňuje ModalDialog — zavírá právě tento dialog. */
    onClose?: () => void;
}

export const NodeDaosForm = ({ nodeId, readMode, daoId, onClose }: Props) => {
    const intl = useIntl();
    const dispatch = useAppThunkDispatch();

    const fund = useAppSelector(({ arrRegion }) =>
        arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : undefined,
    );
    const daoList = fund?.nodeDaoList;

    const [selectedDaoId, setSelectedDaoId] = useState(daoId);
    const [selectedDaoFileId, setSelectedDaoFileId] = useState<number | null>(null);

    const close = () => (onClose ? onClose() : dispatch(modalDialogHide()));

    // Odpojení poslední digitální entity vyprázdní celý obsah dialogu. Prázdné okno
    // uživateli nic neříká, proto ho v takovém případě zavřeme. Rozhoduje se až podle
    // dočteného seznamu, aby dialog nezavřelo probíhající načítání.
    const hadDaos = useRef(false);
    useEffect(() => {
        if (!daoList?.fetched || daoList.isFetching) {
            return;
        }
        if (daoList.rows.length > 0) {
            hadDaos.current = true;
        } else if (hadDaos.current) {
            close();
        }
    }, [daoList]);

    return (
        <Form>
            <Modal.Body>
                <ArrDaos
                    type="NODE"
                    fund={fund}
                    nodeId={nodeId}
                    readMode={readMode}
                    selectedDaoId={selectedDaoId}
                    selectedDaoFileId={selectedDaoFileId}
                    onSelect={(dao: ArrDaoVO, fileId: number | null) => {
                        setSelectedDaoId(dao.id);
                        setSelectedDaoFileId(fileId);
                    }}
                />
            </Modal.Body>
            <Modal.Footer>
                <Button variant="link" onClick={close}>
                    {intl.formatMessage(messages.close)}
                </Button>
            </Modal.Footer>
        </Form>
    );
};

export default NodeDaosForm;
