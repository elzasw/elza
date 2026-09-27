import { useEffect, useState } from 'react';
import { Tab, TabList, TabListProps } from '@fluentui/react-components';
import { FormattedMessage } from 'react-intl';

import { AREA_AIP, aipFetchIfNeeded, selectAip } from 'actions/aip/aip';
import { storeFromArea } from 'shared/utils';
import { AppState } from 'typings/store';
import { useAppSelector, useThunkDispatch } from 'utils/hooks';
import AipExplorer from './AipExplorer';
import { AipOverview } from './AipOverview';
import { ExplorerMode } from './ExplorerContext';
import PackageBrowser from './PackageBrowser';
import AipConnectPanel from 'components/arr/aip/assignment/AipConnectPanel';
import { detailMessages, explorerPageMessages } from '../messages';
import './AipExplorerTabs.scss';

export type TabKey = 'package' | 'structure' | 'files' | 'connect';

interface Props {
    aipId: number;
    /** Nabídnout kartu připojení k popisu - jen v archivním souboru, do kterého uživatel smí zapisovat. */
    connectable?: boolean;
    /** Karta otevřená při zobrazení; karta připojení jen když ji lze nabídnout. */
    initialTab?: TabKey;
}

/**
 * The AIP explorer: the package as a whole first, then the structure ELZA made of it
 * (representations, logical structure, metadata), then the files as they arrived from the
 * digital archive - the last one works even when processing failed, which is when it is needed.
 * Inside a fund the user may also connect the package, or its parts, to the archival description.
 */
export function AipExplorerTabs({aipId, connectable = false, initialTab = 'package'}: Props) {
    const dispatch = useThunkDispatch();
    const aip = useAppSelector((state: AppState) => storeFromArea(state, AREA_AIP));
    const [chosenTab, setTab] = useState<TabKey>(initialTab);
    // the fund is reported closed while it loads, so the connect tab may become available later
    const tab: TabKey = chosenTab === 'connect' && !connectable ? 'package' : chosenTab;
    const [fileToOpen, setFileToOpen] = useState<string | undefined>();

    useEffect(() => {
        dispatch(selectAip(aipId));
        dispatch(aipFetchIfNeeded(aipId));
        // a file to open belongs to the package it was chosen in
        setFileToOpen(undefined);
    }, [aipId]);

    const handleTabSelect: TabListProps['onTabSelect'] = (_event, data) => setTab(data.value as TabKey);

    const openFile = (file: string) => {
        setFileToOpen(file);
        setTab('files');
    };

    return (
        <div className="aip-explorer-tabs-container">
            <TabList selectedValue={tab} onTabSelect={handleTabSelect} className="aip-explorer-tabs">
                <Tab value="package"><FormattedMessage {...explorerPageMessages.packageTab}/></Tab>
                <Tab value="structure"><FormattedMessage {...explorerPageMessages.structureTab}/></Tab>
                <Tab value="files"><FormattedMessage {...explorerPageMessages.filesTab}/></Tab>
                {connectable && <Tab value="connect"><FormattedMessage {...explorerPageMessages.connectTab}/></Tab>}
            </TabList>
            <div className="aip-explorer-tabs-panel">
                {tab === 'package' && (aip.data
                    ? <AipOverview detail={aip.data} onOpenProblemFile={openFile}/>
                    : <p className="aip-explorer-tabs-loading"><FormattedMessage {...detailMessages.loading}/></p>)}
                {tab === 'structure' && <AipExplorer mode={ExplorerMode.VIEW} hideRoot/>}
                {tab === 'files' && <PackageBrowser aipId={aipId}
                                                    problemType={aip.data?.problemType}
                                                    problemDescription={aip.data?.problemDescription}
                                                    problemFile={aip.data?.problemFile}
                                                    selectPath={fileToOpen}/>}
                {tab === 'connect' && <AipConnectPanel aipId={aipId}/>}
            </div>
        </div>
    );
}

export type AipExplorerTabsProps = Props;

export default AipExplorerTabs;
