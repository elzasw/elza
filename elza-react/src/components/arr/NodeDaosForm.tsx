/** Okno s digitálními entitami připojenými k jednotce popisu. */
import { useEffect, useRef, useState } from 'react';
import { Button, makeStyles, tokens } from '@fluentui/react-components';
import { defineMessages, useIntl } from 'react-intl';
import { CollapsibleDragWindow } from 'components/shared/dialog/FluentModalDialog';
import { globalMessages } from 'components/shared/lang/messages';
import { useAppSelector } from 'utils/hooks';
import { ArrDaoVO } from 'typings/dao';
import { Fund } from 'typings/store';
import { ArrDaos } from './ArrDaos';

const messages = defineMessages({
    title: {
        id: 'nodeDaos.title',
        defaultMessage: 'Digitální entity pro jednotku popisu',
    },
});

const useStyles = makeStyles({
    content: {
        display: 'flex',
        flexGrow: 1,
        // Bez minHeight by obsah okno roztahoval, místo aby se do něj vešel.
        minHeight: 0,
        paddingTop: tokens.spacingVerticalS,
    },
    footer: {
        display: 'flex',
        flexShrink: 0,
        justifyContent: 'flex-end',
        paddingTop: tokens.spacingVerticalS,
    },
});

interface Props {
    nodeId: number;
    readMode: boolean;
    /** Zavření okna — doplňuje useNodeDaosModal. */
    onClose: () => void;
    /** DAO, které se má rovnou otevřít na detailu; jinak se vybere první. */
    daoId?: number;
}

export type NodeDaosFormProps = Props;

export function NodeDaosForm({ nodeId, readMode, onClose, daoId }: Props) {
    const intl = useIntl();
    const styles = useStyles();

    const fund = useAppSelector(({ arrRegion }) =>
        arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : undefined,
    );
    const daoList = fund?.nodeDaoList;

    const [selectedDaoId, setSelectedDaoId] = useState(daoId);
    const [selectedDaoFileId, setSelectedDaoFileId] = useState<number | null>(null);

    // Odpojení poslední digitální entity vyprázdní celý obsah okna. Prázdné okno
    // uživateli nic neříká, proto ho v takovém případě zavřeme. Rozhoduje se až podle
    // dočteného seznamu, aby okno nezavřelo probíhající načítání.
    const hadDaos = useRef(false);
    useEffect(() => {
        if (!daoList?.fetched || daoList.isFetching) {
            return;
        }
        if (daoList.rows.length > 0) {
            hadDaos.current = true;
        } else if (hadDaos.current) {
            onClose();
        }
    }, [daoList, onClose]);

    return (
        <CollapsibleDragWindow
            title={intl.formatMessage(messages.title)}
            onClose={onClose}
            initialWidth={1100}
            initialHeight={700}
        >
            <div className={styles.content}>
                <ArrDaos
                    type="NODE"
                    fund={fund as Fund}
                    nodeId={nodeId}
                    readMode={readMode}
                    selectedDaoId={selectedDaoId}
                    selectedDaoFileId={selectedDaoFileId}
                    autoSelectFirst
                    onSelect={(dao: ArrDaoVO, fileId: number | null) => {
                        setSelectedDaoId(dao.id);
                        setSelectedDaoFileId(fileId);
                    }}
                />
            </div>
            <div className={styles.footer}>
                <Button appearance="subtle" onClick={onClose}>
                    {intl.formatMessage(globalMessages.close)}
                </Button>
            </div>
        </CollapsibleDragWindow>
    );
}
