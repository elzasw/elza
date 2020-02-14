import * as React from 'react';
import {Button, Row, Col, Modal, ControlLabel} from 'react-bootstrap';
import {AbstractReactComponent, Icon} from 'components/shared';
import {connect} from "react-redux";
import storeFromArea from "../../shared/utils/storeFromArea";
import * as scopeActions from "../../actions/scopes/scopes";
import i18n from "../i18n";
import ListBox from "../shared/listbox/ListBox";
import indexById from "../../shared/utils/indexById";
import Loading from "../shared/loading/Loading";
import {WebApi} from "../../actions";
import ScopeListForm from "../form/ScopeListForm";

import "./ScopeLists.scss";

class ScopeLists extends AbstractReactComponent {

    state = {id: null, initialValues: undefined};

    componentDidMount() {
        this.fetchData(this.props);
    }

    componentWillReceiveProps(nextProps) {
        this.fetchData(nextProps);
    }

    fetchData = (props) => {
        props.dispatch(scopeActions.scopesListFetchIfNeeded());
    };

    select = ([index]) => {
        const item = this.props.scopeList.rows[index];
        WebApi.getScopeWithConnected(item.id).then((data) => {
            this.setState({id: data.id, initialValues: {...data, language: data.language || ""}});
        });
    };

    create = () => {
        WebApi.createScope().then(this.onCreate);
        this.props.dispatch(scopeActions.scopeListInvalidate());
    };

    delete = (item) => {
        WebApi.deleteScope(item.id).then(() => {
            this.setState({id: null, initialValues: {}});
            this.props.dispatch(scopeActions.scopeListInvalidate());
        });
    };

    onCreate = data => {
        this.setState({id: data.id, initialValues: {...data, language: data.language || ""}});
        this.props.dispatch(scopeActions.scopeListInvalidate());
    };

    onSave = data => {
        this.setState({id: data.id, initialValues: {...data, language: data.language || ""}});
        this.props.dispatch(scopeActions.scopeListInvalidate());
    };

    renderScope = (value) => {
        return <div>{value.item.name} <Button bsStyle="action" bsSize="xs" className="pull-right" onClick={this.delete.bind(this, value.item)}><Icon glyph="fa-trash" /></Button></div>;
    };

    render() {

        const {onClose, scopeList} = this.props;
        const {id, initialValues} = this.state;

        if (!scopeList.fetched) {
            return <Loading/>;
        }

        const activeIndex = id !== null ? indexById(scopeList.rows, this.state.id) : null;
        const isLoading = id && !initialValues;

        return (
            <div className={"scope-list"}>
                <Modal.Body>
                    <Row className="flex">
                        <Col xs={6} sm={3} className="flex flex-column">
                            <div className="flex" style={{"alignItems": "baseline"}}>
                                <ControlLabel>{i18n('accesspoint.scope.list')}</ControlLabel>
                                <Button bsStyle={"action"} onClick={this.create}>
                                    <Icon glyph="fa-plus" />
                                </Button>
                            </div>
                            {scopeList ? <ListBox className="flex-1" items={scopeList.rows} activeIndex={activeIndex} onChangeSelection={this.select} renderItemContent={this.renderScope} />
                            : <Loading/>}
                        </Col>
                        <Col xs={6} sm={9}>
                            {isLoading ? <Loading/> : <ScopeListForm id={id} onCreate={this.onCreate} onSave={this.onSave} initialValues={initialValues} />}
                        </Col>
                    </Row>
                </Modal.Body>
                <Modal.Footer>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.close')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

export default connect((state) => {
    return {
        scopeList: storeFromArea(state, scopeActions.AREA_SCOPE_LIST),
    }
})(ScopeLists);
