import { WebApi } from 'actions';
import { urlEntity } from '../../../../constants';
import { FormattedMessage, useIntl } from 'react-intl';
import { apDetailMessages } from '../messages';
import { TooltipTrigger } from 'components/shared';
import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { ApAccessPointVO } from '../../../../api/ApAccessPointVO';
import { Icon } from '../../../index';
import DetailDescriptionsItem from './DetailDescriptionsItem';
import './DetailHeader.scss';

export const ReplacedEntities = ({ids = []}:{ids?: number[]}) => {
    const intl = useIntl();
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
                    <div>{`${<FormattedMessage {...apDetailMessages.detailReplacingEntities} />}: `}</div>
                    {entities?.map((entity) => {
                        return <div>
                            <Link style={{color: "#33afff"}} to={urlEntity(entity.id)}> {`${entity.id}: ${entity.name || intl.formatMessage(apDetailMessages.detailReplacedEntityNoName)}`} </Link>
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
