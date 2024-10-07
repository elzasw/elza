
import './AipDetail.scss';
import { Dismiss24Regular } from "@fluentui/react-icons";
import { DrawerBody, DrawerHeader, DrawerHeaderTitle, InlineDrawer, Button } from '@fluentui/react-components';
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
        <InlineDrawer
            separator
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
                      <h2>Detail AIP</h2>
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

                    {/* <h2>Chyba</h2>
                    <DetailRow label="Technický popis"/>
                        <div className='br'>
                            ERRR
                        </div>
                    <DetailRow label="Podrobný popis"/>
                    <div className='br'>
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer iaculis nunc at scelerisque pretium. Proin quis nisi leo. Maecenas gravida orci a turpis blandit efficitur. Pellentesque porta purus mauris, et semper metus mollis sit amet. Curabitur varius et augue non tincidunt. Vivamus accumsan mollis odio non imperdiet. In ut urna quam.
                    </div> */}
                </>}
            </DrawerBody>
        </InlineDrawer>
    );
}

export default AipDetail;
