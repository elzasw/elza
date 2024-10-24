
import './AipDetail.scss';
import { Dismiss24Regular } from "@fluentui/react-icons";
import { DrawerBody, DrawerHeader, DrawerHeaderTitle, InlineDrawer, Button, OverlayDrawer } from '@fluentui/react-components';
import { formatAipSize, formatDate, getBoolIcon } from './utils';
import { useEffect } from 'react';
import { useSelector } from 'react-redux';
import { storeFromArea } from 'shared/utils';
import { AppState } from 'typings/store';
import { useThunkDispatch } from 'utils/hooks';
import * as aipActions from '../../actions/aip/aip';
import { useHistory } from 'react-router';
import { urlAip } from '../../constants';

import { FolderOpen20Filled } from '@fluentui/react-icons';
import i18n from 'components/i18n';
import { modalDialogHide, modalDialogShow } from 'actions/global/modalDialog';
import AipExplorerModalWrapper from './explorer/AipExplorerWrapper';
import { ExplorerMode } from './explorer/ExplorerContext';
import AipDetailBody from './AipDetailBody';

const AipDetail = () => {
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
        history.replace(urlAip());
    }

    const handleOpenExplorer = () => {
        dispatch(
            modalDialogShow(
                this,
                "AIP Průzkumník",
                <AipExplorerModalWrapper
                //@ts-ignore
                    onOk={() => dispatch(modalDialogHide())}
                    onClose={() => dispatch(modalDialogHide())}
                    mode={ExplorerMode.VIEW}
                />,
                "aip-explorer"
            ),
        );
    }
    return (
        <OverlayDrawer
            position="end"
            open={aip.id != null && aip.data != null}
            style={{ width: "400px" }}
            className='aip-detail'
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
                            disabled={!aip.data.metadataLoad || aip.data.metadataError}
                        >
                            <FolderOpen20Filled/>
                            <span>{i18n("aip.detail.explorer.open")}</span>
                        </Button>
                        <AipDetailBody detail={aip.data} />
                    </div>
                </>}
            </DrawerBody>
        </OverlayDrawer>
    );
}

export default AipDetail;
