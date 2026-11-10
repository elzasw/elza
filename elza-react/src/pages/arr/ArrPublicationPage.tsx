import { Ribbon } from 'components/index.jsx';
import PageLayout from '../shared/layout/PageLayout';
import { useSelector } from 'react-redux';
import { AppState } from 'typings/store';
import PublicationTable from 'components/arr/publication/PublicationTable';
import { getFundVersion } from '../../constants';
import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { PublicationType } from 'elza-api';
import { Api } from 'api/api';

export function ArrPublicationPage() {
    const { id } = useParams<{ id: string }>();
    const fundId = Number(id);
    const activeFund = useSelector(({ arrRegion }: AppState) =>
        arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null
    );
    const [publicationTypes, setPublicationTypes] = useState<PublicationType[]>([]);

    const fetchPublicationTypes = async () => {
        const { data } = await Api.publication.publicationTypeAdminListPublicationTypes();
        setPublicationTypes(data);
    };

    useEffect(() => {
        fetchPublicationTypes();
    }, []);

    const ribbon = (
        <Ribbon
            arr
            subMenu
            fundId={activeFund?.id}
            versionId={getFundVersion(activeFund)}
        />
    );

    const centerPanel = (
        <div className="splitter-home">
            <div style={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}>
                <PublicationTable fundId={fundId} publicationTypes={publicationTypes} />
            </div>
        </div>
    );

    return <PageLayout ribbon={ribbon} centerPanel={centerPanel} />;
}

export default ArrPublicationPage;
