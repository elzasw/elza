/**
 * Vstupní URL /da/<kód digitálního archivu>/aip/<identifikátor AIPu v archivu>[?part=<část>].
 *
 * Jiná aplikace (typicky sám digitální archiv) zná jen svůj identifikátor balíčku, ne jednotku
 * popisu, ke které je v ELZA připojen. Stránka balíček dohledá obecným hledáním AIP a hned
 * přejde dál - viz aipEntryTarget; zůstane zobrazená jen tehdy, když balíček v ELZA není.
 * Na serveru je jen mapování adresy na aplikaci (i pro nepřihlášeného uživatele).
 */
import { FC, useEffect, useState } from 'react';
import { defineMessages, FormattedMessage } from 'react-intl';
import { useHistory, useLocation, useRouteMatch } from 'react-router';

import { Api } from '../../api';
import { Loading } from 'components/shared';
import { Button } from 'components/ui';
import { urlAip } from '../../constants';
import { aipEntryFound, aipEntrySearch, aipEntryTarget } from './aipEntryTarget';

import './AipEntryPage.scss';

const messages = defineMessages({
    notFound: {
        id: 'aip.entry.notFound',
        defaultMessage: 'AIP „{aipCode}“ digitálního archivu „{repositoryCode}“ v ELZA není. Pokud jej archiv předal teprve nedávno, může ještě čekat ve frontě ke stažení.',
    },
    failed: {
        id: 'aip.entry.failed',
        defaultMessage: 'AIP „{aipCode}“ digitálního archivu „{repositoryCode}“ se nepodařilo dohledat: {message}',
    },
    aipList: {
        id: 'aip.entry.aipList',
        defaultMessage: 'Otevřít seznam AIP',
    },
});

interface AipEntryPageUrlParams {
    repositoryCode: string;
    aipCode: string;
}

/** Why the page stays: the AIP is not in ELZA, or the search failed (with the reason). */
type Failure = { notFound: boolean; message?: string };

const AipEntryPage: FC = () => {
    const history = useHistory();
    const location = useLocation();
    const { repositoryCode, aipCode } = useRouteMatch<AipEntryPageUrlParams>().params;
    const part = new URLSearchParams(location.search).get('part');
    const [failure, setFailure] = useState<Failure | undefined>();

    useEffect(() => {
        let active = true;
        setFailure(undefined);
        Api.aips.aipFindByFilter(aipEntrySearch(repositoryCode, aipCode), undefined, { overrideErrorHandler: true })
            .then(({ data }) => {
                if (!active) {
                    return;
                }
                const aip = aipEntryFound(data.rows, aipCode);
                if (aip) {
                    // replace: going back must not land on the entry URL and forward again
                    history.replace(aipEntryTarget(aip, part));
                } else {
                    setFailure({ notFound: true });
                }
            })
            .catch(error => {
                if (active) {
                    setFailure({ notFound: false,
                        message: error?.response?.data?.message ?? error?.message ?? String(error?.response?.status) });
                }
            });
        return () => {
            active = false;
        };
    }, [history, repositoryCode, aipCode, part]);

    if (!failure) {
        return <Loading />;
    }
    return (
        <div className="aip-entry-page">
            <p>
                {failure.notFound
                    ? <FormattedMessage {...messages.notFound} values={{ repositoryCode, aipCode }} />
                    : <FormattedMessage {...messages.failed} values={{ repositoryCode, aipCode, message: failure.message }} />}
            </p>
            <Button onClick={() => history.push(urlAip())}>
                <FormattedMessage {...messages.aipList} />
            </Button>
        </div>
    );
};

export default AipEntryPage;
