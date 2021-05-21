import i18n from 'components/i18n';
import { SmallButton } from 'components/shared/button/small-button';
import ValidationResultIcon from 'components/ValidationResultIcon';
import React, { FC } from 'react';
import { ApPartVO } from '../../../../api/ApPartVO';
import { ItemType } from '../../../../api/ApViewSettings';
import { PartValidationErrorsVO } from '../../../../api/PartValidationErrorsVO';
import { RulPartTypeVO } from '../../../../api/RulPartTypeVO';
import { Bindings } from '../../../../types';
import Icon from '../../../shared/icon/Icon';
import './DetailMultiSelection.scss';
import { DetailPartInfo } from '../part';

interface Props {
    label: string;
    parts: ApPartVO[];
    onEdit?: (part: ApPartVO) => void;
    onAdd?: () => void;
    editMode?: boolean;
    globalEntity: boolean;
    bindings: Bindings;
    partValidationErrors?: PartValidationErrorsVO[];
    itemTypeSettings: ItemType[];
    partType: RulPartTypeVO;
}

const DetailBodySection: FC<Props> = ({
    editMode,
    label,
    parts,
    globalEntity,
    onEdit = () => console.warn("Neni definovan 'onEdit' callback"),
    onAdd = () => console.warn("Neni definovan 'onAdd' callback"),
    // partValidationErrors = [],
    bindings,
    itemTypeSettings,
    partType,
    partValidationErrors = [],
}) => {
    if (!editMode && parts.length === 0) {
        return null;
    }

    const showValidationError = () => {
        return partValidationErrors.map((partValidationError, index) => {
            if (partValidationError?.errors && partValidationError.errors.length > 0) {
                return <ValidationResultIcon key={index} message={partValidationError.errors} />;
            }
        })
    };

    const hasInfo = (parts: ApPartVO[]) => {
        if(parts.length === 0) return false;
        if(parts.length === 1 && (parts[0].items?.length === 0 || parts[0].items === null)) return false;
        return true;
    }

    return (
        <div className="detail-multi-selection">
            <div className="detail-section-header" style={{display: "flex"}}>
                <span className="">{label}</span>
                <div className="actions" style={{fontSize: "0.8em", marginLeft: "10px"}}>
                    { editMode && (
                        <SmallButton 
                            title={i18n("ap.detail.edit", partType.name)}
                            onClick={()=> {
                                if(parts.length === 0) return onAdd()
                                if(parts.length === 1) return onEdit(parts[0])
                            }}
                        >
                            <Icon glyph={'fa-pencil'} />
                        </SmallButton>
                    )}
                </div>
                <div className="actions" style={{fontSize: "0.8em", marginLeft: "10px"}}>
                    {showValidationError()}
                </div>
            </div>

            <div className={`parts single-part`}>
                {!hasInfo(parts) ? <span>{i18n("ap.detail.noInfo")}</span> :
                parts.map((part, index) => {
                    if(part.items){
                        return (
                            <div key={index} className={`part`}>
                                <DetailPartInfo
                                    globalEntity={globalEntity}
                                    itemTypeSettings={itemTypeSettings}
                                    bindings={bindings}
                                    items={part.items}
                                    />
                            </div>
                        );
                    }
                })}
            </div>
        </div>
    );
};

export default DetailBodySection;
