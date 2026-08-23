/** Dialog zobrazení DAO k JP. */
import PropTypes from 'prop-types';

import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent, i18n} from 'components/shared';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {modalDialogHide} from 'actions/global/modalDialog.jsx';
import ArrDaos from './ArrDaos.jsx';

class NodeDaosForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.state = {
            daoId: props.daoId,
            daoFileId: null,
            hadDaos: false,
        };
    }

    componentDidMount() {
        this.closeWhenEmptied();
    }

    componentDidUpdate() {
        this.closeWhenEmptied();
    }

    /**
     * Odpojení poslední digitální entity vyprázdní celý obsah dialogu. Prázdné okno
     * uživateli nic neříká, proto ho v takovém případě zavřeme.
     */
    closeWhenEmptied = () => {
        const {fund, onClose, dispatch} = this.props;
        const daoList = fund ? fund.nodeDaoList : null;
        if (!daoList || !daoList.fetched || daoList.isFetching) {
            return;
        }
        if (daoList.rows && daoList.rows.length > 0) {
            if (!this.state.hadDaos) {
                this.setState({hadDaos: true});
            }
            return;
        }
        if (this.state.hadDaos) {
            onClose ? onClose() : dispatch(modalDialogHide());
        }
    };

    static propTypes = {
        fund: PropTypes.object.isRequired,
        nodeId: PropTypes.number.isRequired,
        daoId: PropTypes.number, // pokud má být vybrán konkrétní DAO na detail
        readMode: PropTypes.bool.isRequired,
        onClose: PropTypes.func, // doplňuje ModalDialog, zavírá právě tento dialog
    };

    handleSelectDao = (dao, fileId) => {
        this.setState({
            daoId: dao.id,
            daoFileId: fileId,
        });
    };

    render() {
        const {fund, nodeId, readMode} = this.props;
        const {daoId, daoFileId} = this.state;

        return (
            <Form>
                <Modal.Body>
                    <ArrDaos
                        onSelect={this.handleSelectDao}
                        fund={fund}
                        type="NODE"
                        nodeId={nodeId}
                        readMode={readMode}
                        selectedDaoId={daoId}
                        selectedDaoFileId={daoFileId}
                    />
                </Modal.Body>
                <Modal.Footer>
                    <Button
                        variant="link"
                        onClick={() => {
                            this.props.dispatch(modalDialogHide());
                        }}
                    >
                        {i18n('global.action.close')}
                    </Button>
                </Modal.Footer>
            </Form>
        );
    }
}

function mapStateToProps(state) {
    const {arrRegion} = state;
    let fund = null;
    if (arrRegion.activeIndex != null) {
        fund = arrRegion.funds[arrRegion.activeIndex];
    }
    return {
        fund,
    };
}

export default connect(mapStateToProps)(NodeDaosForm);
