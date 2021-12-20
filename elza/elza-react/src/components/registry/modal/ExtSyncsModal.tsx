import React, {useState} from 'react';
import {
    ConfigProps,
    Field,
    FieldArray,
    Form as ReduxForm,
    InjectedFormProps,
    reduxForm,
    SubmitHandler,
} from 'redux-form';
import {Col, Modal, Row} from 'react-bootstrap';
import {connect} from "react-redux";
import {Action} from "redux";
import {ThunkDispatch} from "redux-thunk";
import {Button} from "../../ui";
import i18n from "../../i18n";
import './ExtSyncsModal.scss';
import {ArchiveEntityVO} from "../../../api/ArchiveEntityVO";
import {getMapFromList, indexById} from "../../../shared/utils";
import InifiniteList from "../../../shared/list/InifiniteList";
import {FormInputField, HorizontalLoader, Icon} from "../../shared";
import {ExtAsyncQueueState} from "../../../api/ExtAsyncQueueState";
import {ExtStatesField} from "../field/ExtStatesField";
import {ScopesField} from "../../admin/ScopesField";
import {FundScope} from "../../../types";
import {WebApi} from "../../../actions/WebApi";
import {SyncsFilterVO} from "../../../api/SyncsFilterVO";
import {ExtSyncsQueueItemVO} from "../../../api/ExtSyncsQueueItemVO";
import * as ExtStateInfo from "../form/filter/ExtStateInfo";
import {dateToDateTimeString, localUTCToDateTime, utcToDateTime} from "../../../shared/utils/commons";

const FORM_NAME = "extSyncs";

type FormProps = {}

const validate = (values) => {
    const errors: any = {};
    if (!values.extSystem) {
        errors.extSystem = i18n('global.validation.required');
    }
    return errors;
};

const formConfig: ConfigProps<FormProps> = {
    form: FORM_NAME,
    validate
};

type Props = {
    scopes: any[];
    scopesMap: object;
    handleSubmit: SubmitHandler<FormData, any, any>;
    formData?: FormProps;
    submitting: boolean;
    extSystems: any[];
    onClose: () => void;
    onNavigateAp: (accessPointId: number) => void;
    accessPointId?: number;
} & ReturnType<typeof mapDispatchToProps> & ReturnType<typeof mapStateToProps> & InjectedFormProps;

type Data = {
    isFetching: boolean;
    fetched: boolean;
    lastCount: number;
    externalSystemCode: string;
    data: ArchiveEntityVO[];
    total: number;
    filter: object;
}

const createFilter = (values): SyncsFilterVO => {
    const states = values.states as ExtAsyncQueueState[];
    const scopes = values.scopes as string[];
    return {
        states: states,
        scopes: scopes
    };
}

