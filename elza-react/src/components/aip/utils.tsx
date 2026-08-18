import {Icon} from 'components/shared';
import { pad2 } from 'components/validate';
import {LinkedNodeVO, LinkType} from 'elza-api';
import {Aips} from 'typings/store';
import {Button} from "react-bootstrap";
import {serverContextPath} from "../../api";

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

export const getConnectedToJPIcon = (link: LinkType | null) => {
    let iconString = "fa fa-close";
    if(link == LinkType.Aip) {
        iconString="fa fa-check"
    }else if(link == LinkType.ComponentAip) {
        iconString="fa fa-chain-broken"
    } else if(link == LinkType.PartAip) {
        iconString="fa fa-link"
    }
    return <Icon glyph={iconString}/>;
}

export const getConnectedToJP = (linkedNodes: Array<LinkedNodeVO> | null, fundId: number, handleDeleteLink: any) => {
    let iconString = "fa fa-close";
    let nodes;

    if (linkedNodes) {
        iconString = "fa fa-check";

        nodes = linkedNodes.map(item =>
            <div key={item.id}>
                <a href={`${serverContextPath}/fund/${fundId}/node/${item.nodeId}`}>{item.name}</a>
                <Button key="deleteLink" variant="action" onClick={() => handleDeleteLink(item.id)}>
                    <Icon glyph="fa fa-close" />
                </Button>
            </div>)
    }

    return <div><Icon glyph={iconString}/> {nodes}</div>;
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
    if (bytes === 0) return '0 B';

    const k = 1024;
    const sizes = ['B', 'kB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    const size = (bytes / Math.pow(k, i)).toFixed(1);

    return `${size} ${sizes[i]}`;
}

