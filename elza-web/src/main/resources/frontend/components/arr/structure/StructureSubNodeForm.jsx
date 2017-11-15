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
        calendarTypes: PropTypes.object,
        descItemTypes: PropTypes.object,
        subNodeForm: PropTypes.object,
        focus: PropTypes.object,
    };

    initFocus = () => {
        this.refs.subNodeForm.getWrappedInstance().initFocus();
    };

    componentDidMount() {
        const {versionId} = this.props;
        this.props.dispatch(structureFormActions.fundSubNodeFormFetchIfNeeded(versionId, null));
    }

    render() {
        const {versionId, focus, fundId, rulDataTypes, packetTypes, calendarTypes, descItemTypes, subNodeForm} = this.props;

        if (!subNodeForm || !subNodeForm.fetched) {
            return <Loading />
        }

        return (
            <div className="output-item-form-container">
                <SubNodeForm
                    ref="subNodeForm"
                    typePrefix="structure"
                    versionId={versionId}
                    fundId={fundId}
                    routingKey={null}
                    nodeSetting={null}
                    rulDataTypes={rulDataTypes}
                    calendarTypes={calendarTypes}
                    descItemTypes={descItemTypes}
                    packetTypes={packetTypes}
                    packets={null}
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
                    readMode={false}
                />
            </div>
        )
    }
}

function mapStateToProps(state) {
    const {arrRegion, focus, refTables} = state;
    let fund = null;
    if (arrRegion.activeIndex != null) {
        fund = arrRegion.funds[arrRegion.activeIndex];
    }

    return {
        subNodeForm: fund ? fund.structureNodeForm.subNodeForm : null,
        focus,
        packetTypes: refTables.packetTypes,
        rulDataTypes: refTables.rulDataTypes,
        calendarTypes: refTables.calendarTypes,
        descItemTypes: refTables.descItemTypes,
    }
}

export default connect(mapStateToProps)(StructureSubNodeForm);
