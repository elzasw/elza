import './AipDetail.scss';
import { Dismiss24Regular, ArrowDownload20Filled, FolderOpen20Filled } from "@fluentui/react-icons";
import { DrawerBody, DrawerHeader, DrawerHeaderTitle, Button, InlineDrawer } from '@fluentui/react-components';
import { useEffect } from 'react';
import { useIntl } from 'react-intl';
import { useSelector } from 'react-redux';
import { useHistory } from 'react-router';
import { storeFromArea } from 'shared/utils';
import { AppState } from 'typings/store';
import { useThunkDispatch } from 'utils/hooks';
import { globalMessages } from 'components/shared/lang/messages';
import * as aipActions from '../../actions/aip/aip';
import { urlAip, urlAipExplorer } from '../../constants';
import { AipDetailBody } from './AipDetailBody';
import { detailMessages } from './messages';
import { packageDownloadUrl } from './explorer/packageUrls';

interface Props {
    open: boolean;
    onClose: () => void;
}

export function AipDetail({ open, onClose }: Props) {
    const aip = useSelector((state: AppState) => storeFromArea(state, aipActions.AREA_AIP));
    const dispatch = useThunkDispatch();
    const history = useHistory();
    const { formatMessage } = useIntl();

    useEffect(() => {
        dispatch(aipActions.aipFetchIfNeeded(aip.id));
    }, [dispatch, aip.id]);

    const handleClose = () => {
        dispatch(aipActions.selectAip(null));
        onClose();
        history.replace(urlAip());
    };

    /**
     * Průzkumník je samostatná stránka, takže je dostupný i u AIPu, jehož zpracování
     * selhalo - záložka Balíček ukáže stažený balíček tak, jak přišel.
     */
    const handleOpenExplorer = () => {
        history.push(urlAipExplorer(aip.id));
    };

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
                            aria-label={formatMessage(globalMessages.close)}
                            icon={<Dismiss24Regular />}
                            onClick={handleClose}
                        />
                    }
                >
                    {formatMessage(detailMessages.title)}
                </DrawerHeaderTitle>
            </DrawerHeader>
            <DrawerBody>
                {aip.isFetching && <span>{formatMessage(detailMessages.loading)}</span>}
                {aip.data && (
                    <div className='detail-body'>
                        {/* Both need a package on disk; the load flag says whether there is one. */}
                        <Button
                            as="a"
                            className="open-btn"
                            onClick={handleOpenExplorer}
                            disabled={!aip.data.metadataLoad}
                        >
                            <FolderOpen20Filled />
                            <span>{formatMessage(detailMessages.explorerOpen)}</span>
                        </Button>
                        {/* Stažení balíčku nezávisí na zpracování - u balíčku, který ELZA
                            zpracovat nedokáže, je to cesta, jak si ho prohlédnout jinde. */}
                        {aip.data.metadataLoad && (
                            <Button
                                as="a"
                                className="open-btn"
                                href={packageDownloadUrl(aip.data.aipId)}
                                download
                            >
                                <ArrowDownload20Filled />
                                <span>{formatMessage(detailMessages.downloadPackage)}</span>
                            </Button>
                        )}
                        <AipDetailBody detail={aip.data} />
                    </div>
                )}
            </DrawerBody>
        </InlineDrawer>
    );
}

export type AipDetailProps = Props;

export default AipDetail;
