import React, {FC} from 'react';
import DetailPart from './DetailPart';
//import DetailActionButton from "../DetailActionButton";
//import {faPlus, faTrash} from "@fortawesome/free-solid-svg-icons";
import {connect} from "react-redux";
import DetailRelatedPart from "./DetailRelatedPart";
import Icon from '../../shared/icon/Icon';
import {MOCK_CODE_DATA} from './mock';
import {ApPartVO} from "../../../api/ApPartVO";
import {ApValidationErrorsVO} from "../../../api/ApValidationErrorsVO";

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
    onAddRelated?: (part: ApPartVO) => void;
    onDeleteParts?: (parts: Array<ApPartVO>) => void;
    editMode?: boolean;
    globalEntity: boolean;
    deletableWholePart?: boolean;
    singlePart?: boolean;
    validationResult?: ApValidationErrorsVO;
}

const DetailMultiSection: FC<Props> = ({
                                           singlePart,
                                           editMode,
                                           label,
                                           parts,
                                           globalEntity,
                                           relatedParts = [],
                                           preferred,
                                           onSetPreferred,
                                           onDelete,
                                           onEdit,
                                           onAdd,
                                           onAddRelated,
                                           globalCollapsed,
                                           onDeleteParts,
                                           deletableWholePart,
                                           validationResult
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
        })
    }

    return (
        <div className="ml-3 mt-3 mr-3 border-bottom detail-multi-selection">
            <h3 className="mb-3">
                <span className="mr-1">{label}</span>
                {editMode && (!singlePart || (singlePart && parts.length === 0)) && <Icon
                    className="ml-1"
                    glyph={'fa-plus'}
                    onClick={() => onAdd && onAdd()}
                />}
                {editMode && !singlePart && deletableWholePart && parts.length > 0 && <Icon
                    className="ml-1"
                    glyph={'fa-trash'}
                    onClick={() => onDeleteParts && onDeleteParts(parts)}
                />}
            </h3>

            {/* TODO sort tak, aby preferred byly prvni  */}
            {[...parts].map((part, index) => {
                return <div key={index}>
                    <DetailPart
                        key={index}
                        part={part}
                        label={part.value}
                        singlePart={parts.length === 1}
                        editMode={editMode}
                        preferred={part.id === preferred}
                        globalCollapsed={globalCollapsed}
                        onDelete={onDelete}
                        onEdit={onEdit}
                        onAddRelated={onAddRelated}
                        onSetPreferred={onSetPreferred ? onSetPreferred : () => {
                            console.error("Není definován set preferred callback")
                        }}
                        validationResult={validationResult}
                        globalEntity={globalEntity}
                    />
                    {part.id && relatedPartsMap[part.id] && relatedPartsMap[part.id].map((part, index) => {
                        return <DetailRelatedPart
                            key={index}
                            part={part}
                            label={part.value}
                            editMode={editMode}
                            globalCollapsed={globalCollapsed}
                            onDelete={onDelete}
                            onEdit={onEdit}
                            validationResult={validationResult}
                            globalEntity={globalEntity}
                        />
                    })}
                </div>
            })}
            {parts.length > 0 && <div className={'pb-2'}/>}
        </div>
    );
};

DetailMultiSection.defaultProps = {
    deletableWholePart: false
};

const mapStateToProps = ({codelist}: any) => ({
    codelist: MOCK_CODE_DATA
});

export default connect(
    mapStateToProps
)(DetailMultiSection);