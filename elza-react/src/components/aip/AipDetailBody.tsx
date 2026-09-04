import { useIntl } from 'react-intl';
import { Link } from 'react-router-dom';
import { AipDetailVO } from 'elza-api';

import { Icon } from 'components/shared';
import { useThunkDispatch } from 'utils/hooks';
import { Api, serverContextPath } from '../../api';
import { aipFetchIfNeeded } from '../../actions/aip/aip.ts';
import { urlEntity } from '../../constants';
import { DetailRow } from './DetailRow';
import { QueueStateCell, getBoolIcon, getConnectedToJP } from './AipCells';
import { formatAipSize, formatUnitDate } from './format';
import { detailMessages, linkStateMessages, messages, problemMessages } from './messages';

interface Props {
    detail: AipDetailVO;
}

/**
 * Values of one AIP. A row is shown only for a value the AIP has - an empty row says nothing -
 * except the load and link flags, where "no" is information too. Labels shared with the list
 * come from the column messages, so the two never drift apart.
 */
export function AipDetailBody({ detail }: Props) {
    const { formatMessage } = useIntl();
    const dispatch = useThunkDispatch();

    const handleDeleteLink = (linkId: number) => {
        Api.aips.aipDeleteDaoLink(linkId).then(() => {
            dispatch(aipFetchIfNeeded(detail.aipId, true));
        });
    };

    return (
        <>
            <DetailRow label={formatMessage(messages.aipId)} value={detail.aipId} />
            <DetailRow label={formatMessage(messages.code)} value={detail.code} />
            {detail.aipVersion &&
                <DetailRow label={formatMessage(messages.aipVersion)} value={detail.aipVersion} />}
            {/* Popis problému je celá věta a nese cesty a jmenné prostory, takže dostává
                celou šířku panelu, ne polovinu jako ostatní hodnoty. */}
            {detail.problemType &&
                <div className="aip-problem-block">
                    <div className="aip-problem">
                        <Icon glyph="fa-exclamation-triangle" />
                        {formatMessage(problemMessages[detail.problemType])}
                    </div>
                    {detail.problemDescription &&
                        <div className="aip-problem-description">{detail.problemDescription}</div>}
                </div>}
            {detail.fund &&
                <DetailRow label={formatMessage(messages.fund)} value={
                    <a href={`${serverContextPath}/fund/${detail.fund.id}`}>{detail.fund.name}</a>
                } />}
            {detail.fundCode &&
                <DetailRow label={formatMessage(messages.fundCode)} value={detail.fundCode} />}
            {detail.institution &&
                <DetailRow label={formatMessage(messages.institution)} value={
                    <Link to={urlEntity(detail.institution.accessPointId)}>{detail.institution.name}</Link>
                } />}
            {detail.institutionCode &&
                <DetailRow label={formatMessage(messages.institutionCode)} value={detail.institutionCode} />}
            {detail.unitdateFrom &&
                <DetailRow label={formatMessage(messages.unitdate)}
                           value={formatUnitDate(detail.unitdateFrom, detail.unitdateTo)} />}
            {detail.originatorInstitution
                ? <DetailRow label={formatMessage(messages.originator)} value={
                    <Link to={urlEntity(detail.originatorInstitution.accessPointId)}>{detail.originatorInstitution.name}</Link>
                } />
                : detail.originator &&
                    <DetailRow label={formatMessage(messages.originator)} value={detail.originator} />}
            {detail.ingestionCode &&
                <DetailRow label={formatMessage(messages.ingestionCode)} value={detail.ingestionCode} />}
            {detail.referenceNumber &&
                <DetailRow label={formatMessage(messages.referenceNumber)} value={detail.referenceNumber} />}
            {detail.nadChangeCode &&
                <DetailRow label={formatMessage(messages.nadChangeCode)} value={detail.nadChangeCode} />}
            {detail.aipSize != null && detail.aipSize > -1 &&
                <DetailRow label={formatMessage(messages.aipSize)} value={formatAipSize(detail.aipSize)} />}
            <DetailRow label={formatMessage(messages.metadataLoad)} value={getBoolIcon(detail.metadataLoad)} />
            <DetailRow label={formatMessage(messages.completeAipLoad)} value={getBoolIcon(detail.completeAipLoad)} />
            {detail.aipVersionMetadata &&
                <DetailRow label={formatMessage(detailMessages.aipVersionMetadata)} value={detail.aipVersionMetadata} />}
            {detail.importState &&
                <DetailRow label={formatMessage(messages.importState)} value={
                    <QueueStateCell state={detail.importState} message={detail.importStateMessage}
                                    date={detail.importStateDate} />
                } />}
            {detail.exportState &&
                <DetailRow label={formatMessage(messages.exportState)} value={
                    <QueueStateCell state={detail.exportState} message={detail.exportStateMessage}
                                    date={detail.exportStateDate} />
                } />}
            {detail.linkState &&
                <DetailRow label={formatMessage(messages.linkState)}
                           value={formatMessage(linkStateMessages[detail.linkState])} />}
            {detail.fund &&
                <DetailRow label={formatMessage(detailMessages.linkedNodes)}
                           value={getConnectedToJP(detail.linkedNodes, detail.fund.id, handleDeleteLink)} />}
        </>
    );
}

export type AipDetailBodyProps = Props;
