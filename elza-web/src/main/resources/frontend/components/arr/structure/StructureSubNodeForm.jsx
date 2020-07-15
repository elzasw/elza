import React from 'react';
import ReactDOM from 'react-dom';
import {
    Icon,
    i18n,
    AbstractReactComponent,
    NoFocusButton,
    Loading
} from 'components/shared'
import {connect} from 'react-redux'

import '../NodeSubNodeForm.less';
import SubNodeForm from "../SubNodeForm";
import {structureFormActions} from "../../../actions/arr/subNodeForm";
import PropTypes from 'prop-types';
import {getDescItemsAddTree, getOneSettings} from "../ArrUtils";
import AddDescItemTypeForm from "../nodeForm/AddDescItemTypeForm";
import {modalDialogShow} from "../../../actions/global/modalDialog";

import './StructureSubNodeForm.less'
import objectById from "../../../shared/utils/objectById";

/**
 * Formulář detailu a editace jedné JP - jednoho NODE v konkrétní verzi.
 */
class StructureSubNodeForm extends AbstractReactComponent {

    static PropTypes = {
        versionId: PropTypes.number.isRequired,
        fundId: PropTypes.number.isRequired,
        selectedSubNodeId: PropTypes.number.isRequired,
        // Store
        rulDataTypes: PropTypes.object,
        structureTypes: PropTypes.object.isRequired,
        calendarTypes: PropTypes.object,
        descItemTypes: PropTypes.object,
        subNodeForm: PropTypes.object,
        focus: PropTypes.object,
        readMode: PropTypes.bool,
        descItemFactory: PropTypes.object.isRequired
    };

    static defaultProps = {
        readMode: false
    };

    initFocus = () => {
        this.refs.subNodeForm.getWrappedInstance().initFocus();
    };

    componentDidMount() {
        const {versionId, id} = this.props;
        this.props.dispatch(structureFormActions.fundSubNodeFormFetchIfNeeded(versionId, id, true));
    }

    componentWillReceiveProps() {
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
        this.props.dispatch(modalDialogShow(
            this,
            i18n('subNodeForm.descItemType.title.add'),
            <AddDescItemTypeForm
                descItemTypes={descItemTypes}
                onSubmitForm={(data) => this.props.dispatch(structureFormActions.fundSubNodeFormDescItemTypeAdd(versionId, id, data.descItemTypeId.id))}
            />
        ));
    };

    descItemTypeItems = () => {
        const {fund, subNodeForm, userDetail} = this.props;

        let {activeVersion:{strictMode}} = fund;

        const userStrictMode = getOneSettings(userDetail.settings, 'FUND_STRICT_MODE', 'FUND', fund.id);
        if (userStrictMode && userStrictMode.value !== null) {
            strictMode = userStrictMode.value === 'true';
        }
        return getDescItemsAddTree(subNodeForm.formData.descItemGroups, subNodeForm.infoTypesMap, subNodeForm.refTypesMap, subNodeForm.infoGroups, strictMode);
    };

    render() {
        const {versionId, focus, fundId, rulDataTypes, calendarTypes, structureTypes, descItemTypes, subNodeForm, readMode, id} = this.props;

        if (!subNodeForm || !subNodeForm.fetched) {
            return <Loading />
        }

        return (
            <div className="structure-item-form-container">
                {this.descItemTypeItems().length > 0 && !readMode && <NoFocusButton onClick={this.handleAddDescItemType}><Icon glyph="fa-plus-circle"/>{i18n('subNodeForm.section.item')}</NoFocusButton>}
                <SubNodeForm
                    ref="subNodeForm"
                    typePrefix="structure"
                    versionId={versionId}
                    fundId={fundId}
                    routingKey={id}
                    nodeSetting={null}
                    rulDataTypes={rulDataTypes}
                    calendarTypes={calendarTypes}
                    descItemTypes={descItemTypes}
                    structureTypes={structureTypes}
                    subNodeForm={subNodeForm}
                    closed={false}
                    conformityInfo={{missings: [], errors: []}}
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
        )
    }
}

function mapStateToProps(state, props) {
    const {arrRegion, focus, refTables, userDetail, structures} = state;
    let fund = null;
    let structureTypes = null;
    if (arrRegion.activeIndex != null) {
        fund = arrRegion.funds[arrRegion.activeIndex];
        structureTypes = objectById(refTables.structureTypes.data, fund.versionId, "versionId");
    }

    return {
        userDetail,
        fund,
        subNodeForm: structures.stores.hasOwnProperty(props.id) ? structures.stores[props.id].subNodeForm : null,
        focus,
        structureTypes,
        rulDataTypes: refTables.rulDataTypes,
        calendarTypes: refTables.calendarTypes,
        descItemTypes: refTables.descItemTypes,
    }
}

export default connect(mapStateToProps)(StructureSubNodeForm);
