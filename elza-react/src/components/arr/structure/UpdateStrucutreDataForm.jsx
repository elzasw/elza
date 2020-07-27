import React from 'react';
import {connect} from 'react-redux';
import {Modal} from 'react-bootstrap';
import {Button} from '../../ui';
import {AbstractReactComponent, i18n} from 'components';
import StructureSubNodeForm from './StructureSubNodeForm';
import {structureNodeFormFetchIfNeeded} from '../../../actions/arr/structureNodeForm';
import Loading from '../../shared/loading/Loading';
import PropTypes from 'prop-types';

class UpdateStructureDataForm extends AbstractReactComponent {
    static propTypes = {
        multiple: PropTypes.bool,
        fundVersionId: PropTypes.number.isRequired,
        fundId: PropTypes.number.isRequired,
        id: PropTypes.number.isRequired,
        readMode: PropTypes.bool.isRequired,
        descItemFactory: PropTypes.func.isRequired,
    };

    UNSAFE_componentWillMount() {
        const {fundVersionId, id} = this.props;
        this.props.dispatch(structureNodeFormFetchIfNeeded(fundVersionId, id));
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        const {fundVersionId, id} = nextProps;
        this.props.dispatch(structureNodeFormFetchIfNeeded(fundVersionId, id));
    }

    render() {
        const {onClose, fundVersionId, fundId, structureNodeForm, readMode, descItemFactory} = this.props;

        return (
            <div>
                <Modal.Body>
                    {structureNodeForm && structureNodeForm.fetched ? (
                        <StructureSubNodeForm
                            versionId={fundVersionId}
                            readMode={readMode}
                            fundId={fundId}
                            selectedSubNodeId={structureNodeForm.id}
                            descItemFactory={descItemFactory}
                        />
                    ) : (
                        <Loading />
                    )}
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="link" onClick={onClose}>
                        {i18n('global.action.close')}
                    </Button>
                </Modal.Footer>
            </div>
        );
    }
}

export default connect((state, props) => {
    const {arrRegion} = state;
    let fund = null;
    if (arrRegion.activeIndex != null) {
        fund = arrRegion.funds[arrRegion.activeIndex];
    }
    return {
        structureNodeForm: fund ? fund.structureNodeForm : null,
    };
})(UpdateStructureDataForm);
