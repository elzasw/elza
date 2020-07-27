import PropTypes from 'prop-types';
import React from 'react';
import AbstractReactComponent from '../AbstractReactComponent';
import DescItemType from './nodeForm/DescItemType';
import {objectEquals, objectEqualsDiff, propsEquals} from '../Utils';

class FormDescItemType extends AbstractReactComponent {
    shouldComponentUpdate(nextProps, nextState) {
        // return true;
        if (this.state !== nextState) {
            console.log('[FormDescItemType]#[state]##############', nextProps.refType.name);
            return true;
        }
        console.log('[FormDescItemType]###############', nextProps.refType.name, nextProps.descItemType.id);
        // return !propsEquals(this.props, nextProps, undefined, true);
        return !objectEqualsDiff(this.props, nextProps, undefined, '', true);
    }

    static propTypes = {
        descItemType: PropTypes.object.isRequired,
        descItemTypeIndex: PropTypes.number.isRequired,
        descItemGroupIndex: PropTypes.number.isRequired,
        nodeSetting: PropTypes.object.isRequired,

        refType: PropTypes.object.isRequired,
        infoType: PropTypes.object.isRequired,
        typePrefix: PropTypes.string.isRequired,
        singleDescItemTypeEdit: PropTypes.bool.isRequired,
        arrPerm: PropTypes.bool.isRequired,
        fundId: PropTypes.number.isRequired,
        strictMode: PropTypes.bool.isRequired,
        showNodeAddons: PropTypes.bool.isRequired,
        descItemCopyFromPrevEnabled: PropTypes.bool.isRequired,
        conformityInfo: PropTypes.object.isRequired,
        versionId: PropTypes.number.isRequired,
        readMode: PropTypes.bool.isRequired,
        calendarTypes: PropTypes.object.isRequired,
        structureTypes: PropTypes.object.isRequired,
        descItemFactory: PropTypes.func.isRequired,
        customActions: PropTypes.object,
        descItemRef: PropTypes.func.isRequired, // (key, ref) => void

        onCreateParty: PropTypes.func.isRequired,
        onDetailParty: PropTypes.func.isRequired,
        onCreateRecord: PropTypes.func.isRequired,
        onDetailRecord: PropTypes.func.isRequired,
        onCreateFile: PropTypes.func.isRequired,
        onFundFiles: PropTypes.func.isRequired,
        onDescItemAdd: PropTypes.func.isRequired,
        onCoordinatesUpload: PropTypes.func.isRequired,
        onJsonTableUpload: PropTypes.func.isRequired,
        onDescItemRemove: PropTypes.func.isRequired,
        onCoordinatesDownload: PropTypes.func.isRequired,
        onJsonTableDownload: PropTypes.func.isRequired,
        onChange: PropTypes.func.isRequired,
        onChangePosition: PropTypes.func.isRequired,
        onChangeSpec: PropTypes.func.isRequired,
        onBlur: PropTypes.func.isRequired,
        onFocus: PropTypes.func.isRequired,
        onDescItemTypeRemove: PropTypes.func.isRequired,
        onSwitchCalculating: PropTypes.func.isRequired,
        onDescItemTypeLock: PropTypes.func.isRequired,
        onDescItemTypeCopy: PropTypes.func.isRequired,
        onDescItemTypeCopyFromPrev: PropTypes.func.isRequired,
        onDescItemNotIdentified: PropTypes.func.isRequired,
    };

    isDescItemLocked(nodeSetting, descItemTypeId) {
        // existuje nastavení o JP - zamykání
        if (nodeSetting && nodeSetting.descItemTypeLockIds) {
            const index = nodeSetting.descItemTypeLockIds.indexOf(descItemTypeId);

            // existuje type mezi zamknutými
            if (index >= 0) {
                return true;
            }
        }
        return false;
    }

