import PropTypes, {bool, object} from 'prop-types';
import React from 'react';
import AbstractReactComponent from '../AbstractReactComponent';
import classNames from 'classnames';
import FormDescItemType from './FormDescItemType';

class FormDescItemGroup extends AbstractReactComponent {
    static propTypes = {
        descItemGroup: PropTypes.object.isRequired,
        descItemGroupIndex: PropTypes.number.isRequired,
        nodeSetting: PropTypes.object,
        singleDescItemTypeEdit: PropTypes.bool,
        singleDescItemTypeId: PropTypes.number,

        subNodeForm: PropTypes.object.isRequired,
        typePrefix: PropTypes.string.isRequired,
        arrPerm: PropTypes.bool,
        fundId: PropTypes.number.isRequired,
        strictMode: PropTypes.bool.isRequired,
        showNodeAddons: PropTypes.bool.isRequired,
        descItemCopyFromPrevEnabled: PropTypes.bool.isRequired,
        conformityInfo: PropTypes.object.isRequired,
        versionId: PropTypes.number.isRequired,
        readMode: PropTypes.bool.isRequired,
        structureTypes: PropTypes.object.isRequired,
        descItemFactory: PropTypes.func.isRequired,
        customActions: PropTypes.func,
        descItemRef: PropTypes.func.isRequired, // (key, ref) => void

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
        onDescItemInhibit: PropTypes.func.isRequired,
    };

    render() {
        const {
            descItemGroup,
            descItemGroupIndex,
            nodeSetting,
            singleDescItemTypeId,
            singleDescItemTypeEdit,
        } = this.props;
        const {
            typePrefix,
            subNodeForm,
            arrPerm,
            fundId,
            strictMode,
            showNodeAddons,
            descItemCopyFromPrevEnabled,
            conformityInfo,
            versionId,
            readMode,
            structureTypes,
            descItemFactory,
            customActions,
            descItemRef,
        } = this.props;

        const {
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
            onDescItemInhibit,
        } = this.props;

        const onFuncs = {
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
            onDescItemInhibit,
        };

        const descItemTypes = [];
        descItemGroup.descItemTypes.forEach((descItemType, descItemTypeIndex) => {
            const render =
                !singleDescItemTypeEdit || (singleDescItemTypeEdit && singleDescItemTypeId === descItemType.id);

            if (render) {
                // console.log(22222222, subNodeForm)
                const refType = subNodeForm.refTypesMap[descItemType.id];
                const infoType = subNodeForm.infoTypesMap[descItemType.id];
                const rulDataType = refType.dataType;
                const customActionsValue = customActions && customActions(rulDataType.code, infoType);

                descItemTypes.push(
                    <FormDescItemType
                        parentId={subNodeForm.nodeId}
                        key={descItemType.id}
                        descItemType={descItemType}
                        descItemTypeIndex={descItemTypeIndex}
                        descItemGroupIndex={descItemGroupIndex}
                        nodeSetting={nodeSetting}
                        fundId={fundId}
                        strictMode={strictMode}
                        descItemCopyFromPrevEnabled={descItemCopyFromPrevEnabled}
                        conformityInfo={conformityInfo}
                        versionId={versionId}
                        readMode={readMode}
                        structureTypes={structureTypes}
                        refType={refType}
                        infoType={infoType}
                        typePrefix={typePrefix}
                        singleDescItemTypeEdit={singleDescItemTypeEdit}
                        arrPerm={arrPerm}
                        showNodeAddons={showNodeAddons}
                        descItemFactory={descItemFactory}
                        customActions={customActionsValue}
                        descItemRef={descItemRef}
                        {...onFuncs}
                    />,
                );
                // const i = this.renderDescItemType(descItemType, descItemTypeIndex, descItemGroupIndex, nodeSetting);
                // descItemTypes.push(i);
            }
        });
        const cls = classNames({
            'desc-item-group': true,
            active: descItemGroup.hasFocus,
        });

        if (singleDescItemTypeEdit && descItemTypes.length === 0) {
            return null;
        }

        return (
            <div key={'type-' + descItemGroup.code + '-' + descItemGroupIndex} className={cls}>
                <div className="desc-item-types">{descItemTypes}</div>
            </div>
        );
    }
}

export default FormDescItemGroup;
