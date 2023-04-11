import { WebApi } from 'actions';
import { getArchiveEntityUrl } from "actions/registry/registry";
import i18n from "components/i18n";
import { TooltipTrigger } from 'components/shared';
import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { ApAccessPointVO } from '../../../../api/ApAccessPointVO';
import { Icon } from '../../../index';
import DetailDescriptionsItem from './DetailDescriptionsItem';
import './DetailHeader.scss';

export const ReplacedEntities = ({ids = []}:{ids?: number[]}) => {
    const [entities, setEntities] = useState<ApAccessPointVO[]>();
    const [fetching, setFetching] = useState(false);
    const isFetched = entities !== undefined;

    const handleMouseEnter = () => {
        if(!isFetched){
            setFetching(true)
            setTimeout(async () => {
                const response = await Promise.all(ids.map((id) => WebApi.getAccessPoint(id)))
                setEntities(response);
                setFetching(false);
            }, 0)
        }
    }

    return (
        <TooltipTrigger
            style={{width: "auto"}}
            onMouseEnter={handleMouseEnter}
            content={
            fetching
                ? <div style={{
                    width: "30px",
                    height: "30px",
                    display:"flex",
                    alignItems: "center",
                    justifyContent: "center"}}
                >
                    <Icon glyph="fa-spinner" className="fa-spin"/>

                </div>
                : <div>
                    <div>{`${i18n("ap.detail.replacingEntities")}: `}</div>
                    {entities?.map((entity) => {
                        return <div>
                            <Link style={{color: "#33afff"}} to={getArchiveEntityUrl(entity.id)}> {`${entity.id}: ${entity.name || i18n("ap.detail.replacedEntity.noName")}`} </Link>
                        </div>
                    })
                }
                    </div>
        }>
            <DetailDescriptionsItem>
                <div >
                    <Icon glyph={'fa-sitemap'} className="fa-rotate-90"/>
                </div>
            </DetailDescriptionsItem>
        </TooltipTrigger>
    )
}