    render() {
        const {descItemType, descItemTypeIndex, descItemGroupIndex, nodeSetting} = this.props;

        const {
            refType,
            infoType,
            typePrefix,
            singleDescItemTypeEdit,
            arrPerm,
            fundId,
            strictMode,
            showNodeAddons,
            descItemCopyFromPrevEnabled,
            conformityInfo,
            versionId,
            readMode,
            calendarTypes,
            structureTypes,
            descItemFactory,
            customActions,
            descItemRef,
        } = this.props;

        console.log('[FormDescItemType] RENDER', refType.name);

        const {
            onCreateParty,
            onDetailParty,
            onCreateRecord,
            onDetailRecord,
            onCreateFile,
            onFundFiles,
            onDescItemAdd,
            onCoordinatesUpload,
            onJsonTableUpload,
            onDescItemRemove,
            onCoordinatesDownload,
            onJsonTableDownload,
            onChange,
            onChangePosition,
            onChangeSpec,
            onBlur,
            onFocus,
            onDescItemTypeRemove,
            onSwitchCalculating,
            onDescItemTypeLock,
            onDescItemTypeCopy,
            onDescItemTypeCopyFromPrev,
            onDescItemNotIdentified,
        } = this.props;

        const rulDataType = refType.dataType;

        let locked = this.isDescItemLocked(nodeSetting, descItemType.id);

        if (infoType.cal === 1) {
            locked = locked || !infoType.calSt;
        }

        let copy = false;

        // existují nějaké nastavení pro konkrétní node
        if (nodeSetting) {
            // existuje nastavení o JP - kopírování
            if (nodeSetting && nodeSetting.descItemTypeCopyIds) {
                const index = nodeSetting.descItemTypeCopyIds.indexOf(descItemType.id);

                // existuje type mezi kopírovanými
                if (index >= 0) {
                    copy = true;
                }
            }
        }

        let notIdentified = false;

        descItemType.descItems.forEach(descItem => {
            if (!descItemType.rep && descItem.undefined) {
                notIdentified = true;
            }
        });

        return (
            <DescItemType
                key={descItemType.id}
                typePrefix={typePrefix}
                ref={ref => {
                    descItemRef && descItemRef('descItemType' + descItemType.id, ref);
                }}
                descItemType={descItemType}
                singleDescItemTypeEdit={singleDescItemTypeEdit}
                refType={refType}
                infoType={infoType}
                rulDataType={rulDataType}
                calendarTypes={calendarTypes}
                structureTypes={structureTypes}
                onCreateParty={(descItemIndex, partyTypeId) =>
                    onCreateParty(descItemGroupIndex, descItemTypeIndex, descItemIndex, partyTypeId)
                }
                onDetailParty={(descItemIndex, partyId) =>
                    onDetailParty(descItemGroupIndex, descItemTypeIndex, descItemIndex, partyId)
                }
                onCreateRecord={descItemIndex => onCreateRecord(descItemGroupIndex, descItemTypeIndex, descItemIndex)}
                onDetailRecord={(descItemIndex, recordId) =>
                    onDetailRecord(descItemGroupIndex, descItemTypeIndex, descItemIndex, recordId)
                }
                onCreateFile={descItemIndex => onCreateFile(descItemGroupIndex, descItemTypeIndex, descItemIndex)}
                onFundFiles={descItemIndex => onFundFiles(descItemGroupIndex, descItemTypeIndex, descItemIndex)}
                onDescItemAdd={() => onDescItemAdd(descItemGroupIndex, descItemTypeIndex)}
                onCoordinatesUpload={file => onCoordinatesUpload(descItemType.id, file)}
                onJsonTableUpload={file => onJsonTableUpload(descItemType.id, file)}
                onDescItemRemove={descItemIndex =>
                    onDescItemRemove(descItemGroupIndex, descItemTypeIndex, descItemIndex)
                }
                onCoordinatesDownload={onCoordinatesDownload}
                onJsonTableDownload={onJsonTableDownload}
                onChange={(descItemIndex, value, error) =>
                    onChange(descItemGroupIndex, descItemTypeIndex, descItemIndex, value, error)
                }
                onChangePosition={(from, to) => onChangePosition(descItemGroupIndex, descItemTypeIndex, from, to)}
                onChangeSpec={(descItemIndex, specId) =>
                    onChangeSpec(descItemGroupIndex, descItemTypeIndex, descItemIndex, specId)
                }
                onBlur={descItemIndex => onBlur(descItemGroupIndex, descItemTypeIndex, descItemIndex)}
                onFocus={descItemIndex => onFocus(descItemGroupIndex, descItemTypeIndex, descItemIndex)}
                onDescItemTypeRemove={() => onDescItemTypeRemove(descItemGroupIndex, descItemTypeIndex)}
                onSwitchCalculating={() => onSwitchCalculating(descItemGroupIndex, descItemTypeIndex)}
                onDescItemTypeLock={notLock => onDescItemTypeLock(descItemType.id, notLock)}
                onDescItemTypeCopy={notCopy => onDescItemTypeCopy(descItemType.id, notCopy)}
                onDescItemTypeCopyFromPrev={() =>
                    onDescItemTypeCopyFromPrev(descItemGroupIndex, descItemTypeIndex, descItemType.id)
                }
                onDescItemNotIdentified={(descItemIndex, descItem) =>
                    onDescItemNotIdentified(descItemGroupIndex, descItemTypeIndex, descItemIndex, descItem)
                }
                showNodeAddons={showNodeAddons}
                locked={singleDescItemTypeEdit ? false : locked}
                closed={closed}
                copy={copy}
                conformityInfo={conformityInfo}
                descItemCopyFromPrevEnabled={descItemCopyFromPrevEnabled}
                versionId={versionId}
                fundId={fundId}
                readMode={readMode}
                arrPerm={arrPerm}
                strictMode={strictMode}
                notIdentified={notIdentified}
                customActions={customActions}
                descItemFactory={descItemFactory}
            />
        );
    }
}

export default FormDescItemType;
