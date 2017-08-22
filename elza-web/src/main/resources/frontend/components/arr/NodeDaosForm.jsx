/** Dialog zobrazení DAO k JP. */
import React from "react";
import {connect} from "react-redux";
import {Icon, Loading, AbstractReactComponent, i18n, ArrDao} from 'components/shared';
import {indexById} from "stores/app/utils.jsx";
import {Modal, Button, Form} from "react-bootstrap";
import {dateToString} from "components/Utils.jsx";
import {userDetailsSaveSettings} from "actions/user/userDetail.jsx";
import {fundChangeReadMode} from "actions/arr/fund.jsx";
import {setSettings, getOneSettings} from "components/arr/ArrUtils.jsx";
import {LazyListBox, ListBox} from 'components/shared';
import {modalDialogHide} from 'actions/global/modalDialog.jsx'
import {WebApi} from 'actions/index.jsx';
import ArrDaos from "./ArrDaos.jsx";

class NodeDaosForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    static PropTypes = {
        fund: React.PropTypes.object.isRequired,
        nodeId: React.PropTypes.number.isRequired,
        daoId: React.PropTypes.number,  // pokud má být vybrán konkrétní DAO na detail
        readMode: React.PropTypes.bool.isRequired
    }

    render() {
        const {fund, nodeId, daoId, readMode} = this.props;

        return (
            <Form>
                <Modal.Body>
                    <ArrDaos
                        fund={fund}
                        type="NODE"
                        nodeId={nodeId}
                        readMode={readMode}
                        selectedDaoId={daoId}
                        />
                </Modal.Body>
                <Modal.Footer>
                    <Button bsStyle="link" onClick={() => {
                        this.props.dispatch(modalDialogHide())
                    }}>{i18n('global.action.close')}</Button>
                </Modal.Footer>
            </Form>
        )
    }
}

function mapStateToProps(state) {
    const {arrRegion} = state
    let fund = null;
    if (arrRegion.activeIndex != null) {
        fund = arrRegion.funds[arrRegion.activeIndex];
    }
    return {
        fund,
    }
}

export default connect(mapStateToProps)(NodeDaosForm);
