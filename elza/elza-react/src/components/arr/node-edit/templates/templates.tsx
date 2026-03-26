import { WebApi } from 'actions';
import { modalDialogHide, modalDialogShow } from 'actions/global/modalDialog';
import { userDetailsSaveSettings } from 'actions/user/userDetail';
import TemplateUseForm from 'components/arr/TemplateUseForm';
import i18n from 'components/i18n';
import { NodeItem } from 'elza-api';
import { indexById } from 'shared/utils';
import { useAppThunkDispatch } from 'utils/hooks';
import { useAppSelector } from 'utils/hooks/useAppSelector';
import { ItemClass } from '../../../../constants';
import { getOneSettings, setSettings } from '../../ArrUtils';
import TemplateForm, { EXISTS_TEMPLATE as exists_template, NEW_TEMPLATE as new_template } from '../../TemplateForm';
import { useActiveFund } from '../hooks';
import { convertToNewTemplate, convertToOldDescItem } from './conversionUtils';
import { hasValue, isValueEqual } from './utils';
import { ActionTypes } from 'actions/constants/ActionTypes';
import { isDataEnum } from './types';

enum TemplateAddType {
    NEW_TEMPLATE = new_template,
    EXISTS_TEMPLATE = exists_template,
}

interface TemplateFormData {
    type: TemplateAddType;
    withValues: boolean;
    name: string;
}

interface UseTemplatesProps {
    descItems: NodeItem[];
    nodeId: number;
    nodeVersion: number;
    fondsVersionId: number;
    onAddDescItem: (itemTypeId: number, itemSpecId?: number) => void;
}

export interface DeprecatedNodeTemplateItem {
    '@class': ItemClass;
    descItemSpecId?: number;
    value?: number | string | null;
    strValue?: string | null;
    description?: string | null;
    refTemplateId?: number | null;
    nodeId?: number | null;
    undefined?: boolean;
    position?: number;
}

export interface NodeTemplate {
    name: string;
    withValues: boolean;
    formData: NodeTemplateItem[];
}

export interface DeprecatedNodeTemplate {
    name: string;
    withValues: boolean;
    formData: Record<number, DeprecatedNodeTemplateItem[]>;
}

export type NodeTemplateItem = Omit<
    NodeItem,
    'id' | 'itemObjectId' | 'readOnly' | 'nodeId' | 'nodeVersion' | 'inhibited' | 'undefined'
>;

export function isNodeTemplate(template: NodeTemplate | DeprecatedNodeTemplate): template is NodeTemplate {
    return Array.isArray(template.formData);
}

