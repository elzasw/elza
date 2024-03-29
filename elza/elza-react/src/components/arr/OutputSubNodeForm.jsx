import PropTypes from 'prop-types';
import React from 'react';
import {AbstractReactComponent} from 'components/shared';
import {connect} from 'react-redux';
import {outputFormActions} from 'actions/arr/subNodeForm';

import './NodeSubNodeForm.scss';
import SubNodeForm from './SubNodeForm';
import objectById from '../../shared/utils/objectById';
import DescItemFactory from 'components/arr/nodeForm/DescItemFactory.jsx';

/**
 * Formulář detailu a editace jedné JP - jednoho NODE v konkrétní verzi.
 */
class OutputSubNodeForm extends AbstractReactComponent {
    static propTypes = {
        versionId: PropTypes.number.isRequired,
        fundId: PropTypes.number.isRequired,
        selectedSubNodeId: PropTypes.number.isRequired,
        rulDataTypes: PropTypes.object.isRequired,
        descItemTypes: PropTypes.object.isRequired,
        structureTypes: PropTypes.object.isRequired,
        subNodeForm: PropTypes.object.isRequired,
        closed: PropTypes.bool.isRequired,
        readMode: PropTypes.bool.isRequired,
        focus: PropTypes.object.isRequired,
    };

    initFocus = () => {
        this.refs.subNodeForm.initFocus();
    };

    render() {
        const {
            versionId,
            focus,
            closed,
            fundId,
            rulDataTypes,
            structureTypes,
            descItemTypes,
            subNodeForm,
            readMode,
        } = this.props;

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
        );
    }
}

function mapStateToProps(state) {
    const {arrRegion, focus, refTables} = state;
    let fund = null;
    if (arrRegion.activeIndex != null) {
        fund = arrRegion.funds[arrRegion.activeIndex];
    }
    let structureTypes = objectById(refTables.structureTypes.data, null, 'versionId');

    return {
        fund,
        focus,
        structureTypes,
    };
}

export default connect(mapStateToProps)(OutputSubNodeForm);
