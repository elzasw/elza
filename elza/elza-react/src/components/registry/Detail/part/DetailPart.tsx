import classNames from 'classnames';
import ValidationResultIcon from 'components/ValidationResultIcon';
import React, { FC, useEffect, useState } from 'react';
import { ApPartVO } from '../../../../api/ApPartVO';
import { ItemType } from '../../../../api/ApViewSettings';
import { PartValidationErrorsVO } from '../../../../api/PartValidationErrorsVO';
import { Bindings } from '../../../../types';
import i18n from '../../../i18n';
import Icon from '../../../shared/icon/Icon';
import './DetailPart.scss';
import { DetailPartInfo } from './DetailPartInfo';
import { PartName } from "./PartName";
import { SyncIcon } from "../sync-icon";
import { SyncState } from '../../../../api/SyncState';

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
    const classNameContent = classNames({
        'detail-part-preferred': preferred,
        'detail-part-expanded': !collapsed,
    });


    const showValidationError = () => {
        if (partValidationError?.errors && partValidationError.errors.length > 0) {
            return <ValidationResultIcon message={partValidationError.errors} />;
        }
    };

    const partBinding = bindings.partsMap[part.id];

    return (
        <div className="detail-part">
            <div className={classNameHeader}>
                <div style={{display: "flex", alignItems: "center"}}>
                    <PartName 
                        label={label} 
                        collapsed={collapsed} 
                        preferred={preferred}
                        onClick={() => setCollapsed(!collapsed)}
                    />
                    <div className="actions">
                        {partBinding != null && (
                            <SyncIcon 
                                syncState={
                                    partBinding ? 
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

            {!collapsed && (
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
