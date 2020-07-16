import React, {useState} from 'react';
import {
    ConfigProps,
    Form as ReduxForm,
    formValueSelector,
    InjectedFormProps,
    reduxForm,
    SubmitHandler,
} from 'redux-form';
import {Col, Dropdown, DropdownButton, Modal, Row} from 'react-bootstrap';
import {connect} from "react-redux";
import {Action} from "redux";
import {ThunkDispatch} from "redux-thunk";
import {Button} from "../../ui";
import i18n from "../../i18n";
import './ApExtSearchModal.scss';
import ExtSystemFilterSection from '../form/filter/ExtSystemFilterSection';
import TextFilterSection from "../form/filter/TextFilterSection";
import BaseFilterSection from "../form/filter/BaseFilterSection";
import CreExtFilterSection from "../form/filter/CreExtFilterSection";
import {WebApi} from "../../../actions/WebApi";
import {SearchFilterVO} from "../../../api/SearchFilterVO";
import {ArchiveEntityVO} from "../../../api/ArchiveEntityVO";
import {AeState} from "../../../api/AeState";
import {indexById} from "../../../shared/utils";
import InifiniteList from "../../../shared/list/InifiniteList";
import {HorizontalLoader} from "../../shared";
import ExtendsFilterSection from "../form/filter/ExtendsFilterSection";
import RelationsFilterSection from "../form/filter/RelationsFilterSection";
import {ArchiveEntityResultListVO} from "../../../api/ArchiveEntityResultListVO";

const FORM_NAME = "apExtSearch";

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

export enum TypeModal {
    SEARCH = 'SEARCH',
    CONNECT = 'CONNECT'
}

type Props = {
    refTables: {};
    scopes: any[];
    handleSubmit: SubmitHandler<FormData, any, any>;
    formData?: FormProps;
    submitting: boolean;
    extSystems: any[];
    onClose: () => void;
    onConnected: () => void;
    itemType: TypeModal;
    accessPointId?: number;
} & ReturnType<typeof mapDispatchToProps> & ReturnType<typeof mapStateToProps> & InjectedFormProps;

type Data = {
    isFetching: boolean;
    fetched: boolean;
    lastIndex: number;
    externalSystemCode: string;
    data: ArchiveEntityVO[];
    total: number;
    filter: object;
}

const createFilter = (values): SearchFilterVO => {
    const aeStates = values.states as AeState[];
    const aeTypeIds = values.types as number[];
    const extFilters = values.extFilters ? values.extFilters.map(f => {
        return {
            partTypeCode: f.partType ? f.partType.code : null,
            itemTypeId: f.itemType ? f.itemType.id : null,
            itemSpecId: f.itemSpec ? f.itemSpec.id : null,
            value: f.obj ? f.obj.id : f.value,
        }
    }) : null;
    const relFilters = values.relFilters ? values.relFilters.map(f => {
        return {
            relTypeId: f.itemType ? f.itemType.id : null,
            code: f.obj ? f.obj.id : null,
        }
    }) : null;
    return {
        search: values.search,
        area: values.area,
        aeStates: aeStates,
        aeTypeIds: aeTypeIds,
        onlyMainPart: values.onlyMainPart === 'true',
        user: values.user,
        code: values.id,
        creation: values.creation,
        extinction: values.extinction,
        relFilters: relFilters,
        extFilters: extFilters
    };
}

