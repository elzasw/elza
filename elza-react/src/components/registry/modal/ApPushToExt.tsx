import React, { useEffect, useState } from 'react';
import {Modal} from 'react-bootstrap';
import {Button} from "../../ui";
import { FormattedMessage, defineMessages } from "react-intl";
import { globalMessages } from "components/shared/lang/messages";

// Id jsou převzatá z legacy katalogu beze změny. Zpráva se <b> není HTML, ale
// rich-text značky react-intl - handler se předá ve values, takže tu mizí
// dangerouslySetInnerHTML.
const messages = defineMessages({
    multipleSyncs: {
        id: "ap.push-to-ext.multipleSyncs.message",
        defaultMessage:
            "Nahrání do externího systému není možné pro entity, které mají napojen více než jeden externí systém.",
    },
    readOnly: {
        id: "ap.push-to-ext.readOnly.message",
        defaultMessage: "Externí systém napojený k vybrané entitě je určen pouze pro čtení.",
    },
    unsyncedTitle: {
        id: "ap.push-to-ext.unsyncedEntities.title",
        defaultMessage:
            "Odesílaný záznam odkazuje na jiné entity, které nejsou zapsány v cílovém systému. ",
    },
    unsyncedMessage: {
        id: "ap.push-to-ext.unsyncedEntities.message",
        defaultMessage: "Po potvrzení akce nedojde k odeslání všech vztahů. Záznam zůstane ve stavu Lokální změna nebo může dojít k celkové chybě odeslání.\nPro dosažení aktivní synchronizace je nutné odeslání všech souvisejících entit. Po jejich odeslání je třeba opětovně zapsat tuto entitu do externího systému.",
    },
    unsyncedListTitle: {
        id: "ap.push-to-ext.unsyncedEntities.listTitle",
        defaultMessage: "Dotčené části záznamu entity:",
    },
    selectedExtSystem: {
        id: "ap.push-to-ext.selectedExtSystem.message",
        defaultMessage: "Přejete si zapsat entitu <b>{0}</b> do systému <b>{1}</b>?",
    },
});
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
                <FormattedMessage {...messages.multipleSyncs} />
            </Modal.Body>
            <Modal.Footer>
                <Button variant="link" onClick={onClose}>
                    <FormattedMessage {...globalMessages.close} />
                </Button>
            </Modal.Footer>
        </>
    }

    // Synced external system is read only
    if(syncedExtSystem?.type === AP_EXT_SYSTEM_TYPE.CAM_UUID){
        return <>
            <Modal.Body>
                <FormattedMessage {...messages.readOnly} />
            </Modal.Body>
            <Modal.Footer>
                <Button variant="link" onClick={onClose}>
                    <FormattedMessage {...globalMessages.close} />
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
                            <b><FormattedMessage {...messages.unsyncedTitle} /></b>
                        </h3>
                        <p>
                            <FormattedMessage {...messages.unsyncedMessage} />
                        </p>
                        <p>
                            <FormattedMessage {...messages.unsyncedListTitle} />
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
                        <FormattedMessage
                            {...messages.selectedExtSystem}
                            values={{ b: (chunks) => <b>{chunks}</b>, 0: detail.name, 1: selectedExtSystem?.name }}
                        />
                    </div>}
                </Modal.Body>
                <Modal.Footer>
                    <Button disabled={submitting} onClick={handleSubmit} variant="outline-secondary"><FormattedMessage {...globalMessages.write} /></Button>
                    <Button variant="link" onClick={onClose} disabled={submitting}>
                        <FormattedMessage {...globalMessages.cancel} />
                    </Button>
                </Modal.Footer>
            </>
        }} 
    </Form>;
}