export function useTemplates({ descItems, nodeId, nodeVersion, fondsVersionId, onAddDescItem }: UseTemplatesProps) {
    const userSettings = useAppSelector(({ userDetail }) => userDetail.settings);
    const activeFund = useActiveFund();
    const dispatch = useAppThunkDispatch();

    const fundTemplates = getOneSettings(userSettings, 'FUND_TEMPLATES', 'FUND', activeFund.id);

    const _fundTemplates: (NodeTemplate | DeprecatedNodeTemplate)[] = fundTemplates?.value
        ? JSON.parse(fundTemplates.value)
        : [];
    const templates = _fundTemplates.map((template) => template.name);

    function createTemplate() {
        const initialValues = {
            type: TemplateAddType.NEW_TEMPLATE,
            withValues: true,
        };

        dispatch(
            modalDialogShow(
                this,
                i18n('arr.fund.addTemplate.create'),
                <TemplateForm
                    initialValues={initialValues}
                    //@ts-expect-error TODO add templates to props/convert to final form and tsx
                    templates={templates}
                    onSubmitForm={({ withValues, name, type }: TemplateFormData) => {
                        const formData: NodeTemplateItem[] = descItems
                            .filter(({ nodeId: _nodeId }) => _nodeId === nodeId) // remove inherited
                            .map((descItem) => ({
                                // convert to TemplateItem
                                itemSpecId: !isDataEnum(descItem.data) || withValues ? descItem.itemSpecId : undefined,
                                itemTypeId: descItem.itemTypeId,
                                position: descItem.position,
                                data: withValues
                                    ? descItem.data
                                    : {
                                          dataType: descItem.data.dataType,
                                      },
                            }));
                        const template = { name, withValues, formData };

                        switch (type) {
                            case TemplateAddType.NEW_TEMPLATE: {
                                const value = fundTemplates.value
                                    ? [...JSON.parse(fundTemplates.value), template]
                                    : [template];
                                value.sort((a, b) => {
                                    return a.name.localeCompare(b.name);
                                });

                                fundTemplates.value = JSON.stringify(value);
                                const settings = setSettings(userSettings, fundTemplates.id, fundTemplates);
                                dispatch(userDetailsSaveSettings(settings));
                                break;
                            }
                            case TemplateAddType.EXISTS_TEMPLATE: {
                                const value = JSON.parse(fundTemplates.value);
                                const index = indexById(value, name, 'name');

                                if (index == null) {
                                    console.error('Nebyla nalezena šablona s názvem: ' + name);
                                } else {
                                    value[index] = template;
                                    fundTemplates.value = JSON.stringify(value);
                                    const settings = setSettings(userSettings, fundTemplates.id, fundTemplates);
                                    dispatch(userDetailsSaveSettings(settings));
                                }
                                break;
                            }
                            default:
                                break;
                        }
                        return dispatch(modalDialogHide());
                    }}
                />
            )
        );
    }

    function applyTemplate() {
        const initialValues = {
            replaceValues: false,
            name:
                templates.indexOf(activeFund.lastUseTemplateName as string) >= 0
                    ? activeFund.lastUseTemplateName
                    : null,
        };

        dispatch(
            modalDialogShow(
                this,
                i18n('arr.fund.useTemplate.title'),
                <TemplateUseForm
                    initialValues={initialValues}
                    // @ts-expect-error TODO add templates to props/convert to final form and tsx
                    templates={templates}
                    onSubmitForm={async (data: { name: string; replaceValues?: boolean }) => {
                        let template = _fundTemplates.find(({ name }) => data.name === name);

                        if (!template) {
                            throw `Nebyla nalezena šablona s názvem: ${data.name}`;
                        }

                        if (!isNodeTemplate(template)) {
                            template = convertToNewTemplate(template);
                        }

                        const descItemsWithoutValue = template.formData.filter((item) => !hasValue(item));
                        const descItemsWithValue = template.formData.filter((item) => hasValue(item));

                        // exclude inherited items
                        const ownDescItems = descItems.filter(({ nodeId: _nodeId }) => _nodeId === nodeId);

                        const createItems: NodeItem[] = [];
                        const deleteItems: NodeItem[] = [];

                        const itemTypePositions: Record<number, number> = {};
                        const skippedItemObjectIds: number[] = [];

                        descItemsWithValue
                            .sort((a, b) => a.itemTypeId - b.itemTypeId || a.position - b.position) // sort by itemTypeId and position
                            .forEach((descItem) => {
                                // remove items already processed, exclude items without values
                                const pendingDescItems = ownDescItems.filter(
                                    ({ itemObjectId }) => itemObjectId != undefined
                                        && !skippedItemObjectIds.includes(itemObjectId)
                                );

                                // skip items that already have the same value
                                const itemWithSameValue = pendingDescItems.find((_descItem) =>
                                    _descItem.itemTypeId === descItem.itemTypeId
                                    && isValueEqual(_descItem, descItem)
                                );
                                if (itemWithSameValue) {
                                    skippedItemObjectIds.push(itemWithSameValue.itemObjectId);
                                    return;
                                }

                                // get next position for created item
                                const highestPositionDescItem = ownDescItems
                                    .filter(({ itemTypeId }) => itemTypeId === descItem.itemTypeId)
                                    .sort((a, b) => a.position - b.position)
                                    .pop();

                                const lastPosition =
                                    itemTypePositions[descItem.itemTypeId] || // incremented position
                                    (!data.replaceValues && highestPositionDescItem?.position) || // previous item position
                                    0;
                                const nextPosition = lastPosition + 1;
                                itemTypePositions[descItem.itemTypeId] = nextPosition;

                                createItems.push({
                                    ...descItem,
                                    position: nextPosition,
                                });
                            });

                        // Get remaining unprocessed descItems with value and add them to be deleted
                        // if they are of item type, that has been changed
                        if (data.replaceValues) {
                            const remainingDescItems = ownDescItems.filter(
                                ({ itemObjectId }) => !skippedItemObjectIds.includes(itemObjectId)
                                    && itemObjectId != undefined
                            );
                            const processedItemTypes = descItemsWithValue.map(({ itemTypeId }) => itemTypeId);
                            remainingDescItems.forEach((descItem) => {
                                if (processedItemTypes.includes(descItem.itemTypeId)) {
                                    deleteItems.push(descItem);
                                }
                            })
                        }

                        // add local empty desc items
                        descItemsWithoutValue
                            .filter(
                                ({ itemTypeId, itemSpecId }) =>
                                    !ownDescItems.find((_descItem) =>(
                                        _descItem.itemTypeId === itemTypeId
                                        && _descItem.itemSpecId === itemSpecId
                                    ))
                            )
                            .forEach((item) => {
                                onAddDescItem(item.itemTypeId, item.itemSpecId);
                            });

                        if (createItems.length > 0 || deleteItems.length > 0) {
                            await WebApi.updateDescItems(
                                fondsVersionId,
                                nodeId,
                                nodeVersion,
                                createItems,
                                [],
                                deleteItems
                            );
                        }

                        // store last used template
                        dispatch({
                            type: ActionTypes.FUND_TEMPLATE_USE,
                            versionId: fondsVersionId,
                            template: {name: template.name},
                        });

                        return dispatch(modalDialogHide());
                    }}
                />
            )
        );
    }

    return { templates, createTemplate, applyTemplate };
}
