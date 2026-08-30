import { useEffect, useState } from 'react';
import { Tab, Tabs } from 'react-bootstrap';
import { FormattedMessage } from 'react-intl';

import { aipFetchIfNeeded, selectAip } from 'actions/aip/aip';
import { useThunkDispatch } from 'utils/hooks';
import AipExplorer from './AipExplorer';
import { ExplorerMode } from './ExplorerContext';
import PackageBrowser from './PackageBrowser';
import { explorerPageMessages } from '../messages';
import './AipExplorerTabs.scss';

type Props = {
    aipId: number;
};

/**
 * Obsah průzkumníka AIPu.
 *
 * Výchozí je Struktura, tedy digitální entity, které vznikly zpracováním - to je běžný
 * pohled na balíček. Záložka Balíček ukazuje stažený balíček tak, jak přišel z digitálního
 * archivu; slouží k diagnostice, hlavně když se zpracování metadat nezdařilo.
 */
export function AipExplorerTabs({aipId}: Props) {
    const dispatch = useThunkDispatch();
    const [tab, setTab] = useState<string>('structure');

    useEffect(() => {
        dispatch(selectAip(aipId));
        dispatch(aipFetchIfNeeded(aipId));
    }, [aipId]);

    return (
        <Tabs activeKey={tab} onSelect={key => setTab(key ?? 'structure')} className="aip-explorer-tabs">
            <Tab eventKey="structure" title={<FormattedMessage {...explorerPageMessages.structureTab}/>}>
                <AipExplorer mode={ExplorerMode.VIEW}/>
            </Tab>
            <Tab eventKey="package" title={<FormattedMessage {...explorerPageMessages.packageTab}/>}>
                <PackageBrowser aipId={aipId}/>
            </Tab>
        </Tabs>
    );
}

export default AipExplorerTabs;