const ApExtSearchModal = ({handleSubmit, onClose, onConnected, submitting, extSystems, extSystem, refTables, scopes, reset, itemType, accessPointId}: Props) => {
    const [data, setData] = useState<Data>({
        isFetching: false,
        fetched: false,
        lastIndex: 0,
        externalSystemCode: "",
        data: [],
        total: 0,
        filter: {}
    });
    const [taked, setTaked] = useState<string[]>([]);
    const [calling, setCalling] = useState<boolean>(false);

    const {apTypes} = refTables;

    const handleItemTake = (ae: ArchiveEntityVO, scopeId: number) => {
        setCalling(true);
        return WebApi.takeArchiveEntity(ae.id, scopeId, data.externalSystemCode).then(() => {
            setTaked([...taked, data.externalSystemCode + ae.id]);
        }).finally(() => {
            setCalling(false);
        });
    };

    const handleItemConnect = (ae: ArchiveEntityVO) => {
        if (accessPointId != null) {
            setCalling(true);
            return WebApi.connectArchiveEntity(ae.id, accessPointId, data.externalSystemCode).then(() => {
                onConnected && onConnected();
                onClose && onClose();
            }).finally(() => {
                setCalling(false);
            });
        } else {
            console.error('Modal dialogu nebyl předán vstupní parametr accessPointId');
        }
    };

    const fetchData = (from: number, externalSystemCode: string, filter: SearchFilterVO) => {
        return WebApi.findArchiveEntitiesInExternalSystem(from, 50, externalSystemCode, filter);
    }

    const fetchWithState = (tmpData, from, externalSystemCode, filter, oldData: ArchiveEntityVO[] = []) => {
        setData(tmpData);
        return fetchData(from, externalSystemCode, filter).then(result => {
            tmpData = {
                ...tmpData,
                fetched: true,
                data: [...oldData, ...result.data],
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
        const from = 0;
        const externalSystemCode = values.extSystem;
        const filter = createFilter(values);
        let tmpData = {
            ...data,
            externalSystemCode,
            lastIndex: from,
            isFetching: true,
            filter,
            data: [] as ArchiveEntityVO[],
        };
        return fetchWithState(tmpData, from, externalSystemCode, filter);
    };

    const fetchMore = () => {
        if (data.isFetching) {
            return false;
        }
        const from = data.lastIndex + 50;
        let tmpData = {
            ...data,
            lastIndex: from,
            isFetching: true,
        };
        return fetchWithState(tmpData, from, data.externalSystemCode, data.filter, data.data);
    }

    const renderAction = (item: ArchiveEntityVO, index: number) => {
        switch (itemType) {
            case TypeModal.CONNECT:
                return <Button disabled={calling} onClick={() => handleItemConnect(item)} type="button" variant="outline-secondary">{i18n('global.action.choose')}</Button>
            case TypeModal.SEARCH:
                return <>
                    {taked.indexOf(data.externalSystemCode + item.id) === -1 &&
                    <DropdownButton disabled={calling} variant="default" id={"b" + index} title={i18n('ap.ext-search.label.take-to-scope')}>
                        {scopes.map((scope, index) => <Dropdown.Item key={index}
                                                                     onClick={() => handleItemTake(item, scope.id)}>{scope.name}</Dropdown.Item>)}
                    </DropdownButton>}
                </>
        }
    }

    const renderResultItem = (item: ArchiveEntityVO, index: number) => {
        return <Row key={index} className="result-item">
            <Col>
                <span className="name">{item.name}</span>
                <span className="ident">{item.id}</span>
            </Col>
            <Col xs="auto">
                {renderAction(item, index)}
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

    const relEntityApi = (itemTypeId: number, itemSpecId: number, filter: any): Promise<ArchiveEntityResultListVO> => {
        return WebApi.findArchiveEntitiesInExternalSystem(0, 50, extSystem, filter);
    };

    return <ReduxForm className="ap-ext-search-modal" onSubmit={handleSubmit(submit)}>
        <Modal.Body className="no-padding">
            <Row noGutters>
                <Col className="search-panel" xs={3}>
                    <div className="search-fields">
                        <ExtSystemFilterSection submitting={submitting} extSystems={extSystems}/>
                        <TextFilterSection submitting={submitting}/>
                        <BaseFilterSection submitting={submitting} types={apTypes.items}/>
                        <CreExtFilterSection submitting={submitting}/>
                        <RelationsFilterSection externalSystemCode={extSystem} formName={FORM_NAME} submitting={submitting || !extSystem}/>
                        <ExtendsFilterSection relEntityApi={relEntityApi} formName={FORM_NAME} submitting={submitting || !extSystem}/>
                    </div>
                    <div className="search-controller">
                        <Button disabled={submitting} type="submit" variant="outline-secondary">{i18n('global.action.search')}</Button>
                        <Button disabled={submitting} type="button" onClick={reset} variant="link">{i18n('global.action.filter.clean')}</Button>
                    </div>
                </Col>
                <Col id="ListScrollableLayout" className="results" xs={9}>
                    {data.isFetching && <HorizontalLoader hover/>}
                    {data.fetched && !data.isFetching && data.data.length === 0 && <div className="text-center mt-5"><h2>{i18n('ap.ext-search.label.no-entities')}</h2></div>}
                    {data.fetched && data.data.length > 0 && renderResults(data)}
                    {!data.fetched && <div className="text-center mt-5"><h2>{i18n('ap.ext-search.label.params')}</h2></div>}
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

const mapDispatchToProps = (dispatch: ThunkDispatch<{}, {}, Action<string>>) => ({
    dispatch
});

const mapStateToProps = (state: any) => {
    const selector = formValueSelector(FORM_NAME);
    const scopesData = state.refTables.scopesData;
    const id = scopesData && indexById(scopesData.scopes, -1, 'versionId'); // všechny scope
    let scopes = [];
    if (id !== null) {
        scopes = scopesData.scopes[id].scopes;
    }
    return {
        extSystem: selector(state, 'extSystem'),
        refTables: state.refTables,
        scopes: scopes,
    };
};

export default connect(mapStateToProps, mapDispatchToProps)(reduxForm<any, any>(formConfig)(ApExtSearchModal));
