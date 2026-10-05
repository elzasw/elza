import { useEffect, useState } from 'react';
import { Tab, TabList, TabListProps } from '@fluentui/react-components';
import { FormattedMessage } from 'react-intl';
import { useHistory, useLocation } from 'react-router-dom';

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

const TABS: TabKey[] = ['package', 'structure', 'files', 'connect'];

interface Props {
    aipId: number;
    /** Nabídnout kartu připojení k popisu - jen v archivním souboru, do kterého uživatel smí zapisovat. */
    connectable?: boolean;
}

/**
 * The AIP explorer: the package as a whole first, then the structure ELZA made of it
 * (representations, logical structure, metadata), then the files as they arrived from the
 * digital archive - the last one works even when processing failed, which is when it is needed.
 * Inside a fund the user may also connect the package, or its parts, to the archival description.
 *
 * What is shown lives in the address: `tab` is the open tab, `select` the UUID of the part
 * selected in the structure. Any view of an inner part of the package can be linked to, and
 * the address is replaced, not pushed, so browsing the package does not flood the history.
 */
export function AipExplorerTabs({aipId, connectable = false}: Props) {
    const dispatch = useThunkDispatch();
    const history = useHistory();
    const location = useLocation();
    const aip = useAppSelector((state: AppState) => storeFromArea(state, AREA_AIP));
    const query = new URLSearchParams(location.search);
    const urlTab = query.get('tab') as TabKey | null;
    const chosenTab: TabKey = urlTab && TABS.includes(urlTab) ? urlTab : 'package';
    const selectedPart = query.get('select') ?? undefined;
    // the fund is reported closed while it loads, so the connect tab may become available later
    const tab: TabKey = chosenTab === 'connect' && !connectable ? 'package' : chosenTab;

    /** Změní v adrese jen dané parametry; ostatní (i cizí) zůstanou. */
    const updateAddress = (changes: { tab?: TabKey; select?: string | null }) => {
        const next = new URLSearchParams(location.search);
        if (changes.tab !== undefined) {
            changes.tab === 'package' ? next.delete('tab') : next.set('tab', changes.tab);
        }
        if (changes.select !== undefined) {
            changes.select == null ? next.delete('select') : next.set('select', changes.select);
        }
        const search = next.toString();
        if (search !== query.toString()) {
            history.replace({ pathname: location.pathname, search: search ? '?' + search : '' });
        }
    };
    const setTab = (next: TabKey) => updateAddress({ tab: next });
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
                {tab === 'structure' && <AipExplorer mode={ExplorerMode.VIEW} hideRoot selected={selectedPart}
                                                      onSelectionChange={uuid => updateAddress({select: uuid ?? null})}/>}
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
