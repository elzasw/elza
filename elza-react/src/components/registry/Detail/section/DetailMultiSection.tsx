import i18n from 'components/i18n';
import { SmallButton } from 'components/shared/button/small-button';
import React, { FC } from 'react';
import { ApPartVO } from '../../../../api/ApPartVO';
import { ItemType } from '../../../../api/ApViewSettings';
import { PartValidationErrorsVO } from '../../../../api/PartValidationErrorsVO';
import { RulPartTypeVO } from '../../../../api/RulPartTypeVO';
import { objectById } from '../../../../shared/utils';
import { Bindings } from '../../../../types';
import Icon from '../../../shared/icon/Icon';
import { DetailPart, DetailRelatedPart } from '../part';
import './DetailMultiSelection.scss';
import { RevisionPart } from '../../revision';

interface Props {
    label: string;
    parts: RevisionPart[];
    relatedParts?: RevisionPart[];
    preferred?: number;
    newPreferred?: number;
    revPreferred?: number;
    revision?: boolean;
    globalCollapsed: boolean;
    onSetPreferred?: (part: RevisionPart) => void;
    onDelete?: (part: RevisionPart) => void;
    onEdit?: (part: RevisionPart) => void;
    onAdd?: () => void;
    onAddRelated?: (parentPartId?: number, revParentPartId?: number) => void;
    onRevert?: (part: RevisionPart) => void;
    editMode?: boolean;
    globalEntity: boolean;
    singlePart?: boolean;
    bindings: Bindings;
    partValidationErrors?: PartValidationErrorsVO[];
    itemTypeSettings: ItemType[];
    // Typ vykreslovaneho partu
    // vsechny party jsou tohoto typu
    partType: RulPartTypeVO;
    select: boolean;
}

