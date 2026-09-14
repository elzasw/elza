import {Col, Dropdown, Modal, Row, Table} from 'react-bootstrap';
import ListBox from '../shared/listbox/ListBox';
import {Button} from '../ui';
import Loading from '../shared/loading/Loading';
import IssueListForm from '../form/IssueListForm';
import React, {useEffect, useState} from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { getIntl } from 'components/shared/lang/intlInstance';
import { templateMessages } from './templateMessages';
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
    const intl = useIntl();
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
    const afterUpdateItem = (result: ArrRefTemplateVO) => {
        setList([
            ...list!.slice(0, activeIndex!),
            {
                ...result!,
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
                            <h3>{<FormattedMessage {...templateMessages.refTemplatesListTitle} />}</h3>
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
                        {activeIndex === null && <div>{<FormattedMessage {...templateMessages.refTemplatesNoSelected} />}</div>}
                        {activeIndex !== null &&
                            (list === null ? (
                                <Loading />
                            ) : (
                                <>
                                    <h2>
                                        {<FormattedMessage {...templateMessages.refTemplatesDetailBasicInfo} />}{' '}
                                        <Button
                                            variant={'action'}
                                            onClick={() => onUpdateBase(selectedItem!, afterUpdateItem)}
                                        >
                                            <Icon glyph={'fa-pencil'} />
                                        </Button>
                                    </h2>
                                    <Row>
                                        <Col>
                                            <dl>
                                                <dt>{<FormattedMessage {...templateMessages.refTemplatesDetailName} />}</dt>
                                                <dd>{selectedItem?.name}</dd>
                                            </dl>
                                        </Col>
                                        <Col>
                                            <dl>
                                                <dt>{<FormattedMessage {...templateMessages.refTemplatesDetailItemTypeId} />}</dt>
                                                <dd>
                                                    {selectedItem!.itemTypeId
                                                        ? descItemTypes.itemsMap[selectedItem!.itemTypeId].name
                                                        : '-'}
                                                </dd>
                                            </dl>
                                        </Col>
                                    </Row>
                                    <h2>
                                        {<FormattedMessage {...templateMessages.refTemplatesMappingTitle} />}{' '}
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
                                                <th>{<FormattedMessage {...templateMessages.refTemplatesMappingFromItemTypeId} />}</th>
                                                <th>{<FormattedMessage {...templateMessages.refTemplatesMappingToItemTypeId} />}</th>
                                                <th>{<FormattedMessage {...templateMessages.refTemplatesMappingFromParentLevel} />}</th>
                                                <th>{<FormattedMessage {...templateMessages.refTemplatesMappingMapAllSpec} />}</th>
                                                <th>{<FormattedMessage {...templateMessages.refTemplatesMappingRefTemplateMapSpecVOList} />}</th>
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
                                                                    ? intl.formatMessage(globalMessages.yes)
                                                                    : intl.formatMessage(globalMessages.no)}
                                                            </td>
                                                            <td>
                                                                {i.mapAllSpec
                                                                    ? intl.formatMessage(globalMessages.yes)
                                                                    : intl.formatMessage(globalMessages.no)}
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
                                                                            {<FormattedMessage {...globalMessages.save} />}
                                                                        </Dropdown.Item>
                                                                        <Dropdown.Item
                                                                            onClick={() => onDeleteMapping(i.id)}
                                                                        >
                                                                            {<FormattedMessage {...globalMessages.delete} />}
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
                    {<FormattedMessage {...globalMessages.close} />}
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
                    getIntl().formatMessage(templateMessages.refTemplatesDetailUpdate),
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
                    getIntl().formatMessage(templateMessages.refTemplatesMappingCreateTitle),
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
                    getIntl().formatMessage(templateMessages.refTemplatesMappingUpdateTitle),
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
