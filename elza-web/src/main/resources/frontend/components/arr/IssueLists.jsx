import * as React from 'react';
import {Button, Row, Col, Modal} from 'react-bootstrap';
import {AbstractReactComponent, Icon} from 'components/shared';
import {connect} from "react-redux";
import storeFromArea from "../../shared/utils/storeFromArea";
import * as issuesActions from "../../actions/arr/issues";
import FormInput from "../shared/form/FormInput";
import i18n from "../i18n";
import ListBox from "../shared/listbox/ListBox";
import indexById from "../../shared/utils/indexById";
import Loading from "../shared/loading/Loading";
import IssueListForm from "../form/IssueListForm";
import {WebApi} from "../../actions";

import "./IssueLists.less";


class IssueLists extends AbstractReactComponent {

    state = {id: null, initialValues: undefined};

    static propTypes = {
        fundId: React.PropTypes.number.isRequired
    };

    componentWillReceiveProps(nextProps: Readonly<P>, nextContext: any): void {
        nextProps.dispatch(issuesActions.protocols.fetchIfNeeded(this.props.fundId));
    }

    componentWillUnmount(): void {
        this.props.dispatch(issuesActions.protocols.filter({}));
    }

    select = ([index]) => {
        const item = this.props.issueProtocols.rows[index];
        WebApi.getIssueList(item.id).then(this.onSave);
    };

    create = () => {
        this.setState({id: null, initialValues: IssueListForm.initialValues});
    };

    onCreate = data => {
        this.props.dispatch(issuesActions.protocols.fetchIfNeeded(this.props.fundId, true));
        this.setState({id:data.id, initialValues: data});
    };

    onSave = data => {
        this.setState({id:data.id, initialValues: data});
    };

    filter = ({target: {value}}) => {
        this.props.dispatch(issuesActions.protocols.filter({open: value == 'true'}));
    };

    render() {
        const {onClose, issueProtocols, fundId} = this.props;
        const {id, initialValues} = this.state;
        const activeIndex = id !== null ? indexById(issueProtocols.rows, this.state.id) : null;
        const isLoading = id && !initialValues;
        return (
            <div className={"issue-list"}>
                <Modal.Body>
                    <Row className="flex">
                        <Col xs={6} sm={3} className="flex flex-column">
                            <FormInput componentClass="select" name="state" onChange={this.filter}>
                                <option value={true}>{i18n("issueList.open.true")}</option>
                                <option value={false}>{i18n("issueList.open.false")}</option>
                            </FormInput>
                            <ListBox className="flex-1" items={issueProtocols.rows} activeIndex={activeIndex} onChangeSelection={this.select} />
                            <div>
                                <Button bsStyle={"action"} onClick={this.create}>
                                    <Icon glyph="fa-plus" />
                                </Button>
                            </div>
                        </Col>
                        <Col xs={6} sm={9}>
                            {isLoading ? <Loading/> : <IssueListForm id={id} fundId={fundId} onCreate={this.onCreate} onSave={this.onSave} initialValues={initialValues} />}
                        </Col>
                    </Row>
                </Modal.Body>
                <Modal.Footer>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

export default connect((state) => {
    return {
        issueTypes: state.refTables.issueTypes,
        issueList: storeFromArea(state, issuesActions.AREA_LIST),
        issueProtocols: storeFromArea(state, issuesActions.AREA_PROTOCOLS)
    }
})(IssueLists);

