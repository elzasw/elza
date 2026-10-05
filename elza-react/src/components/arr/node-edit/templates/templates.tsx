import { WebApi } from 'actions';
import { modalDialogHide, modalDialogShow } from 'actions/global/modalDialog';
import { userDetailsSaveSettings } from 'actions/user/userDetail';
import TemplateUseForm from 'components/arr/TemplateUseForm';
import { daoMessages } from 'components/arr/daoMessages';
import { NodeItem } from 'elza-api';
import { indexById } from 'shared/utils';
import { useAppThunkDispatch } from 'utils/hooks';
import { useAppSelector } from 'utils/hooks/useAppSelector';
import { ItemClass } from '../../../../constants';
import { getOneSettings, setSettings } from '../../ArrUtils';
import TemplateForm, { EXISTS_TEMPLATE as exists_template, NEW_TEMPLATE as new_template } from '../../TemplateForm';
import { useActiveFund } from 'utils/hooks';
import { convertToNewTemplate, convertToOldDescItem } from './conversionUtils';
import { mergeItemsIntoNode } from './mergeItems';
import { ActionTypes } from 'actions/constants/ActionTypes';
import { isDataEnum } from './types';
import { getIntl } from 'components/shared/lang/intlInstance';

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
    'id' | 'itemObjectId' | 'readOnly' | 'nodeId' | 'nodeVersion' | 'inhibited'
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
                getIntl().formatMessage(daoMessages.fundAddTemplateCreate),
                <TemplateForm
                    initialValues={initialValues}
                    //@ts-expect-error TODO add templates to props/convert to final form and tsx
                    templates={templates}
                    onSubmitForm={({ withValues, name, type }: TemplateFormData) => {
                        const formData: NodeTemplateItem[] = descItems
                            .filter(({ nodeId: _nodeId }) => _nodeId === nodeId) // remove inherited
                            .map((descItem) => {
                                // convert to TemplateItem; an undefined item has no data
                                const isEnum = descItem.data != undefined && isDataEnum(descItem.data);
                                const typeOnlyData = descItem.data ? { dataType: descItem.data.dataType } : undefined;
                                return {
                                    itemSpecId: !isEnum || withValues ? descItem.itemSpecId : undefined,
                                    itemTypeId: descItem.itemTypeId,
                                    position: descItem.position,
                                    undefined: withValues ? descItem.undefined : undefined,
                                    data: withValues ? descItem.data : typeOnlyData,
                                };
                            });
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
                getIntl().formatMessage(daoMessages.fundUseTemplateTitle),
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

                        // exclude inherited items
                        const ownDescItems = descItems.filter(({ nodeId: _nodeId }) => _nodeId === nodeId);

                        const { createItems, deleteItems, missingEmptyItems } = mergeItemsIntoNode(
                            ownDescItems,
                            template.formData,
                            { replace: Boolean(data.replaceValues) },
                        );

                        // add local empty desc items
                        missingEmptyItems.forEach((item) => {
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
