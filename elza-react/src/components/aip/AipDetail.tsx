
import './AipDetail.scss';
import { Dismiss24Regular } from "@fluentui/react-icons";
import { DrawerBody, DrawerHeader, DrawerHeaderTitle, Button, InlineDrawer } from '@fluentui/react-components';
import { FC, useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { storeFromArea } from 'shared/utils';
import { AppState } from 'typings/store';
import { useThunkDispatch } from 'utils/hooks';
import * as aipActions from '../../actions/aip/aip';
import { useHistory } from 'react-router';
import { urlAip, urlAipExplorer } from '../../constants';

import { FolderOpen20Filled } from '@fluentui/react-icons';
import i18n from 'components/i18n';
import AipDetailBody from './AipDetailBody';

interface Props {
    open: boolean;
    onClose: () => void;
    onOpen: () => void;
}

const AipDetail: FC<Props> = ({open, onClose, onOpen}) => {
    const aip = useSelector((state: AppState) => storeFromArea(state, aipActions.AREA_AIP));
    const dispatch = useThunkDispatch();
    const history = useHistory();

    const fetchData = () => {
        dispatch(aipActions.aipFetchIfNeeded(aip.id));
    }

    useEffect(() => {
        fetchData()
    }, [aip.id])

    const handleClose = () => {
        dispatch(aipActions.selectAip(null));
        onClose();
        history.replace(urlAip());
    }

    /**
     * Průzkumník je samostatná stránka, takže je dostupný i u AIPu, jehož zpracování
     * selhalo - záložka Balíček ukáže stažený balíček tak, jak přišel.
     */
    const handleOpenExplorer = () => {
        history.push(urlAipExplorer(aip.id));
    }

    return (
        <InlineDrawer
            position="end"
            separator
            style={{ width: "400px" }}
            className='aip-detail'
            open={open}
        >
            <DrawerHeader>
                <DrawerHeaderTitle
                    action={
                        <Button
                            appearance="subtle"
                            aria-label="Close"
                            icon={<Dismiss24Regular />}
                            onClick={handleClose}
                        />
                    }
                >
                      {i18n("aip.detail.title")}
                </DrawerHeaderTitle>
            </DrawerHeader>
            <DrawerBody>
                {aip.isFetching && <span>Načítání...</span>}
                {aip.data && <>
                    <div className='detail-body'>
                        {/* CompoundButton is way too big */}
                        <Button
                            as="a"
                            className="open-btn"
                            onClick={handleOpenExplorer}
                        >
                            <FolderOpen20Filled/>
                            <span>{i18n("aip.detail.explorer.open")}</span>
                        </Button>
                        <AipDetailBody detail={aip.data} />
                    </div>
                </>}
            </DrawerBody>
        </InlineDrawer>
    );
}

export default AipDetail;
