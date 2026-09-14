/**
 * Samostatná stránka průzkumníka AIPu - hlavička s návratem na seznam a záložky průzkumníka.
 */
import { FC } from 'react';
import { FormattedMessage } from 'react-intl';
import { useHistory, useRouteMatch } from 'react-router';

import AipPageRibbon from 'components/aip/AipPageRibbon';
import AipExplorerTabs from 'components/aip/explorer/AipExplorerTabs';
import { explorerPageMessages } from 'components/aip/messages';
import { Button } from 'components/ui';
import { AREA_AIP } from 'actions/aip/aip';
import { urlAip } from '../../constants';
import { storeFromArea } from 'shared/utils';
import { AppState } from 'typings/store';
import { useAppSelector } from 'utils/hooks';

import './AipExplorerPage.scss';

interface AipExplorerPageUrlParams {
    id: string;
}

const AipExplorerPage: FC = () => {
    const history = useHistory();
    const match = useRouteMatch<AipExplorerPageUrlParams>();
    const aipId = Number(match.params.id);
    const aip = useAppSelector((state: AppState) => storeFromArea(state, AREA_AIP));

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
                <AipExplorerTabs aipId={aipId}/>
            </div>
        </>
    );
};

export default AipExplorerPage;
