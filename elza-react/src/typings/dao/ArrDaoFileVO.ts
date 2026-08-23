/** Odpovídá cz.tacr.elza.controller.vo.ArrDaoFileVO. */
export interface ArrDaoFileVO {
    id: number;
    code?: string;
    fileName?: string;
    checksum?: string;
    checksumType?: string;
    created?: string;
    mimetype?: string;
    size?: number;
    duration?: string;
    imageWidth?: number;
    imageHeight?: number;
    sourceXDimesionUnit?: string;
    sourceXDimesionValue?: number;
    sourceYDimesionUnit?: string;
    sourceYDimesionValue?: number;
    description?: string;
    url?: string;
    thumbnailUrl?: string;
}
