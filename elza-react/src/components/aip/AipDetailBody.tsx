import { useIntl } from 'react-intl';
import { Link } from 'react-router-dom';
import { AipDetailVO, LinkType, LinkedNodeVO } from 'elza-api';

import { Icon } from 'components/shared';
import { useThunkDispatch } from 'utils/hooks';
import { Api } from '../../api';
import { aipFetchIfNeeded } from '../../actions/aip/aip.ts';
import { urlEntity, urlFundAb } from '../../constants';
import { DetailRow } from './DetailRow';
import { QueueStateCell, getBoolIcon, getConnectedToJP } from './AipCells';
import { formatAipSize, formatUnitDate } from './format';
import { detailMessages, linkStateMessages, messages, packageMessages, problemMessages } from './messages';
import './AipDetailBody.scss';

interface Props {
    detail: AipDetailVO;
    /** Offered next to the problem when the caller can show the file the problem is about. */
    onOpenProblemFile?: (file: string) => void;
}

/**
 * A link without a type comes from a server that does not send it yet; it is treated as
 * a whole-package link, which is what the detail showed before the types existed.
 */
const isPackageLink = (link: LinkedNodeVO) => link.linkType == null || link.linkType === LinkType.Aip;

/**
 * Values of one AIP. A row is shown only for a value the AIP has - an empty row says nothing -
 * except the load and link flags, where "no" is information too. Labels shared with the list
 * come from the column messages, so the two never drift apart.
 */
export function AipDetailBody({ detail, onOpenProblemFile }: Props) {
    const { formatMessage } = useIntl();
    const dispatch = useThunkDispatch();

    const handleDeleteLink = (linkId: number) => {
        Api.aips.aipDeleteDaoLink(linkId).then(() => {
            dispatch(aipFetchIfNeeded(detail.aipId, true));
        });
    };

    // Links of the whole package are shown by name; links of its parts only counted - the parts
    // are browsed in the structure of the package, not here.
    const packageLinks = (detail.linkedNodes ?? []).filter(isPackageLink);
    const partLinkCount = (detail.linkedNodes ?? []).length - packageLinks.length;

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
                    {detail.problemFile && onOpenProblemFile &&
                        <div className="aip-problem-file">
                            {formatMessage(packageMessages.problemHint)}
                            <button type="button" className="aip-problem-file-link"
                                    onClick={() => onOpenProblemFile(detail.problemFile)}>
                                {detail.problemFile}
                            </button>
                        </div>}
                </div>}
            {detail.fund &&
                <DetailRow label={formatMessage(messages.fund)} value={
                    <Link to={urlFundAb(detail.fund.id, undefined, detail.aipId)}>{detail.fund.name}</Link>
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
                           value={getConnectedToJP(packageLinks, detail.fund.id, handleDeleteLink)} />}
            {partLinkCount > 0 &&
                <DetailRow label={formatMessage(detailMessages.partLinks)}
                           value={formatMessage(detailMessages.partLinksCount, { count: partLinkCount })} />}
        </>
    );
}

export type AipDetailBodyProps = Props;
