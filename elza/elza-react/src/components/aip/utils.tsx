import { Icon } from 'components/shared';
import { pad2 } from 'components/validate';
import {Aips} from 'typings/store';

export const getAipRows = (aips: Aips) => {
    if(aips.fetched && aips.rows){
        if(
            aips.filter?.from &&
            aips.filter?.pageSize &&
            aips.filter.from > aips.filter.pageSize - 1
        ){
            return aips.rows;
        }
        return [
            ...aips.rows,
        ];
    }

    return [];
};

export const getBoolIcon = (bool: Boolean) => {
    return bool ? <Icon glyph="fa-check"/> : <Icon glyph="fa-close"/>;
}

export const generateUUID = () => {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
        const r = Math.random() * 16 | 0;
        const v = c == 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}
    
/**
 * Přeformátování datumu do DD.MM.YYYY řetězce.
 *
 * @param date
 * @returns {string} formatted date
 */
export const formatDate = (date: Date): string => {
    let month = date.getMonth() + 1,
        day = date.getDate(),
        year = date.getFullYear();
    return [pad2(day), pad2(month), year].join('.');
}

export const formatAipSize = (bytes: number): string => {
    if (bytes === 0) return '0 Bytes';
    
    const k = 1024;
    const sizes = ['B', 'kB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    
    const size = (bytes / Math.pow(k, i)).toFixed(1);
    
    return `${size} ${sizes[i]}`;
}

const DA_AIP_PATH = "da_aip";
const DA_AIP_STATE_PATH = "da_aip_state";
const DA_SYNC_QUEUE_ITEM = "da_sync_queue_item";
const ORIG_ACCESS_POINT = "originator_access_point";
const INST_ACCESS_POINT = "institution_access_point";
const ARR_FUND = "arr_fund";

export const findColDefByKey = (key: string) => {
    return colDef.find(item => item.key == key);
}

export const colDef = [
    {key: "aipId", name: "ID Aipu", path: DA_AIP_PATH, type: "number", minWidth: 50, idealWidth: 70},
    {key: "code", name: "Kód aipu", path: DA_AIP_PATH, type: "string", minWidth: 50, idealWidth: 70},
    {key: "aipVersion", name: "Verze Aipu", path: DA_AIP_STATE_PATH,type: "string", minWidth: 60, idealWidth: 90},
    {key: "fundName", name: "Archivní soubor", path: ARR_FUND, type: "ref", minWidth: 70, idealWidth: 115},
    {key: "instApName", name: "Instituce", path: INST_ACCESS_POINT, type: "ref", minWidth: 70, idealWidth: 70},
    {key: "institutionCode", name: "Kód instutuce", path:DA_AIP_STATE_PATH, type: "string", minWidth: 70, idealWidth:70},
    {key: "unitdateFrom", name: "Datace od-do", path: DA_AIP_STATE_PATH, type: "date", minWidth: 60, idealWidth: 105},
    {key: "originApName", name: "Původce", path: ORIG_ACCESS_POINT, type: "ref", minWidth: 70, idealWidth: 70},
    {key: "ingestionCode", name: "Číslo příjemky", path: DA_AIP_STATE_PATH,type: "string", minWidth: 65, idealWidth: 105},
    {key: "referenceNumber", name: "Číslo jednací", path: DA_AIP_STATE_PATH, type: "string", minWidth: 60, idealWidth: 100 },
    {key: "nadChangeCode",name: "Vnější změna", path: DA_AIP_STATE_PATH,type: "string", minWidth: 60, idealWidth: 100},
    {key: "aipSize", name: "Velikost", path: DA_AIP_STATE_PATH,type: "number", minWidth: 65, idealWidth: 65},
    {key: "metadataLoad", name: "Načtená metadata", path: DA_AIP_STATE_PATH, type: "bool", minWidth: 70, idealWidth: 130},
    // TODO: @kasparova Bude upřesněno v budoucnu
    // {name: "Stažené komponenty", type: "bool", minWidth: 90, idealWidth: 145},
    // {name: "Napojen archivní popis", type: "bool", minWidth: 70, idealWidth: 155},
     {key: "state", name: "Aktuální verze", path: DA_SYNC_QUEUE_ITEM, type: "enum", minWidth: 65, idealWidth: 105}
]