const DetailMultiSection: FC<Props> = ({
    singlePart,
    editMode,
    label,
    parts,
    globalEntity,
    relatedParts = [],
    preferred,
    newPreferred,
    revPreferred,
    revision,
    onSetPreferred = () => console.error('Není definován set preferred callback'),
    onDelete = () => console.warn("Neni definovan 'onDelte' callback"),
    onEdit = () => console.warn("Neni definovan 'onEdit' callback"),
    onAdd = () => console.warn("Neni definovan 'onAdd' callback"),
    onRevert = () => console.warn("Neni definovan 'onRevert' callback"),
    onAddRelated,
    globalCollapsed,
    partValidationErrors,
    bindings,
    itemTypeSettings,
    partType,
    select,
}) => {
    if (!editMode && parts.length === 0) {
        return null;
    }

    /*
    let relatedPartsMap: Record<number, ApPartVO[]> = {};
    if (relatedParts && relatedParts.length > 0) {
        relatedParts.forEach(rp => {
            if (rp.partParentId) {
                if (!relatedPartsMap[rp.partParentId]) {
                    relatedPartsMap[rp.partParentId] = [];
                }
                relatedPartsMap[rp.partParentId].push(rp);
            }
        });
    }
    */

    const groupRelatedPartsByParent = (data: RevisionPart[]):Record<string, RevisionPart[]> =>
    data.reduce<Record<string, RevisionPart[]>>((accumulator, value) => {
        const parentId = value.part?.partParentId || value.updatedPart?.partParentId;
        const revParentId = value.part?.revPartParentId || value.updatedPart?.revPartParentId;

        if(revParentId != undefined){
            const currentValue = accumulator[revParentId] || [];
            accumulator[revParentId.toString()] = [...currentValue, value];
        } 
        else if(parentId != undefined){
            const currentValue = accumulator[parentId] || [];
            accumulator[parentId.toString()] = [...currentValue, value];
        }
        return accumulator;
    }, {});

    const relatedRevisionPartsMap = groupRelatedPartsByParent(relatedParts)

    const renderHeaderActions = () => {
        return <>
            {editMode && <>
                {!singlePart && (
                    <SmallButton title={i18n("ap.detail.add", partType.name)} onClick={() => onAdd()}>
                        <Icon glyph="fa-plus"/>
                    </SmallButton>
                )}
                {singlePart && parts.length === 0 &&
                    <SmallButton title={i18n("ap.detail.edit", partType.name)} onClick={()=> onAdd()}>
                        <Icon glyph="fa-pencil"/>
                    </SmallButton>
                }
                </>
            }
            </>
    }

    const isPartPreferred = (part: ApPartVO | undefined, updatedPart: ApPartVO | undefined) => {
        let isPreferred;
        if (revPreferred) {
            isPreferred = updatedPart ? updatedPart.id === revPreferred : false;
        } else if (newPreferred) {
            isPreferred = part ? part.id === newPreferred : false;
        } else {
            isPreferred = part ? part.id === preferred : false;
        }
        return isPreferred;
    };

    const isPartOldPreferred = (part: ApPartVO | undefined) => {
        let isOldPreferred;
        if (revPreferred || newPreferred) {
            isOldPreferred = part ? part.id === preferred && newPreferred !== preferred : false;
        } else {
            isOldPreferred = false;
        }
        return isOldPreferred;
    };

    const isPartNewPreferred = (part: ApPartVO | undefined, updatedPart: ApPartVO | undefined) => {
        let isNewPreferred;
        if (newPreferred) {
            isNewPreferred = part ? part.id === newPreferred && newPreferred !== preferred : false;
        } else if (revPreferred) {
            isNewPreferred = updatedPart ? updatedPart.id === revPreferred : false;
        } else {
            isNewPreferred = false;
        }
        return isNewPreferred;
    };

    const renderPartActions = (part: RevisionPart, forceRender: boolean = false) => {

        // mozna zbytecne?? - jen kontrola vstupu
        if(!part.updatedPart && !part.part){
            return;
        }

        // Nastaveni jako preferovane se zobrazi jen u jmena
        const showPreferredSwitch = partType.code === 'PT_NAME';
        const isPreferred = isPartPreferred(part.part, part.updatedPart);
        
        const isDeleted = part.updatedPart?.changeType === "DELETED";
        const isModified = part.updatedPart?.changeType === "UPDATED";
        
        const partId = part.part?.id;
        const revPartId = part.updatedPart?.id;

        return <>
            {editMode &&
                <>
                    {showPreferredSwitch && !isPreferred && !isDeleted && (
                        <SmallButton title={i18n("ap.detail.setPreferred")} onClick={()=> onSetPreferred(part)}>
                            <Icon glyph={'fa-star'} />
                        </SmallButton>
                    )}
                    {!isDeleted &&
                        <SmallButton title={i18n("ap.detail.edit", "")} onClick={()=> onEdit(part)}>
                            <Icon glyph="fa-pencil" />
                        </SmallButton>
                    }
                    {!isDeleted && onAddRelated && (
                        <SmallButton 
                            title={i18n("ap.detail.add.related")} 
                            onClick={() => onAddRelated(partId, revPartId)}
                        >
                            <Icon glyph="fa-link"/>
                        </SmallButton>
                    )}
                    {!isPreferred && !isDeleted &&
                        <SmallButton title={i18n("ap.detail.delete")} onClick={()=> onDelete(part)}>
                            <Icon glyph="fa-trash"/>
                        </SmallButton>}
                    {(isDeleted || isModified) &&
                        <SmallButton title={i18n("ap.detail.revert")} onClick={()=> onRevert(part)}>
                            <Icon glyph="fa-undo" />
                        </SmallButton>
                    }
                </>
            }
        </>
    }

    const firstPart = parts.length > 0 && parts[0];

    return (
        <div className="detail-multi-selection">
            <div className="detail-section-header" style={{display: "flex"}}>
                <span className="">{label}</span>
                <div className="actions" style={{fontSize: "0.8em", marginLeft: "10px"}}>
                    {renderHeaderActions()}
                    { /*singlePart && firstPart &&
                        renderPartActions(firstPart, true)
                    */
                    }
                </div>
            </div>

            <div className={`parts ${singlePart ? "single-part" : ""}`}>
            {parts.length === 0 && <span className="no-info-msg">{i18n("ap.detail.noInfo")}</span>}
            {parts.map(({part, updatedPart}, index) => {
                    const relatedParts = part?.id != null && relatedRevisionPartsMap[part.id] ? relatedRevisionPartsMap[part.id] : [];
                    const revRelatedParts = updatedPart?.id != null && relatedRevisionPartsMap[updatedPart.id] ? relatedRevisionPartsMap[updatedPart.id] : [];
                    let isPreferred = isPartPreferred(part, updatedPart);
                    let isOldPreferred = isPartOldPreferred(part);
                    let isNewPreferred = isPartNewPreferred(part, updatedPart);
                    console.log("related parts", relatedParts, revRelatedParts)
                    return (
                        <div key={index} className={`part ${isPreferred ? "preferred" : ""}`}>
                            {!singlePart && <div className="bracket"/>}
                            <DetailPart
                                key={index}
                                part={{part, updatedPart}}
                                preferred={isPreferred}
                                oldPreferred={isOldPreferred}
                                newPreferred={isNewPreferred}
                                revision={revision}
                                globalCollapsed={globalCollapsed}
                                partValidationError={part?.id && objectById(partValidationErrors, part.id)}
                                globalEntity={globalEntity}
                                bindings={bindings}
                                itemTypeSettings={itemTypeSettings}
                                renderActions={renderPartActions}
                                select={select}
                                />
                            {[...relatedParts, ...revRelatedParts].length > 0 &&
                                <div className="related-parts">
                                    {[...relatedParts, ...revRelatedParts].map(({part, updatedPart}, index) => {
                                        return (
                                            <DetailRelatedPart
                                                key={index}
                                                part={{part, updatedPart}}
                                                label={updatedPart ? updatedPart.value : part?.value as any}
                                                editMode={editMode}
                                                globalCollapsed={globalCollapsed}
                                                onDelete={onDelete}
                                                onEdit={onEdit}
                                                partValidationError={part?.id && objectById(partValidationErrors, part.id)}
                                                globalEntity={globalEntity}
                                                bindings={bindings}
                                                itemTypeSettings={itemTypeSettings}
                                                revision={revision}
                                                select={select}
                                                onRevert={onRevert}
                                                />
                                        );
                                    })}
                                </div>
                            }
                        </div>
                    );
                })}
            </div>
        </div>
    );
};

export default DetailMultiSection;
