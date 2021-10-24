import classNames from 'classnames';
import ValidationResultIcon from 'components/ValidationResultIcon';
import React, { FC, useEffect, useState } from 'react';
import { ApPartVO } from '../../../../api/ApPartVO';
import { ItemType } from '../../../../api/ApViewSettings';
import { PartValidationErrorsVO } from '../../../../api/PartValidationErrorsVO';
import { SyncState } from '../../../../api/SyncState';
import { Bindings } from '../../../../types';
import { SyncIcon } from "../sync-icon";
import './DetailPart.scss';
import { DetailPartInfo } from './DetailPartInfo';
import { PartName } from "./PartName";
import { RevisionDisplay } from './RevisionDisplay';

type Props = {
    label: string;
    part: ApPartVO;
    globalCollapsed: boolean;
    preferred?: boolean;
    globalEntity: boolean;
    partValidationError?: PartValidationErrorsVO;
    bindings: Bindings;
    itemTypeSettings: ItemType[];
    renderActions?: (part: ApPartVO) => React.ReactNode;
};

const DetailPart: FC<Props> = ({
    label,
    part,
    globalCollapsed = true,
    preferred,
    globalEntity,
    partValidationError,
    bindings,
    itemTypeSettings,
    renderActions = () => undefined,
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

    const partBinding = bindings.partsMap[part.id];
    const hasBinding = partBinding != null;
    const isModified = hasBinding && !partBinding;

    const isCollapsed = collapsed && !isModified;

    const classNameContent = classNames({
        'detail-part-preferred': preferred,
        'detail-part-expanded': !isCollapsed,
    });

    const areValuesEqual = (value: string, prevValue: string) => value === prevValue
    console.log(label)

    return (
        <div className="detail-part">
            <div className={classNameHeader}>
                <div style={{display: "flex", alignItems: "center"}}>
                    <RevisionDisplay 
                        valuesEqual={areValuesEqual(label, label)}
                        renderPrevValue={() => {
                            return <PartName 
                                label={label} 
                                collapsed={isCollapsed} 
                                preferred={preferred}
                                onClick={() => setCollapsed(!collapsed)}
                                />
                        }} 
                        renderValue={() => {
                            return <PartName 
                                label={label} 
                                collapsed={isCollapsed} 
                                preferred={preferred}
                                onClick={() => setCollapsed(!collapsed)}
                                />
                        }} 
                    >

                    </RevisionDisplay>
                    <div className="actions">
                        {hasBinding && (
                            <SyncIcon 
                                syncState={
                                !isModified ? 
                                SyncState.SYNC_OK : 
                                SyncState.LOCAL_CHANGE
                                }
                                />
                        )}
                        {showValidationError()}
                    </div>
                    <div className="actions hidable">
                        {renderActions(part)}
                    </div>
                </div>
            </div>

            {!isCollapsed && (
                <div className={classNameContent}>
                    <DetailPartInfo
                        items={part.items || []}
                        globalEntity={globalEntity}
                        bindings={bindings}
                        itemTypeSettings={itemTypeSettings}
                        />
                </div>
            )}
        </div>
    );
};

export default DetailPart;
