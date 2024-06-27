
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

const AipDetail = () => {
    const aip = useSelector((state: AppState) => storeFromArea(state, aipActions.AREA_AIP))
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

    const DetailRow = ({label, value}: {label: string, value?: any}) => (
        <div className="item-row">
            <div className="label col">
                <b>{label}</b>
            </div>
            {value && <div className="value col">
                {value}
            </div>
            }
        </div>
    );

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
            <DrawerBody >
                {aip.isFetching && <span>Načítání...</span>}
                {aip.data && <>
                    <div className='detail-body'>
                        {/* CompoundButton is way too big */}
                        <Button
                            as="a"
                            className="open-btn"
                            // href="" TODO: add link
                        >
                            <FolderOpen20Filled/>
                            <span>Otevřít průzkumník</span>
                        </Button>

                        {aip.data.aipId && 
                            <DetailRow label="Id" value={aip.data.aipId.toString()}/>}
                        {aip.data.code && 
                            <DetailRow label="Kód aipu" value={aip.data.code.toString()}/>}
                        {aip.data.aipVersion && 
                            <DetailRow label="Verze" value={aip.data.aipVersion}/>}
                        {aip.data.fundName && 
                            <DetailRow label="Archivní soubor" value={
                                <a href=''>{aip.data.fundName}</a>
                            }/>
                        }
                        {aip.data.instApName && 
                            <DetailRow label="Instituce" value={
                                <a href=''>{aip.data.instApName}</a>
                            }/>
                        }
                        {aip.data.unitdateFrom && 
                            <DetailRow label="Dotace od-do" value={
                                formatDate(new Date(aip.data.unitdateFrom)) + " - " + formatDate(new Date(aip.data.unitdateTo))
                            }/>}
                        {aip.data.originApName && 
                            <DetailRow label="Původce" value={
                                <a href=''>{aip.data.originApName}</a>
                            }/>
                        }
                        {aip.data.stateingestionCode && 
                            <DetailRow label="Číslo přejímky" value={aip.data.ingestionCode}/>}
                        {aip.data.referenceNumber && 
                            <DetailRow label="Číslo jednací" value={aip.data.referenceNumber}/>}
                        {aip.data.nadChangeCode && 
                            <DetailRow label="Vnější změna" value={aip.data.nadChangeCode}/>}
                        {aip.data.aipSize && 
                            <DetailRow label="Velikost" value={formatAipSize(aip.data.aipSize)}/>}
                        {aip.data != null && 
                            <DetailRow label="Načtena metadata" value={getBoolIcon(aip.data.createDaoStructure)}/>}
                        {/* TODO: @kasparova Podle zadání bude upřesněno v budoucnu
                        {aip.data.componentsLoaded != null && <DetailRow label="Stažené komponenty" value={getBoolIcon(aip.data.componentsLoaded)}/>}
                        {aip.data.linkedArchiveDesc != null && <DetailRow label="Napojen archivní popis" value={getBoolIcon(aip.data.linkedArchiveDesc)}/>} */}
                        {aip.data.state && 
                            <DetailRow label="Aktuální verze" value={aip.data.state}/>}
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
