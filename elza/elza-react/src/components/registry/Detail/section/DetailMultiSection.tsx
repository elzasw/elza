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

interface Props {
    label: string;
    parts: ApPartVO[];
    relatedParts?: ApPartVO[];
    preferred?: number;
    globalCollapsed: boolean;
    onSetPreferred?: (part: ApPartVO) => void;
    onDelete?: (part: ApPartVO) => void;
    onEdit?: (part: ApPartVO) => void;
    onAdd?: () => void;
    onAddRelated?: (parentPartId: number) => void;
    editMode?: boolean;
    globalEntity: boolean;
    singlePart?: boolean;
    bindings: Bindings;
    partValidationErrors?: PartValidationErrorsVO[];
    itemTypeSettings: ItemType[];
    partType: RulPartTypeVO;
}

const DetailMultiSection: FC<Props> = ({
    singlePart,
    editMode,
    label,
    parts,
    globalEntity,
    relatedParts = [],
    preferred,
    onSetPreferred = () => console.error('Není definován set preferred callback'),
    onDelete = () => console.warn("Neni definovan 'onDelte' callback"),
    onEdit = () => console.warn("Neni definovan 'onEdit' callback"),
    onAdd = () => console.warn("Neni definovan 'onAdd' callback"),
    onAddRelated,
    globalCollapsed,
    partValidationErrors,
    bindings,
    itemTypeSettings,
    partType
}) => {
    if (!editMode && parts.length === 0) {
        return null;
    }

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

    const renderPartActions = (part: ApPartVO, forceRender: boolean = false) => {
        if(singlePart && !forceRender){
            return undefined;
        }

        let showPreferredSwitch = false;
        if (part.typeId === partType.id && partType?.code === 'PT_NAME') {
            showPreferredSwitch = !singlePart;
        }
        const isPreferred = part.id === preferred;

        return <>
            {editMode &&
                <>
                    {showPreferredSwitch && part.id !== preferred && (
                        <SmallButton title={i18n("ap.detail.setPreferred")} onClick={()=> onSetPreferred(part)}>
                            <Icon glyph={'fa-star'} />
                        </SmallButton>
                    )}
                    <SmallButton title={i18n("ap.detail.edit", "")} onClick={()=> onEdit(part)}>
                        <Icon glyph="fa-pencil" />
                    </SmallButton>
                    {!isPreferred &&
                        <SmallButton title={i18n("ap.detail.delete")} onClick={()=> onDelete(part)}>
                            <Icon glyph="fa-trash"/>
                        </SmallButton>}
                    {onAddRelated && (
                        <SmallButton title={i18n("ap.detail.add.related")} onClick={() => onAddRelated(part.id)}>
                            <Icon glyph="fa-link"/>
                        </SmallButton>
                    )}
                </>
            }
        </>
    }

    return (
        <div className="detail-multi-selection">
            <div className="detail-section-header" style={{display: "flex"}}>
                <span className="">{label}</span>
                <div className="actions" style={{fontSize: "0.8em", marginLeft: "10px"}}>
                    {renderHeaderActions()}
                    { singlePart && parts.length === 1 &&
                        renderPartActions(parts[0], true)
                    }
                </div>
            </div>

            <div className={`parts ${singlePart ? "single-part" : ""}`}>
            {parts.length === 0 && <span className="no-info-msg">{i18n("ap.detail.noInfo")}</span>}
            {parts.map((part, index) => {
                    const relatedParts = part.id != null && relatedPartsMap[part.id] ? relatedPartsMap[part.id] : []
                    return (
                        <div key={index} className={`part ${part.id === preferred ? "preferred" : ""}`}>
                            {!singlePart && <div className="bracket"/>}
                            <DetailPart
                                key={index}
                                part={part}
                                label={part.value}
                                preferred={part.id === preferred}
                                globalCollapsed={globalCollapsed}
                                partValidationError={part.id && objectById(partValidationErrors, part.id)}
                                globalEntity={globalEntity}
                                bindings={bindings}
                                itemTypeSettings={itemTypeSettings}
                                renderActions={renderPartActions}
                                />
                            {relatedParts.length > 0 &&
                                <div className="related-parts">
                                    {relatedParts.map((relatedPart, index) => {
                                        return (
                                            <DetailRelatedPart
                                                key={index}
                                                part={relatedPart}
                                                label={relatedPart.value}
                                                editMode={editMode}
                                                globalCollapsed={globalCollapsed}
                                                onDelete={onDelete}
                                                onEdit={onEdit}
                                                partValidationError={relatedPart.id && objectById(partValidationErrors, relatedPart.id)}
                                                globalEntity={globalEntity}
                                                bindings={bindings}
                                                itemTypeSettings={itemTypeSettings}
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
