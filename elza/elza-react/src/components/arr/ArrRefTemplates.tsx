import {Col, Dropdown, Modal, Row, Table} from 'react-bootstrap';
import ListBox from '../shared/listbox/ListBox';
import {Button} from '../ui';
import Loading from '../shared/loading/Loading';
import IssueListForm from '../form/IssueListForm';
import React, {useEffect, useState} from 'react';
import i18n from '../i18n';
import Icon from '../shared/icon/Icon';
import {WebApi} from '../../actions/WebApi';
import {ArrRefTemplateMapTypeVO, ArrRefTemplateVO} from '../../types';
import {modalDialogHide, modalDialogShow} from '../../actions/global/modalDialog';
import indexById from '../../shared/utils/indexById';
import ArrRefMappingTypeForm from './ArrRefMappingTypeForm';
import {connect, DispatchProp} from 'react-redux';
import {ThunkDispatch} from 'redux-thunk';
import ArrRefTemplateForm from './ArrRefTemplateForm';

type OwnProps = {
    fundId: number;
};

type Props = {dispatch: ThunkDispatch<any, any, any>} & ReturnType<typeof mapStateToProps> &
    ReturnType<typeof mapDispatchToProps> &
    OwnProps & {
        onClose: Function;
    };

const ArrRefTemplates = ({descItemTypes, onUpdateBase, onClose, fundId, onCreateMapping, onUpdateMapping}: Props) => {
    const [activeIndex, setActiveIndex] = useState<number | null>(null);
    const [list, setList] = useState<ArrRefTemplateVO[] | null>(null);

    useEffect(() => {
        WebApi.getRefTemplates(fundId).then(setList);
    }, []);

    const onCreate = () => {
        WebApi.createRefTemplate(fundId).then(i => {
            const newList = [...(list || []), i];
            setList(newList);
            setActiveIndex(newList.length - 1);
        });
    };

    const selectedItem: ArrRefTemplateVO | null = activeIndex !== null && list !== null ? list[activeIndex] : null;

    const onDelete = (id: number) => {
        WebApi.deleteRefTemplate(id).then(() => {
            let index: number | undefined = undefined;
            if (selectedItem?.id === id) {
                index = activeIndex!;
                setActiveIndex(null);
            } else if (list !== null) {
                let byID = indexById(list, id);
                if (byID !== null) {
                    index = byID;
                }
            }
            if (index !== undefined) {
                setList([...list!.slice(0, index), ...list!.slice(index + 1)]);
            }
        });
    };
    const afterCreateMapping = (result: ArrRefTemplateMapTypeVO) => {
        setList([
            ...list!.slice(0, activeIndex!),
            {
                ...selectedItem!,
                refTemplateMapTypeVOList: [...selectedItem!.refTemplateMapTypeVOList!, result],
            },
            ...list!.slice(activeIndex! + 1),
        ]);
    };
    const afterUpdateMapping = (result: ArrRefTemplateMapTypeVO) => {
        const byId = indexById(selectedItem?.refTemplateMapTypeVOList, result.id);
        setList([
            ...list!.slice(0, activeIndex!),
            {
                ...selectedItem!,
                refTemplateMapTypeVOList: [
                    ...selectedItem!.refTemplateMapTypeVOList!.slice(0, byId!),
                    result,
                    ...selectedItem!.refTemplateMapTypeVOList!.slice(byId! + 1),
                ],
            },
            ...list!.slice(activeIndex! + 1),
        ]);
    };
    const onDeleteMapping = (id: number) => {
        WebApi.deleteRefTemplateMapType(selectedItem!.id, id).then(() => {
            const byId = indexById(selectedItem?.refTemplateMapTypeVOList, id);
            setList([
                ...list!.slice(0, activeIndex!),
                {
                    ...selectedItem!,
                    refTemplateMapTypeVOList: [
                        ...selectedItem!.refTemplateMapTypeVOList!.slice(0, byId!),
                        ...selectedItem!.refTemplateMapTypeVOList!.slice(byId! + 1),
                    ],
                },
                ...list!.slice(activeIndex! + 1),
            ]);
        });
    };

    return (
        <>
            <Modal.Body>
                <Row className="flex">
                    <Col xs={6} sm={3} className="flex flex-column">
                        <div className={'d-flex'}>
                            <h3>{i18n('arr.refTemplates.list.title')}</h3>
                            <Button variant={'action'} onClick={onCreate}>
                                <Icon glyph="fa-plus" />
                            </Button>
                        </div>
                        {!list ? (
                            <Loading />
                        ) : (
                            <ListBox
                                className="flex-1"
                                items={list}
                                activeIndex={activeIndex}
                                onChangeSelection={setActiveIndex}
                                renderItemContent={(
                                    {item, active, index}: {item: ArrRefTemplateVO; active: boolean; index: number},
                                    onCheckItem,
                                ) => {
                                    return (
                                        <div className={'d-flex pl-2 pr-1 py-2 border-bottom'}>
                                            {item.name}{' '}
                                            <Button
                                                variant={'action'}
                                                onClick={e => {
                                                    e.preventDefault();
                                                    e.stopPropagation();
                                                    onDelete(item.id);
                                                }}
                                                className={'ml-auto'}
                                            >
                                                <Icon glyph={'fa-trash'} />
                                            </Button>
                                        </div>
                                    );
                                }}
                            />
                        )}
                    </Col>
                    <Col xs={6} sm={9}>
                        {activeIndex === null && <div>{i18n('arr.refTemplates.noSelected')}</div>}
                        {activeIndex !== null &&
                            (list === null ? (
                                <Loading />
                            ) : (
                                <>
                                    <h2>
                                        {i18n('arr.refTemplates.detail.basicInfo')}{' '}
                                        <Button variant={'action'} onClick={() => onUpdateBase(selectedItem!)}>
                                            <Icon glyph={'fa-pencil'} />
                                        </Button>
                                    </h2>
                                    <Row>
                                        <Col>
                                            <dl>
                                                <dt>{i18n('arr.refTemplates.detail.name')}</dt>
                                                <dd>{selectedItem?.name}</dd>
                                            </dl>
                                        </Col>
                                        <Col>
                                            <dl>
                                                <dt>{i18n('arr.refTemplates.detail.itemTypeId')}</dt>
                                                <dd>
                                                    {selectedItem!.itemTypeId
                                                        ? descItemTypes.itemsMap[selectedItem!.itemTypeId].name
                                                        : '-'}
                                                </dd>
                                            </dl>
                                        </Col>
                                    </Row>
                                    <h2>
                                        {i18n('arr.refTemplates.mapping.title')}{' '}
                                        <Button
                                            variant={'action'}
                                            onClick={() => onCreateMapping(selectedItem!.id, afterCreateMapping)}
                                        >
                                            <Icon glyph={'fa-plus'} />
                                        </Button>
                                    </h2>
                                    <Table>
                                        <thead>
                                            <tr>
                                                <th>{i18n('arr.refTemplates.mapping.fromItemTypeId')}</th>
                                                <th>{i18n('arr.refTemplates.mapping.toItemTypeId')}</th>
                                                <th>{i18n('arr.refTemplates.mapping.fromParentLevel')}</th>
                                                <th>{i18n('arr.refTemplates.mapping.mapAllSpec')}</th>
                                                <th>{i18n('arr.refTemplates.mapping.refTemplateMapSpecVOList')}</th>
                                                <th></th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {selectedItem?.refTemplateMapTypeVOList &&
                                                selectedItem?.refTemplateMapTypeVOList.map(i => {
                                                    return (
                                                        <tr key={'id-' + i.id}>
                                                            <td>
                                                                {i.fromItemTypeId &&
                                                                descItemTypes.itemsMap[i.fromItemTypeId]
                                                                    ? descItemTypes.itemsMap[i.fromItemTypeId].name
                                                                    : '-'}
                                                            </td>
                                                            <td>
                                                                {i.toItemTypeId &&
                                                                descItemTypes.itemsMap[i.toItemTypeId]
                                                                    ? descItemTypes.itemsMap[i.toItemTypeId].name
                                                                    : '-'}
                                                            </td>
                                                            <td>
                                                                {i.fromParentLevel
                                                                    ? i18n('global.title.yes')
                                                                    : i18n('global.title.no')}
                                                            </td>
                                                            <td>
                                                                {i.mapAllSpec
                                                                    ? i18n('global.title.yes')
                                                                    : i18n('global.title.no')}
                                                            </td>

                                                            <td>
                                                                {i.refTemplateMapSpecVOList
                                                                    ? i.refTemplateMapSpecVOList.length
                                                                    : 0}
                                                            </td>
                                                            <td>
                                                                <Dropdown>
                                                                    <Dropdown.Toggle
                                                                        variant={'action' as any}
                                                                        id={'actions-' + i.id}
                                                                    >
                                                                        <Icon glyph={'fa-ellipsis-v'} />
                                                                    </Dropdown.Toggle>

                                                                    <Dropdown.Menu>
                                                                        <Dropdown.Item
                                                                            onClick={() =>
                                                                                onUpdateMapping(
                                                                                    selectedItem.id,
                                                                                    i,
                                                                                    afterUpdateMapping,
                                                                                )
                                                                            }
                                                                        >
                                                                            {i18n('global.action.update')}
                                                                        </Dropdown.Item>
                                                                        <Dropdown.Item
                                                                            onClick={() => onDeleteMapping(i.id)}
                                                                        >
                                                                            {i18n('global.action.delete')}
                                                                        </Dropdown.Item>
                                                                    </Dropdown.Menu>
                                                                </Dropdown>
                                                            </td>
                                                        </tr>
                                                    );
                                                })}
                                        </tbody>
                                    </Table>
                                </>
                            ))}
                    </Col>
                </Row>
            </Modal.Body>
            <Modal.Footer>
                <Button variant="link" onClick={onClose}>
                    {i18n('global.action.close')}
                </Button>
            </Modal.Footer>
        </>
    );
};

