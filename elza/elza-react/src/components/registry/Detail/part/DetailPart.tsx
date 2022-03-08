import classNames from 'classnames';
import ValidationResultIcon from 'components/ValidationResultIcon';
import React, { FC, useEffect, useState } from 'react';
// import { ApPartVO } from '../../../../api/ApPartVO';
import { ItemType } from '../../../../api/ApViewSettings';
import { PartValidationErrorsVO } from '../../../../api/PartValidationErrorsVO';
import { SyncState } from '../../../../api/SyncState';
import { Bindings } from '../../../../types';
import { SyncIcon } from "../sync-icon";
import './DetailPart.scss';
import { DetailPartInfo } from './DetailPartInfo';
import { PartName } from "./PartName";
import { RevisionDisplay, RevisionPart, getRevisionItems } from '../../revision';

type Props = {
    part: RevisionPart;
    globalCollapsed: boolean;
    preferred?: boolean;
    newPreferred?: boolean;
    oldPreferred?: boolean;
    revision?: boolean;
    globalEntity: boolean;
    partValidationError?: PartValidationErrorsVO;
    bindings: Bindings;
    itemTypeSettings: ItemType[];
    renderActions?: (part?: RevisionPart) => React.ReactNode;
    select: boolean;
};

const DetailPart: FC<Props> = ({
    part: {part, updatedPart},
    globalCollapsed = true,
    preferred,
    newPreferred,
    oldPreferred,
    revision,
    globalEntity,
    partValidationError,
    bindings,
    itemTypeSettings,
    renderActions = () => undefined,
    select,
}) => {
    const [collapsed, setCollapsed] = useState(globalCollapsed);


    useEffect(() => {
        setCollapsed(globalCollapsed);
    }, [globalCollapsed]);

    const classNameHeader = classNames('detail-part-header', {
        'detail-part-preferred': preferred,
    });


    // Rozbalený content


    const showValidationError = () => {
        if (partValidationError?.errors && partValidationError.errors.length > 0) {
            return <ValidationResultIcon message={partValidationError.errors} />;
        }
    };

    const partBinding = part ? bindings.partsMap[part.id] : false;
    const updatedPartBinding = updatedPart ? bindings.partsMap[updatedPart.id] : false;
    // const hasBinding = partBinding != null;
    // const hasLocalChange = hasBinding && !partBinding;
    const isRevisionModified = updatedPart?.changeType === "UPDATED";
    const isCollapsed = collapsed && !isRevisionModified;
    const isDeleted = updatedPart?.changeType === "DELETED";
    const isNew = updatedPart?.changeType === "NEW";

    const classNameContent = classNames({
        'detail-part-preferred': preferred,
        'detail-part-expanded': !isCollapsed,
    });

    const areValuesEqual = (value: string, prevValue: string) => value === prevValue

    // const items = getRevisionItems(part?.items || [], updatedPart?.items || []);
            const items = getRevisionItems(
                    revision ? part?.items || [] : undefined, 
                    revision ? updatedPart?.items || [] : part?.items || [])

    return (
        <div className="detail-part">
            <div className={classNameHeader}>
                <div style={{display: "flex", alignItems: "center"}}>
                    <RevisionDisplay 
                        disableRevision={!revision}
                        isDeleted={isDeleted}
                        isNew={isNew}
                        valuesEqual={areValuesEqual(part?.value || "", updatedPart ? updatedPart.value : part?.value || "")}
                        renderPrevValue={() => {
                            return <div style={{display: "flex"}}>
                                <PartName
                                    label={part?.value || "no value"}
                                    collapsed={isCollapsed}
                                    preferred={preferred}
                                    oldPreferred={oldPreferred}
                                    newPreferred={newPreferred}
                                    onClick={() => setCollapsed(!collapsed)}
                                    binding={partBinding}
                                    />

                                </div>
                        }}
                        renderValue={() => {
                            return <PartName
                                label={ updatedPart ? updatedPart.value : part?.value || "no new value"}
                                collapsed={isCollapsed}
                                preferred={preferred}
                                oldPreferred={oldPreferred}
                                newPreferred={newPreferred}
                                onClick={() => setCollapsed(!collapsed)}
                                binding={updatedPart ? updatedPartBinding : partBinding}
                                />
                        }}
                    >

                    </RevisionDisplay>
                    <div className="actions">
                        {showValidationError()}
                    </div>
                    <div className="actions hidable">
                        {renderActions({part, updatedPart})}
                    </div>
                </div>
            </div>

            {!isCollapsed && (
                <div className={classNameContent}>
                    <DetailPartInfo
                        select={select}
                        items={items}
                        globalEntity={globalEntity}
                        bindings={bindings}
                        itemTypeSettings={itemTypeSettings}
                        isModified={isRevisionModified || isDeleted}
                        revision={revision}
                        />
                </div>
            )}
        </div>
    );
};

export default DetailPart;
