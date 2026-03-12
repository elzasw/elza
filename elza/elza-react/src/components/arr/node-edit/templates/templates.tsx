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

function isNodeTemplate(template: NodeTemplate | DeprecatedNodeTemplate): template is NodeTemplate {
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
                                itemSpecId: descItem.itemSpecId,
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

                        const createItems: NodeItem[] = [];
                        const deleteItems: NodeItem[] = [];

                        const itemTypePositions: Record<number, number> = {};
                        const processedItemObjectIds: number[] = [];

                        descItemsWithValue
                            .sort((a, b) => a.itemTypeId - b.itemTypeId || a.position - b.position) // sort by itemTypeId and position
                            .forEach((descItem) => {
                                const pendingDescItems = descItems.filter(
                                    ({ itemObjectId }) => !processedItemObjectIds.includes(itemObjectId)
                                ); // remove items already processed

                                // skip items that already have the same value
                                const itemWithSameValue = pendingDescItems.find((_descItem) =>
                                    isValueEqual(_descItem, descItem)
                                );
                                if (itemWithSameValue) {
                                    processedItemObjectIds.push(itemWithSameValue.itemObjectId);
                                    return;
                                }

                                const existingDescItem = pendingDescItems.find(
                                    (_descItem) => descItem.itemTypeId == _descItem.itemTypeId
                                );

                                if (existingDescItem && data.replaceValues) {
                                    deleteItems.push({
                                        ...descItem,
                                        id: existingDescItem.id,
                                        itemObjectId: existingDescItem.itemObjectId,
                                    });
                                    processedItemObjectIds.push(existingDescItem.itemObjectId);
                                }

                                const highestPositionDescItem = descItems
                                    .filter(({ itemTypeId }) => itemTypeId === descItem.itemTypeId)
                                    .sort((a, b) => a.position - b.position)
                                    .pop();

                                const lastPosition =
                                    itemTypePositions[descItem.itemTypeId] || // incremented position
                                    (highestPositionDescItem?.position > 0 && highestPositionDescItem?.position) || // previous item position, if it is not inherited (negative position)
                                    0;
                                const nextPosition = lastPosition + 1;
                                itemTypePositions[descItem.itemTypeId] = nextPosition;

                                createItems.push({
                                    ...descItem,
                                    position: nextPosition,
                                });
                            });

                        // add local empty desc items
                        descItemsWithoutValue
                            .filter(
                                ({ itemTypeId }) => !descItems.find((_descItem) => _descItem.itemTypeId === itemTypeId)
                            )
                            .forEach((item) => {
                                onAddDescItem(item.itemTypeId, item.itemSpecId);
                            });

                        const _createItems = createItems.map((descItem) => convertToOldDescItem(descItem));
                        const _deleteItems = deleteItems.map((descItem) => convertToOldDescItem(descItem));

                        if (createItems.length > 0 || deleteItems.length > 0) {
                            await WebApi.updateDescItems(
                                fondsVersionId,
                                nodeId,
                                nodeVersion,
                                _createItems,
                                [],
                                _deleteItems
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
