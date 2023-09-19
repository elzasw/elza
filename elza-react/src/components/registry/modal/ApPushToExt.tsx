import React, { useEffect, useState } from 'react';
import {Modal} from 'react-bootstrap';
import {Button} from "../../ui";
import i18n from "../../i18n";
import { ExtSystemFilterSectionFinal } from '../form/filter/ExtSystemFilterSection';
import './ApPushToExt.scss';
import { ApAccessPointVO } from 'api';
import { WebApi } from 'actions';
import { ApPartVO } from 'api/ApPartVO';
import { ApExternalSystemSimpleVO } from 'typings/store';
import { Form } from 'react-final-form';
import Icon from 'components/shared/icon/FontIcon';
import { Link } from 'react-router-dom';
import { AP_EXT_SYSTEM_TYPE, urlEntity } from '../../../constants';

const getRelatedEntitiesDetails = async (parts: ApPartVO[]) => {
    const relatedEntitiesIds: number[] = [];
    parts.forEach((part) => {
        const relatedPart:any = part.items?.find((item) => {
            return (item as any).accessPoint !== undefined;
        })
        if(relatedPart?.accessPoint?.id != undefined){
            if(relatedEntitiesIds.findIndex((id) => relatedPart.accessPoint.id === id) < 0){
                relatedEntitiesIds.push((relatedPart).accessPoint?.id);
            }
        }
    })

    return await getAccessPointsDetails(relatedEntitiesIds);
}

const getAccessPointsDetails = async (accessPointIds: number[]) => {
    return await Promise.all(accessPointIds.map((id) => WebApi.getAccessPoint(id)))
}
const getExternalSystemByCode = (extSystemCode: string, extSystems: ApExternalSystemSimpleVO[]) => extSystems.find((externalSystem) => {
    return externalSystem.code === extSystemCode;
});

type Props = {
    onClose: () => void;
    onSubmit: (data: {
        extSystemCode: string;
    }) => Promise<void>;
    extSystems: ApExternalSystemSimpleVO[];
    detail: ApAccessPointVO;
}

export const ApPushToExt = ({
    onClose,
    onSubmit,
    extSystems,
    detail,
}:Props) => {
    const [relatedEntities, setRelatedEntities] = useState<ApAccessPointVO[]>([])
    const [fetchingRelatedEntities, setFetchingRelatedEntities] = useState(false);

    const syncedExtSystemCode = detail.bindings.length === 1 ? detail.bindings[0].externalSystemCode : undefined;
    const syncedExtSystem = syncedExtSystemCode ? getExternalSystemByCode(syncedExtSystemCode, extSystems) : undefined;
 
    const defaultExternalSystemCode = extSystems.length === 1 ? extSystems[0]?.code : undefined;
    const hasBindings = detail.bindings.length > 0;
    const isExternalSystemSelectable = !hasBindings && defaultExternalSystemCode == undefined;

    // Check if related entities are synced with correct external system
    useEffect(() => {
        (async ()=> {
            if(detail){
                setFetchingRelatedEntities(true);
                const relatedEntities = await getRelatedEntitiesDetails(detail.parts)
                setFetchingRelatedEntities(false);
                setRelatedEntities(relatedEntities)
            }
        })()
    }, [detail]);

    const handleSubmit = ({extSystem}) => {
        if(extSystem){
            onSubmit({ extSystemCode: extSystem })
        }
    }

    // Entity has more than one synced external system
    if(detail.bindings.length > 1){
        return <>
            <Modal.Body>
                {i18n("ap.push-to-ext.multipleSyncs.message")}
            </Modal.Body>
            <Modal.Footer>
                <Button variant="link" onClick={onClose}>
                    {i18n('global.action.close')}
                </Button>
            </Modal.Footer>
        </>
    }

    // Synced external system is read only
    if(syncedExtSystem?.type === AP_EXT_SYSTEM_TYPE.CAM_UUID){
        return <>
            <Modal.Body>
                {i18n("ap.push-to-ext.readOnly.message")}
            </Modal.Body>
            <Modal.Footer>
                <Button variant="link" onClick={onClose}>
                    {i18n('global.action.close')}
                </Button>
            </Modal.Footer>
        </>
    }

    return <Form onSubmit={handleSubmit} initialValues={{extSystem: syncedExtSystemCode || defaultExternalSystemCode}}>
        {({submitting, values, handleSubmit}) => {
            const selectedExtSystem = getExternalSystemByCode(values.extSystem, extSystems);

            const unsyncedEntities = values.extSystem ? relatedEntities.filter((entity) => {
                return entity.bindings.length === 0 || entity.bindings.find((binding) => {
                    return binding.externalSystemCode !== values.extSystem;
                })
            }) : [];

            return <>
                <Modal.Body className="ap-push-to-ext-modal">
                    {unsyncedEntities.length > 0 && <div 
                        className="ap-validation-alert" 
                    >
                        <h3>
                            <b>{i18n('ap.push-to-ext.unsyncedEntities.title')}</b>
                        </h3>
                        <p>
                            {i18n('ap.push-to-ext.unsyncedEntities.message')}
                        </p>
                        <p>
                            {i18n('ap.push-to-ext.unsyncedEntities.listTitle')}
                        </p>
                        <ul>
                            {unsyncedEntities.map((unsyncedEntity) => {
                                return <li>
                                    <Link className="error-link" to={urlEntity(unsyncedEntity.id)} target="blank">
                                        {unsyncedEntity.name}
                                    </Link>
                                </li>
                            })}
                        </ul>
                    </div>}
                    {fetchingRelatedEntities && <div><Icon glyph="fa-circle-o-notch fa-spin"/></div>}
                    {isExternalSystemSelectable && <ExtSystemFilterSectionFinal name="extSystem" hideName={true} disabled={submitting} extSystems={extSystems}/> }
                    {selectedExtSystem && <div className="confirm-message">
                        <span dangerouslySetInnerHTML={{
                            __html: i18n("^ap.push-to-ext.selectedExtSystem.message", detail.name, selectedExtSystem?.name)
                        }}/>
                    </div>}
                </Modal.Body>
                <Modal.Footer>
                    <Button disabled={submitting} onClick={handleSubmit} variant="outline-secondary">{i18n('global.action.write')}</Button>
                    <Button variant="link" onClick={onClose} disabled={submitting}>
                        {i18n('global.action.cancel')}
                    </Button>
                </Modal.Footer>
            </>
        }} 
    </Form>;
}