const ExtSyncsModal = ({handleSubmit, onClose, submitting, extSystems, scopes, scopesMap, reset, onNavigateAp}: Props) => {
    const [data, setData] = useState<Data>({
        isFetching: false,
        fetched: false,
        lastCount: 50,
        externalSystemCode: "",
        data: [],
        total: 0,
        filter: {}
    });

    const fetchData = (count: number, externalSystemCode: string, filter: SyncsFilterVO) => {
        return WebApi.findExternalSyncs(0, count, externalSystemCode, filter);
    }

    const fetchWithState = (tmpData, count, externalSystemCode, filter) => {
        setData(tmpData);
        return fetchData(count, externalSystemCode, filter).then(result => {
            tmpData = {
                ...tmpData,
                fetched: true,
                data: result.data,
                total: result.total
            }
            setData(tmpData);
        }).finally(() => {
            tmpData = {
                ...tmpData,
                isFetching: false
            }
            setData(tmpData);
        });
    }

    const submit = (values) => {
        const count = 50;
        const externalSystemCode = values.extSystem;
        const filter = createFilter(values);
        let tmpData = {
            ...data,
            externalSystemCode,
            lastCount: count,
            isFetching: true,
            filter,
            data: [] as ArchiveEntityVO[],
        };
        return fetchWithState(tmpData, count, externalSystemCode, filter);
    };

    const fetchMore = () => {
        if (data.isFetching) {
            return false;
        }
        const count = data.lastCount + 50;
        let tmpData = {
            ...data,
            lastCount: count,
            isFetching: true,
        };
        return fetchWithState(tmpData, count, data.externalSystemCode, data.filter);
    }

    const onDelete = (id: number) => {
        return WebApi.deleteExtSyncsQueueItem(id).then(result => {
            const count = data.lastCount;
            let tmpData = {
                ...data,
                lastCount: count,
                isFetching: true,
            };
            return fetchWithState(tmpData, count, data.externalSystemCode, data.filter);
        });
    }

    const renderResultItem = (item: ExtSyncsQueueItemVO, index: number) => {
        const date = utcToDateTime(item.date);
        const exception = item.stateMessage && item.state !== ExtAsyncQueueState.EXPORT_OK && item.state !== ExtAsyncQueueState.IMPORT_OK;
        return <Row key={index} className="result-item">
            <Col>
                <Row>
                    <Col xs={12}><Button className="ap" variant="link" onClick={() => onNavigateAp(item.accessPointId)}>{item.accessPointName}</Button></Col>
                </Row>
                <Row className={exception? "font-red" : "font-black"}>
                    <Col xs={6}><span className="label">{i18n('ap.ext-syncs.date')}</span>{date ? dateToDateTimeString(date) : '-'}</Col>
                    <Col xs={3}><span className="label">{i18n('ap.ext-syncs.scope')}</span>{scopesMap[item.scopeId].name}</Col>
                    <Col xs={3} title={item.stateMessage}><span className="label">{i18n('ap.ext-syncs.state')}</span>{ExtStateInfo.getName(item.state)}</Col>
                </Row>
            </Col>
            <Col xs lg="2">
                <Button size="small" variant="outline-danger" onClick={() => onDelete(item.id)}>
                    <Icon glyph="fa-trash"/>
                </Button>
            </Col>
        </Row>
    };

    const renderResults = (data) => {
        return <InifiniteList scrollableTarget="ListScrollableLayout" fetchMore={fetchMore} list={data}>
            <div className="result-items">
                {data.data.map((item, index) => renderResultItem(item, index))}
            </div>
        </InifiniteList>
    }

    return <ReduxForm className="ext-syncs-modal" onSubmit={handleSubmit(submit)}>
        <Modal.Body className="no-padding">
            <Row noGutters>
                <Col className="search-panel" xs={3}>
                    <div className="search-fields">
                        <Field name="extSystem"
                               label={i18n('ap.ext-syncs.ext-system')}
                               type="autocomplete"
                               component={FormInputField}
                               getItemId={item => item && item.code}
                               useIdAsValue
                               items={extSystems}
                               disabled={submitting}
                        />
                        <FieldArray
                            name="states"
                            component={ExtStatesField}
                            label={i18n('ap.ext-syncs.state')}
                            disabled={submitting}
                        />
                        <FieldArray
                            name="scopes"
                            component={ScopesField}
                            label={i18n('ap.ext-syncs.scope')}
                            disabled={submitting}
                            scopeList={scopes as FundScope[]}
                        />
                    </div>
                    <div className="search-controller">
                        <Button disabled={submitting} type="submit" variant="outline-secondary">{i18n('global.action.search')}</Button>
                        <Button disabled={submitting} type="button" onClick={reset} variant="link">{i18n('global.action.filter.clean')}</Button>
                    </div>
                </Col>
                <Col id="ListScrollableLayout" className="results" xs={9}>
                    {data.isFetching && <HorizontalLoader hover/>}
                    {data.fetched && !data.isFetching && data.data.length === 0 && <div className="text-center mt-5"><h2>{i18n('ap.ext-syncs.label.no-entities')}</h2></div>}
                    {data.fetched && data.data.length > 0 && renderResults(data)}
                    {!data.fetched && <div className="text-center mt-5"><h2>{i18n('ap.ext-syncs.label.params')}</h2></div>}
                </Col>
            </Row>
        </Modal.Body>
        <Modal.Footer>
            <Button variant="link" onClick={onClose} disabled={submitting}>
                {i18n('global.action.close')}
            </Button>
        </Modal.Footer>
    </ReduxForm>;
};

const mapDispatchToProps = (dispatch: ThunkDispatch<{}, {}, Action<string>>) => ({});

const mapStateToProps = (state: any) => {
    const scopesData = state.refTables.scopesData;
    const id = scopesData && indexById(scopesData.scopes, -1, 'versionId'); // všechny scope
    let scopes = [];
    if (id !== null) {
        scopes = scopesData.scopes[id].scopes;
    }
    return {
        scopes: scopes,
        scopesMap: getMapFromList(scopes)
    };
};

export default connect(mapStateToProps, mapDispatchToProps)(reduxForm<any, any>(formConfig)(ExtSyncsModal));
