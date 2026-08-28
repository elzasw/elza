import DetailRow from "./DetailRow";
import {formatAipSize} from "./format";
import {formatDateCz} from "utils/date";
import {getBoolIcon, getConnectedToJP} from "./AipCells";
import { Icon } from "components/shared";
import {Api, serverContextPath} from "../../api";
import {useThunkDispatch} from "../../utils/hooks";
import {aipFetchIfNeeded} from "../../actions/aip/aip.ts";
import i18n from 'components/i18n';
import {AipDetailVO} from "elza-api";
import { Link } from "react-router-dom";
import { urlEntity } from "../../constants";
import { FormattedMessage, useIntl } from "react-intl";
import { detailMessages, problemMessages } from "./messages";

type AipDetailBodyProps = {
    detail: AipDetailVO;
}

const AipDetailBody = ({detail}: AipDetailBodyProps) => {
    const dispatch = useThunkDispatch();
    const intl = useIntl();

    const handleDeleteLink = (linkId: number) => {
        Api.aips.aipDeleteDaoLink(linkId).then(() => {
            dispatch(aipFetchIfNeeded(detail.aipId, true))
        });
    }

    return (
        <>
            {detail.aipId &&
                <DetailRow label={i18n("aip.detail.id")} value={detail.aipId.toString()}/>}
            {detail.code &&
                <DetailRow label={i18n("aip.detail.code")} value={detail.code.toString()}/>}
            {detail.aipVersion &&
                <DetailRow label={i18n("aip.detail.version")} value={detail.aipVersion}/>}
            {detail.problemType &&
                <DetailRow label={intl.formatMessage(detailMessages.problem)} value={
                    <span className="aip-problem">
                        <Icon glyph="fa-exclamation-triangle"/>
                        <FormattedMessage {...problemMessages[detail.problemType]}/>
                    </span>
                }/>}
            {detail.problemDescription &&
                <DetailRow label={intl.formatMessage(detailMessages.problemDescription)}
                           value={detail.problemDescription}/>}
            {detail.fund &&
                <DetailRow label={i18n("aip.detail.fund")} value={
                    <a href={`${serverContextPath}/fund/${detail.fund.id}`}>{detail.fund.name}</a>
                }/>
            }
            {detail.fundCode &&
                <DetailRow label={i18n("aip.detail.fundCode")} value={detail.fundCode}/>}
            {detail.institution &&
                <DetailRow label={i18n("aip.detail.institution.name")} value={
                    <Link to={urlEntity(detail.institution.accessPointId)}>{detail.institution.name}</Link>
                }/>
            }
            {detail.institutionCode &&
                <DetailRow label={i18n("aip.detail.institution.code")}value={detail.institutionCode}/>}
            {detail.unitdateFrom &&
                <DetailRow label={i18n("aip.detail.unitdateFromTo")} value={
                    formatDateCz(new Date(detail.unitdateFrom)) + " - " + formatDateCz(new Date(detail.unitdateTo))
                }/>}
            {detail.originatorInstitution &&
                <DetailRow label={i18n("aip.detail.originator")}value={
                    <Link to={urlEntity(detail.originatorInstitution.accessPointId)}>{detail.originatorInstitution.name}</Link>
                }/>
            }
            {detail.originator && !detail.originatorInstitution &&
                <DetailRow label={i18n("aip.detail.originator")} value={detail.originator}/>
            }
            {detail.ingestionCode &&
                <DetailRow label={i18n("aip.detail.ingestionCode")} value={detail.ingestionCode}/>}
            {detail.referenceNumber &&
                <DetailRow label={i18n("aip.detail.referenceNumber")} value={detail.referenceNumber}/>}
            {detail.nadChangeCode &&
                <DetailRow label={i18n("aip.detail.nadChangeCode")} value={detail.nadChangeCode}/>}
            {detail.aipSize > -1 &&
                <DetailRow label={i18n("aip.detail.size")} value={formatAipSize(detail.aipSize)}/>}
            {detail != null &&
                <DetailRow label={i18n("aip.detail.metadataLoad")} value={getBoolIcon(detail.metadataLoad)}/>}
            <DetailRow label={i18n("aip.detail.completeAipLoad")} value={getBoolIcon(detail.completeAipLoad)}/>
            {detail.aipVersionMetadata &&
                <DetailRow label={i18n("aip.detail.aipVersionMetadata")} value={detail.aipVersionMetadata}/>}
            {detail.importState &&
                <DetailRow label={i18n("aip.detail.importState")} value={detail.importState}/>}
            {detail.exportState &&
                <DetailRow label={i18n("aip.detail.exportState")} value={detail.exportState}/>}
            {detail.fund &&
                <DetailRow label={i18n("aip.detail.linkedNode")} value={getConnectedToJP(detail.linkedNodes, detail.fund.id, handleDeleteLink)}/>}
        </>
    );
}

export default AipDetailBody;
