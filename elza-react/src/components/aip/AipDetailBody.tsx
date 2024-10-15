import { DaAipDetailVO } from "api/DaAipDetailVO";
import DetailRow from "./DetailRow";
import {formatAipSize, formatDate, getBoolIcon, getConnectedToJP} from "./utils";
import {Api, serverContextPath} from "../../api";
import {useThunkDispatch} from "../../utils/hooks";
import {aipFetchIfNeeded} from "../../actions/aip/aip.ts";
import i18n from 'components/i18n';

type AipDetailBodyProps = {
    detail: DaAipDetailVO;
}

const AipDetailBody = ({detail}: AipDetailBodyProps) => {
    const dispatch = useThunkDispatch();

    const handleDeleteLink = (linkId: number) => {
        Api.aips.aipDeleteDaoLink(linkId).then(() => {
            dispatch(aipFetchIfNeeded(detail.aipId, true))
        });
    }

    console.log('detail :>> ', detail);

    return (
        <>
            {detail.aipId &&
                <DetailRow label={i18n("aip.detail.id")} value={detail.aipId.toString()}/>}
            {detail.code &&
                <DetailRow label={i18n("aip.detail.code")} value={detail.code.toString()}/>}
            {detail.aipVersion &&
                <DetailRow label={i18n("aip.detail.version")} value={detail.aipVersion}/>}
            {detail.fund &&
                <DetailRow label={i18n("aip.detail.fund")} value={
                    <a href={`${serverContextPath}/fund/${detail.fund.id}`}>{detail.fund.name}</a>
                }/>
            }
            {detail.fundCode &&
                <DetailRow label={i18n("aip.detail.fundCode")} value={detail.fundCode}/>}
            {detail.institution &&
                <DetailRow label={i18n("aip.detail.institution.name")} value={
                    <a href={`${serverContextPath}/entity/${detail.institution.id}`}>{detail.institution.name}</a>
                }/>
            }
            {detail.institutionCode &&
                <DetailRow label={i18n("aip.detail.institution.code")}value={detail.institutionCode}/>}
            {detail.unitdateFrom &&
                <DetailRow label={i18n("aip.detail.unitdateFromTo")} value={
                    formatDate(new Date(detail.unitdateFrom)) + " - " + formatDate(new Date(detail.unitdateTo))
                }/>}
            {detail.originatorInstitution &&
                <DetailRow label={i18n("aip.detail.originator")}value={
                    <a href={`${serverContextPath}/entity/${detail.originatorInstitution.id}`}>{detail.originatorInstitution.name}</a>
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
            {detail.metadataError && 
                <DetailRow label={i18n("aip.detail.metadataError")} value={getBoolIcon(detail.metadataError)}/>}
            {detail.metadataErrorException &&
                <DetailRow label={i18n("aip.detail.metadataError")} value={detail.metadataErrorException}/>}
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
