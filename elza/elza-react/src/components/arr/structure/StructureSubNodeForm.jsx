import React from 'react';
import {AbstractReactComponent, i18n, Icon, Loading, NoFocusButton} from 'components/shared';
import {connect} from 'react-redux';

import '../NodeSubNodeForm.scss';
import SubNodeForm from '../SubNodeForm';
import {structureFormActions} from '../../../actions/arr/subNodeForm';
import PropTypes from 'prop-types';
import {getDescItemsAddTree, getOneSettings} from '../ArrUtils';
import AddDescItemTypeForm from '../nodeForm/AddDescItemTypeForm';
import {modalDialogShow} from '../../../actions/global/modalDialog';

import './StructureSubNodeForm.scss';
import objectById from '../../../shared/utils/objectById';

const BLANK_CONFORMITY_INFO = {missings: [], errors: []};

/**
 * Formulář detailu a editace jedné JP - jednoho NODE v konkrétní verzi.
 */
class StructureSubNodeForm extends React.Component {
    refForm = null;
    static propTypes = {
        versionId: PropTypes.number.isRequired,
        fundId: PropTypes.number.isRequired,
        id: PropTypes.number.isRequired,
        // Store
        rulDataTypes: PropTypes.object,
        structureTypes: PropTypes.object.isRequired,
        descItemTypes: PropTypes.object,
        subNodeForm: PropTypes.object,
        focus: PropTypes.object,
        readMode: PropTypes.bool,
        descItemFactory: PropTypes.func.isRequired,
    };

    static defaultProps = {
        readMode: false,
    };

    initFocus = () => {
        this.refForm.initFocus();
    };

    componentDidMount() {
        const {versionId, id} = this.props;
        this.props.dispatch(structureFormActions.fundSubNodeFormFetchIfNeeded(versionId, id, true));
    }

    UNSAFE_componentWillReceiveProps() {
        const {versionId, id} = this.props;
        this.props.dispatch(structureFormActions.fundSubNodeFormFetchIfNeeded(versionId, id));
    }

    /**
     * Zobrazení dialogu pro přidání atributu.
     */
    handleAddDescItemType = () => {
        const {versionId, id} = this.props;

        const descItemTypes = this.descItemTypeItems();

        // Modální dialog
        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('subNodeForm.descItemType.title.add'),
                <AddDescItemTypeForm
                    descItemTypes={descItemTypes}
                    onSubmitForm={data =>
                        this.props.dispatch(
                            structureFormActions.fundSubNodeFormDescItemTypeAdd(versionId, id, data.descItemTypeId.id),
                        )
                    }
                />,
            ),
        );
    };

    descItemTypeItems = () => {
        const {fund, subNodeForm, userDetail} = this.props;

        let {
            activeVersion: {strictMode},
        } = fund;

        const userStrictMode = getOneSettings(userDetail.settings, 'FUND_STRICT_MODE', 'FUND', fund.id);
        if (userStrictMode && userStrictMode.value !== null) {
            strictMode = userStrictMode.value === 'true';
        }
        return getDescItemsAddTree(
            subNodeForm.formData.descItemGroups,
            subNodeForm.infoTypesMap,
            subNodeForm.refTypesMap,
            subNodeForm.infoGroups,
            strictMode,
        );
    };

    render() {
        const {
            versionId,
            focus,
            fundId,
            rulDataTypes,
            structureTypes,
            descItemTypes,
            subNodeForm,
            readMode,
            id,
        } = this.props;

        if (!subNodeForm || !subNodeForm.fetched) {
            return <Loading />;
        }
        let conformityInfo = BLANK_CONFORMITY_INFO;
        if (subNodeForm.data && subNodeForm.data.parent && subNodeForm.data.parent.errorDescription) {
            try {
                const jsErr = JSON.parse(subNodeForm.data.parent.errorDescription);
                conformityInfo = {
                    errors: {},
                    missings: [],
                };
                for (let itemTypeId of jsErr.impossibleItemTypeIds) {
                    const descItemObjectIds = subNodeForm.data.descItems
                        .filter(i => i.itemTypeId === itemTypeId)
                        .map(i => ({
                            descItemObjectId: i.descItemObjectId,
                            description: i18n('arr.structure.conformityInfo.impossible'),
                        }));
                    for (let item of descItemObjectIds) {
                        if (!conformityInfo.errors.hasOwnProperty(item.descItemObjectId)) {
                            conformityInfo.errors[item.descItemObjectId] = [];
                        }
                        conformityInfo.errors[item.descItemObjectId].push(item);
                    }
                }
            } catch (e) {
                ///
            }
        }

        return (
            <div className="structure-item-form-container">
                {this.descItemTypeItems().length > 0 && !readMode && (
                    <NoFocusButton onClick={this.handleAddDescItemType}>
                        <Icon glyph="fa-plus-circle" />
                        {i18n('subNodeForm.section.item')}
                    </NoFocusButton>
                )}
                <SubNodeForm
                    ref={ref => (this.refForm = ref)}
                    typePrefix="structure"
                    versionId={versionId}
                    fundId={fundId}
                    routingKey={id}
                    nodeSetting={null}
                    rulDataTypes={rulDataTypes}
                    descItemTypes={descItemTypes}
                    structureTypes={structureTypes}
                    subNodeForm={subNodeForm}
                    closed={false}
                    conformityInfo={conformityInfo}
                    descItemCopyFromPrevEnabled={false}
                    focus={focus}
                    singleDescItemTypeId={null}
                    singleDescItemTypeEdit={false}
                    onDescItemTypeCopyFromPrev={() => {}}
                    onDescItemTypeLock={() => {}}
                    onDescItemTypeCopy={() => {}}
                    formActions={structureFormActions}
                    showNodeAddons={false}
                    readMode={readMode}
                    customActions={this.props.customActions}
                    descItemFactory={this.props.descItemFactory}
                />
            </div>
        );
    }
}

function mapStateToProps(state, props) {
    const {arrRegion, focus, refTables, userDetail, structures} = state;
    let fund = null;
    let structureTypes = objectById(refTables.structureTypes.data, null, 'versionId');
    if (arrRegion.activeIndex != null) {
        fund = arrRegion.funds[arrRegion.activeIndex];
    }

    const key = props.id ? String(props.id) : null;

    return {
        userDetail,
        fund,
        subNodeForm: key && structures.stores.hasOwnProperty(key) ? structures.stores[key].subNodeForm : null,
        focus,
        structureTypes,
        rulDataTypes: refTables.rulDataTypes,
        descItemTypes: refTables.descItemTypes,
    };
}

export default connect(mapStateToProps)(StructureSubNodeForm);
