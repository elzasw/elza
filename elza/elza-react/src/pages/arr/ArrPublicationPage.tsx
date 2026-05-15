import { Icon, Ribbon } from 'components/index.jsx';
import { RibbonGroup } from 'components/shared';
import { Button } from 'components/ui';
import PageLayout from '../shared/layout/PageLayout';
import { useSelector } from 'react-redux';
import { AppState } from 'typings/store';
import PublicationTable from 'components/arr/publication/PublicationTable';
import { getFundVersion } from '../../constants';
import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { PublicationSystemsDialog } from 'components/arr/publication/PublicationSystemsDialog';
import * as perms from 'actions/user/Permission';
import { FormattedMessage } from 'react-intl';
import { PublicationType } from 'elza-api';
import { Api } from 'api/api';

export function ArrPublicationPage() {
    const { id } = useParams<{ id: string }>();
    const fundId = Number(id);
    const splitter = useSelector(({ splitter }: AppState) => splitter);
    const activeFund = useSelector(({ arrRegion }: AppState) =>
        arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null
    );
    const userDetail = useSelector(({ userDetail }: AppState) => userDetail);
    const [publicationSystemsOpen, setPublicationSystemsOpen] = useState(false);
    const [publicationTypes, setPublicationTypes] = useState<PublicationType[]>([]);

    const fetchPublicationTypes = async () => {
        const { data } = await Api.publication.publicationTypeAdminListPublicationTypes();
        setPublicationTypes(data);
    };

    useEffect(() => {
        fetchPublicationTypes();
    }, []);

    const isFundAdmin = userDetail.hasOne(perms.ADMIN);

    const altSection = isFundAdmin ? (
        <RibbonGroup key="alt-actions" className="small">
            <Button key="publication-systems" onClick={() => setPublicationSystemsOpen(true)}>
                <Icon glyph="fa-newspaper-o" />
                <div>
                    <span className="btnText">
                        <FormattedMessage
                            id="ribbon.action.arr.fund.publicationSystems"
                            defaultMessage="Správa publikačních systémů"
                        />
                    </span>
                </div>
            </Button>
        </RibbonGroup>
    ) : undefined;

    const ribbon = (
        <>
            <Ribbon
                arr
                subMenu
                fundId={activeFund?.id}
                versionId={getFundVersion(activeFund)}
                altSection={altSection}
            />
            <PublicationSystemsDialog
                open={publicationSystemsOpen}
                onClose={() => {
                    setPublicationSystemsOpen(false);
                    fetchPublicationTypes();
                }}
            />
        </>
    );

    const centerPanel = (
        <div className="splitter-home">
            <div style={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}>
                <PublicationTable fundId={fundId} publicationTypes={publicationTypes} />
            </div>
        </div>
    );

    return <PageLayout splitter={splitter} ribbon={ribbon} centerPanel={centerPanel} />;
}

export default ArrPublicationPage;
