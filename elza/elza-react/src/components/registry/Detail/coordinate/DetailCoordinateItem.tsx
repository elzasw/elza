import { CoordinatesDisplay } from 'components/shared/coordinates/CoordinatesDisplay';
import React from 'react';
import { ApItemCoordinatesVO } from 'api/ApItemCoordinatesVO';

interface Props {
    item: ApItemCoordinatesVO;
}

export const DetailCoordinateItem: React.FC<Props> = ({
    item,
}) => {
    return <CoordinatesDisplay value={item.value} id={item.id}/>
};
