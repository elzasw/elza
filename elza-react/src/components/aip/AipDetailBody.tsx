import { DaAipDetailVO } from "api/DaAipDetailVO";
import DetailRow from "./DetailRow";
import {formatAipSize, formatDate, getBoolIcon, getConnectedToJP, getConnectedToJPIcon} from "./utils";

type AipDetailBodyProps = {
    detail: DaAipDetailVO;
}

const AipDetailBody = ({detail}: AipDetailBodyProps) => {
    return (
        <>
            {detail.aipId &&
                <DetailRow label="Id" value={detail.aipId.toString()}/>}
            {detail.code &&
                <DetailRow label="Kód aipu" value={detail.code.toString()}/>}
            {detail.aipVersion &&
                <DetailRow label="Verze" value={detail.aipVersion}/>}
            {detail.fund &&
                <DetailRow label="Archivní soubor" value={
                    <a href={`/fund/${detail.fund.id}`}>{detail.fund.name}</a>
                }/>
            }
            {detail.institution &&
                <DetailRow label="Instituce" value={
                    <a href={`/entity/${detail.institution.id}`}>{detail.institution.name}</a>
                }/>
            }
            {detail.institutionCode &&
                <DetailRow label="Kód instituce" value={detail.institutionCode}/>}
            {detail.unitdateFrom &&
                <DetailRow label="Dotace od-do" value={
                    formatDate(new Date(detail.unitdateFrom)) + " - " + formatDate(new Date(detail.unitdateTo))
                }/>}
            {detail.originatorInstitution &&
                <DetailRow label="Původce" value={
                    <a href={`/entity/${detail.originatorInstitution.id}`}>{detail.originatorInstitution.name}</a>
                }/>
            }
            {detail.originator && !detail.originatorInstitution &&
                <DetailRow label="Původce" value={detail.originator}/>
            }
            {detail.ingestionCode &&
                <DetailRow label="Číslo přejímky" value={detail.ingestionCode}/>}
            {detail.referenceNumber &&
                <DetailRow label="Číslo jednací" value={detail.referenceNumber}/>}
            {detail.nadChangeCode &&
                <DetailRow label="Vnější změna" value={detail.nadChangeCode}/>}
            {detail.aipSize > -1 &&
                <DetailRow label="Velikost" value={formatAipSize(detail.aipSize)}/>}
            {detail != null &&
                <DetailRow label="Načtena metadata" value={getBoolIcon(detail.metadataLoad)}/>}
            <DetailRow label="Načten úplný AIP" value={getBoolIcon(detail.comleteAipLoad)}/>
            <DetailRow label="Chyba při načtení metadat" value={getBoolIcon(detail.metadataError)}/>
            {detail.metadataErrorException &&
                <DetailRow label="Chyba při načtení metadat" value={detail.metadataErrorException}/>}
            {detail.aipVersionMetadata &&
                <DetailRow label="Verze s načtenými metadaty" value={detail.aipVersionMetadata}/>}
            {detail.state &&
                <DetailRow label="Aktuální verze" value={detail.state}/>}
            <DetailRow label="Napojen archivní popis" value={getConnectedToJP(detail.linkedNodes, detail.fund.id)}/>
        </>
    );
}

export default AipDetailBody;
