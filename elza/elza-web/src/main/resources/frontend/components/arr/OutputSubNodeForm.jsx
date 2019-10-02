import React from 'react';
import ReactDOM from 'react-dom';
import {
    Icon,
    i18n,
    AbstractReactComponent,
    NoFocusButton
} from 'components/shared'
import {connect} from 'react-redux'
import {outputFormActions} from '../../actions/arr/subNodeForm';

import './NodeSubNodeForm.less';
import SubNodeForm from "./SubNodeForm";
import objectById from "../../shared/utils/objectById";
import DescItemFactory from "components/arr/nodeForm/DescItemFactory.jsx";

/**
 * Formulář detailu a editace jedné JP - jednoho NODE v konkrétní verzi.
 */
class OutputSubNodeForm extends AbstractReactComponent {

    static PropTypes = {
        versionId: React.PropTypes.number.isRequired,
        fundId: React.PropTypes.number.isRequired,
        selectedSubNodeId: React.PropTypes.number.isRequired,
        rulDataTypes: React.PropTypes.object.isRequired,
        calendarTypes: React.PropTypes.object.isRequired,
        descItemTypes: React.PropTypes.object.isRequired,
        structureTypes: React.PropTypes.object.isRequired,
        subNodeForm: React.PropTypes.object.isRequired,
        closed: React.PropTypes.bool.isRequired,
        readMode: React.PropTypes.bool.isRequired,
        focus: React.PropTypes.object.isRequired,
    };

    initFocus = () => {
        this.refs.subNodeForm.getWrappedInstance().initFocus();
    };

    render() {
        const {versionId, focus, closed, fundId, rulDataTypes, calendarTypes, structureTypes, descItemTypes, subNodeForm, readMode} = this.props;

        return (
            <div className="output-item-form-container">
                <SubNodeForm
                    ref="subNodeForm"
                    typePrefix="output"
                    versionId={versionId}
                    fundId={fundId}
                    routingKey={null}
                    nodeSetting={null}
                    rulDataTypes={rulDataTypes}
                    calendarTypes={calendarTypes}
                    structureTypes={structureTypes}
                    descItemTypes={descItemTypes}
                    subNodeForm={subNodeForm}
                    closed={closed}
                    conformityInfo={{missings: [], errors: []}}
                    descItemCopyFromPrevEnabled={false}
                    focus={focus}
                    singleDescItemTypeId={null}
                    singleDescItemTypeEdit={false}
                    onDescItemTypeCopyFromPrev={() => {}}
                    onDescItemTypeLock={() => {}}
                    onDescItemTypeCopy={() => {}}
                    formActions={outputFormActions}
                    showNodeAddons={false}
                    readMode={closed || readMode}
                    descItemFactory={DescItemFactory}
                />
            </div>
        )
    }
}

function mapStateToProps(state) {
    const {arrRegion, focus, refTables} = state;
    let fund = null;
    let structureTypes = null;
    if (arrRegion.activeIndex != null) {
        fund = arrRegion.funds[arrRegion.activeIndex];
        structureTypes = objectById(refTables.structureTypes.data, fund.versionId, "versionId");
    }

    return {
        fund,
        focus,
        structureTypes
    }
}

export default connect(mapStateToProps, null, null, { withRef: true })(OutputSubNodeForm);
