import i18n from 'components/i18n';
import { SmallButton } from 'components/shared/button/small-button';
import React, { FC } from 'react';
import { ApPartVO } from '../../../api/ApPartVO';
import { ItemType } from '../../../api/ApViewSettings';
import { PartValidationErrorsVO } from '../../../api/PartValidationErrorsVO';
import { RulPartTypeVO } from '../../../api/RulPartTypeVO';
import { Bindings } from '../../../types';
import Icon from '../../shared/icon/Icon';
import './DetailMultiSelection.scss';
import { DetailPartInfo } from './DetailPartInfo';

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
}) => {
    if (!editMode && parts.length === 0) {
        return null;
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
            </div>

            <div className={`parts single-part`}>
                {parts.length === 0 && <span>{i18n("ap.detail.noInfo")}</span>}
                {parts.map((part, index) => {
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
                })}
            </div>
        </div>
    );
};

export default DetailBodySection;
