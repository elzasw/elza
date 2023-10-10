import { WebApi } from "actions";
import { urlEntity } from "../../../../constants";
import { ApAccessPointVO } from 'api/ApAccessPointVO';
import i18n from "components/i18n";
import { TooltipTrigger } from 'components/shared';
import React, { FC, useEffect, useState } from 'react';
import { Link } from "react-router-dom";

interface Props {
    className?: string;
    entityId?: number;
}

export const DescriptionEntityRef: FC<Props> = ({
    entityId,
}) => {
    const [replacedByItem, setReplacedByItem] = useState<ApAccessPointVO>()
    useEffect(() => {
        WebApi.getAccessPoint(entityId).then((response)=>{
            setReplacedByItem(response);
        })
    },[entityId])

    if(entityId == undefined) {return<></>}

    return (
        <TooltipTrigger 
            content={
            <>
                <div>id: {replacedByItem?.id}</div>
                <div>uuid: {replacedByItem?.uuid}</div>
                </>
        }
        >
            {`${i18n("ap.detail.replacedBy")}: `}
            <Link to={urlEntity(entityId)}>
                {replacedByItem ? replacedByItem.name : entityId}
            </Link>
        </TooltipTrigger>
    );
};
