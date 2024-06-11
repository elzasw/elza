import PropTypes from 'prop-types';
import * as React from 'react';
import {Col, Modal, Row} from 'react-bootstrap';
import {connect} from 'react-redux';
import {WebApi} from '../../actions';
import * as issuesActions from '../../actions/arr/issues';
import {AbstractReactComponent, Icon} from '../../components/shared';
import {Button} from '../../components/ui';
import indexById from '../../shared/utils/indexById';
import storeFromArea from '../../shared/utils/storeFromArea';
import IssueListForm, {IssueListFormInitial} from '../form/IssueListForm';
import i18n from '../i18n';
import FormInput from 'components/shared/form/FormInput';
import ListBox from '../shared/listbox/ListBox';
import Loading from '../shared/loading/Loading';

import './IssueLists.scss';

class IssueLists extends AbstractReactComponent {
    state = {id: null, initialValues: undefined};

    static propTypes = {
        fundId: PropTypes.number.isRequired,
    };

    componentDidMount() {
        this.props.dispatch(issuesActions.protocolsConfig.fetchIfNeeded(this.props.fundId, true));
    }

    UNSAFE_componentWillReceiveProps(nextProps, nextContext) {
        nextProps.dispatch(issuesActions.protocolsConfig.fetchIfNeeded(this.props.fundId));
    }

    componentWillUnmount() {
        this.props.dispatch(issuesActions.protocolsConfig.filter({}));
    }

    select = ([index]) => {
        const item = this.props.issueProtocols.rows[index];
        WebApi.getIssueList(item.id).then(this.onSave);
    };

    create = () => {
        WebApi.addIssueList({
            ...IssueListForm.initialValues,
            name: i18n('issueList.new.unnamed'),
            fundId: this.props.fundId,
        }).then(this.onCreate);
    };

    onCreate = data => {
        this.props.dispatch(issuesActions.protocolsConfig.fetchIfNeeded(this.props.fundId, true));
        // @ts-ignore
        this.setState({id: data.id, initialValues: data});
    };

    delete = () => {
        WebApi.deleteIssueList(this.state.id);
        this.props.dispatch(issuesActions.protocolsConfig.fetchIfNeeded(this.props.fundId, true));
        this.setState({id: null, initialValues: undefined});
    }

    onSave = data => {
        this.props.dispatch(issuesActions.protocolsConfig.invalidate(this.props.fundId));
        // @ts-ignore
        this.setState({id: data.id, initialValues: data});
    };

    filter = ({target: {value}}) => {
        this.props.dispatch(issuesActions.protocolsConfig.filter({open: value === 'true'}));
        // @ts-ignore
        this.setState({id: null, initialValues: IssueListFormInitial});
    };

    render() {
        const {onClose, issueProtocols, fundId} = this.props;
        const {id, initialValues} = this.state;
        const activeIndex = id !== null ? indexById(issueProtocols.rows, this.state.id) : null;
        const isLoading = id && !initialValues;

        const ModalBody = Modal.Body;
        const ModalFooter = Modal.Footer;

        return (
            <div className={'issue-list'}>
                <ModalBody>
                    <Row className="flex">
                        <Col xs={6} sm={3} className="flex flex-column">
                            <FormInput type="select" name="state" onChange={this.filter}>
                                <option value={'true'}>{i18n('issueList.open.true')}</option>
                                <option value={'false'}>{i18n('issueList.open.false')}</option>
                            </FormInput>
                            <ListBox
                                className="flex-1"
                                items={issueProtocols.rows}
                                activeIndex={activeIndex}
                                onChangeSelection={this.select}
                            />
                            <div>
                                <Button variant={'action'} onClick={this.create}>
                                    <Icon glyph="fa-plus" />
                                </Button>
                                <Button variant={'action'} disabled={id === null} onClick={this.delete}>
                                    <Icon glyph="fa-trash"/>
                                </Button>
                            </div>
                        </Col>
                        <Col xs={6} sm={9}>
                            {isLoading ? (
                                <Loading />
                            ) : (
                                <IssueListForm
                                    id={id}
                                    fundId={fundId}
                                    onCreate={this.onCreate}
                                    onSave={this.onSave}
                                    initialValues={initialValues}
                                    enableReinitialize={true}
                                />
                            )}
                        </Col>
                    </Row>
                </ModalBody>
                <ModalFooter>
                    <Button variant="link" onClick={onClose}>
                        {i18n('global.action.close')}
                    </Button>
                </ModalFooter>
            </div>
        );
    }
}

export default connect(state => {
    return {
        issueTypes: state.refTables.issueTypes,
        issueList: storeFromArea(state, issuesActions.AREA_LIST),
        issueProtocols: storeFromArea(state, issuesActions.AREA_PROTOCOLS_CONFIG),
    };
})(IssueLists);
