import i18n from 'components/i18n';
import { SmallButton } from 'components/shared/button/small-button';
import ValidationResultIcon from 'components/ValidationResultIcon';
import React, { FC } from 'react';
// import { ApPartVO } from '../../../../api/ApPartVO';
import { ItemType } from '../../../../api/ApViewSettings';
import { PartValidationErrorsVO } from '../../../../api/PartValidationErrorsVO';
import { RulPartTypeVO } from '../../../../api/RulPartTypeVO';
import { Bindings } from '../../../../types';
import Icon from '../../../shared/icon/Icon';
import './DetailMultiSelection.scss';
import { DetailPartInfo } from '../part';
import { RevisionPart, getRevisionItems } from '../../revision';

interface Props {
    label: string;
    part: RevisionPart;
    onEdit?: (part: RevisionPart) => void;
    onAdd?: () => void;
    onDelete?: (part: RevisionPart) => void;
    onRevert?: (part: RevisionPart) => void;
    editMode?: boolean;
    globalEntity: boolean;
    bindings: Bindings;
    partValidationErrors?: PartValidationErrorsVO[];
    itemTypeSettings: ItemType[];
    partType: RulPartTypeVO;
    revision?: boolean;
    select: boolean;
}

const DetailBodySection: FC<Props> = ({
    editMode,
    label,
    part,
    globalEntity,
    onEdit = () => console.warn("Neni definovan 'onEdit' callback"),
    // onAdd = () => console.warn("Neni definovan 'onAdd' callback"),
    onDelete = () => console.warn("Neni definovan 'onDelete' callback"),
    // partValidationErrors = [],
    onRevert = () =>  console.warn("Neni definovan 'onRevert' callback"),
    bindings,
    itemTypeSettings,
    partType,
    partValidationErrors = [],
    revision,
    select,
}) => {
    if (!editMode && !part) {
        return null;
    }

    const showValidationError = () => {
        return partValidationErrors.map((partValidationError, index) => {
            if (partValidationError?.errors && partValidationError.errors.length > 0) {
                return <ValidationResultIcon key={index} message={partValidationError.errors} />;
            }
        })
    };

    const hasInfo = (part: RevisionPart) => {
        if(
            part.part?.items?.length === 0 
            || part.part?.items === null
            || part.updatedPart?.items?.length === 0
            || part.updatedPart?.items === null
        ) return false;
        return true;
    }

    const isModified = (part.part != null && part.updatedPart != null);
    const revisionItems = getRevisionItems(part.part?.items || undefined, part.updatedPart?.items || undefined);

    const renderPartActions = (part: RevisionPart) => {
        const isDeleted = part.updatedPart?.changeType === "DELETED";
        return <>
            {editMode && !isDeleted &&
                <>
                    <SmallButton 
                        title={i18n("ap.detail.edit", partType.name)}
                        onClick={()=> {
                            // if(part.length === 0) return onAdd()
                            if(part) return onEdit(part)
                        }}
                    >
                        <Icon glyph={'fa-pencil'} />
                    </SmallButton>
                    <SmallButton title={i18n("ap.detail.delete")} onClick={()=> onDelete(part)}>
                        <Icon glyph="fa-trash"/>
                    </SmallButton>
                </>
            }
            {isDeleted &&
                <SmallButton title={i18n("ap.detail.revert")} onClick={()=> onRevert(part)}>
                    <Icon glyph="fa-undo" />
                </SmallButton>
            }
        </>
    }

    return (
        <div className="detail-multi-selection">
            <div className="detail-section-header" style={{display: "flex"}}>
                <span className="">{label}</span>
                <div className="actions" style={{fontSize: "0.8em", marginLeft: "10px"}}>
                    {part && renderPartActions(part)}
                </div>
                <div className="actions" style={{fontSize: "0.8em", marginLeft: "10px"}}>
                    {showValidationError()}
                </div>
            </div>

            <div className={`parts single-part`}>
                {!hasInfo(part) ? <span className="no-info-msg">{i18n("ap.detail.noInfo")}</span> :
                <div className={`part`}>
                    <DetailPartInfo
                        select={select}
                        globalEntity={globalEntity}
                        itemTypeSettings={itemTypeSettings}
                        bindings={bindings}
                        items={revisionItems}
                        isModified={isModified}
                        revision={revision}
                        />
                </div>
                }
            </div>
        </div>
    );
};

export default DetailBodySection;