function mapStateToProps(state: any) {
    const {
        refTables: {descItemTypes},
    } = state;
    return {
        descItemTypes,
    };
}

function mapDispatchToProps(dispatch: ThunkDispatch<any, any, any>, props: OwnProps) {
    return {
        onUpdateBase: (base: ArrRefTemplateVO, onSubmitSuccess?: (result) => void) =>
            dispatch(
                modalDialogShow(
                    this,
                    i18n('arr.refTemplates.detail.update'),
                    <ArrRefTemplateForm
                        initialValues={base}
                        onSubmit={data => {
                            return WebApi.updateRefTemplate(data.id, data);
                        }}
                        onSubmitSuccess={(result, dispatch, props) => {
                            dispatch(modalDialogHide());
                            onSubmitSuccess && onSubmitSuccess(result);
                        }}
                    />,
                ),
            ),
        onCreateMapping: (templateId: number, onSubmitSuccess?: (result) => void) =>
            dispatch(
                modalDialogShow(
                    this,
                    i18n('arr.refTemplates.mapping.create.title'),
                    <ArrRefMappingTypeForm
                        create
                        onSubmit={data => {
                            return WebApi.createRefTemplateMapType(templateId, data);
                        }}
                        onSubmitSuccess={(result, dispatch, props) => {
                            dispatch(modalDialogHide());
                            onSubmitSuccess && onSubmitSuccess(result);
                        }}
                    />,
                ),
            ),
        onUpdateMapping: (templateId: number, base: ArrRefTemplateMapTypeVO, onSubmitSuccess?: (result) => void) =>
            dispatch(
                modalDialogShow(
                    this,
                    i18n('arr.refTemplates.mapping.update.title'),
                    <ArrRefMappingTypeForm
                        initialValues={base}
                        onSubmit={data => {
                            return WebApi.updateRefTemplateMapType(templateId, data.id, data);
                        }}
                        onSubmitSuccess={(result, dispatch, props) => {
                            dispatch(modalDialogHide());
                            onSubmitSuccess && onSubmitSuccess(result);
                        }}
                    />,
                ),
            ),
    };
}

export default connect(mapStateToProps, mapDispatchToProps)(ArrRefTemplates);
