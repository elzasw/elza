/**
 * Samostatná stránka průzkumníka AIPu.
 *
 * Záložka Balíček ukazuje stažený balíček tak, jak přišel z digitálního archivu, a je
 * dostupná i tehdy, když se zpracování metadat nezdařilo. Záložka Struktura ukazuje
 * digitální entity, které ze zpracování vznikly, takže má smysl až po něm.
 */
import { FC, useEffect, useState } from 'react';
import { Tab, Tabs } from 'react-bootstrap';
import { FormattedMessage } from 'react-intl';
import { useHistory, useRouteMatch } from 'react-router';

import AipExplorer from 'components/aip/explorer/AipExplorer';
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
    const [tab, setTab] = useState<string>('package');

    useEffect(() => {
        dispatch(selectAip(aipId));
        dispatch(aipFetchIfNeeded(aipId));
    }, [aipId]);

    return (
        <div className="aip-explorer-page">
            <div className="aip-explorer-page-header">
                <Button variant="link" onClick={() => history.push(urlAip(aipId))}>
                    <FormattedMessage {...explorerPageMessages.back}/>
                </Button>
                <span className="aip-explorer-page-title">{aip?.data?.code}</span>
            </div>
            <Tabs activeKey={tab} onSelect={key => setTab(key ?? 'package')} className="aip-explorer-page-tabs">
                <Tab eventKey="package" title={<FormattedMessage {...explorerPageMessages.packageTab}/>}>
                    <PackageBrowser aipId={aipId}/>
                </Tab>
                <Tab eventKey="structure" title={<FormattedMessage {...explorerPageMessages.structureTab}/>}>
                    <AipExplorer mode={ExplorerMode.VIEW}/>
                </Tab>
            </Tabs>
        </div>
    );
};

export default AipExplorerPage;
