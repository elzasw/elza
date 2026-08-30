/**
 * Samostatná stránka průzkumníka AIPu.
 *
 * Výchozí je Struktura, tedy digitální entity, které vznikly zpracováním - to je běžný
 * pohled na balíček. Záložka Balíček ukazuje stažený balíček tak, jak přišel z digitálního
 * archivu; slouží k diagnostice, hlavně když se zpracování metadat nezdařilo.
 */
import { FC, useEffect, useState } from 'react';
import { Tab, Tabs } from 'react-bootstrap';
import { FormattedMessage } from 'react-intl';
import { useHistory, useRouteMatch } from 'react-router';

import AipExplorer from 'components/aip/explorer/AipExplorer';
import AipPageRibbon from 'components/aip/AipPageRibbon';
import { ExplorerMode } from 'components/aip/explorer/ExplorerContext';
import PackageBrowser from 'components/aip/explorer/PackageBrowser';
import { explorerPageMessages } from 'components/aip/messages';
import { Button } from 'components/ui';
import { AREA_AIP, aipFetchIfNeeded, selectAip } from 'actions/aip/aip';
import { urlAip } from '../../constants';
import { storeFromArea } from 'shared/utils';
import { AppState } from 'typings/store';
import { useAppSelector, useThunkDispatch } from 'utils/hooks';

import './AipExplorerPage.scss';

interface AipExplorerPageUrlParams {
    id: string;
}

const AipExplorerPage: FC = () => {
    const dispatch = useThunkDispatch();
    const history = useHistory();
    const match = useRouteMatch<AipExplorerPageUrlParams>();
    const aipId = Number(match.params.id);
    const aip = useAppSelector((state: AppState) => storeFromArea(state, AREA_AIP));
    const [tab, setTab] = useState<string>('structure');

    useEffect(() => {
        dispatch(selectAip(aipId));
        dispatch(aipFetchIfNeeded(aipId));
    }, [aipId]);

    return (
        <>
            <AipPageRibbon />
            <div className="aip-explorer-page">
            <div className="aip-explorer-page-header">
                <Button variant="link" onClick={() => history.push(urlAip(aipId))}>
                    <FormattedMessage {...explorerPageMessages.back}/>
                </Button>
                <span className="aip-explorer-page-title">{aip?.data?.code}</span>
            </div>
            <Tabs activeKey={tab} onSelect={key => setTab(key ?? 'structure')} className="aip-explorer-page-tabs">
                <Tab eventKey="structure" title={<FormattedMessage {...explorerPageMessages.structureTab}/>}>
                    <AipExplorer mode={ExplorerMode.VIEW}/>
                </Tab>
                <Tab eventKey="package" title={<FormattedMessage {...explorerPageMessages.packageTab}/>}>
                    <PackageBrowser aipId={aipId}
                                    problemType={aip?.data?.problemType}
                                    problemDescription={aip?.data?.problemDescription}
                                    problemFile={aip?.data?.problemFile}/>
                </Tab>
            </Tabs>
            </div>
        </>
    );
};

export default AipExplorerPage;
