import { modalDialogHide, modalDialogShow } from "actions/global/modalDialog";
import { userDetailsSaveSettings } from "actions/user/userDetail";
import i18n from "components/i18n";
import { NodeItem } from "elza-api";
import { indexById } from "shared/utils";
import { getOneSettings, setSettings } from "../../ArrUtils";
import TemplateForm, {
  EXISTS_TEMPLATE as exists_template,
  NEW_TEMPLATE as new_template,
} from "../../TemplateForm";
import { useAppThunkDispatch } from "utils/hooks";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { useActiveFund } from "../hooks";

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
}

export function useTemplates({ descItems }: UseTemplatesProps) {
  const userSettings = useAppSelector(({ userDetail }) => userDetail.settings);
  const activeFund = useActiveFund();
  const dispatch = useAppThunkDispatch();

  const fundTemplates = getOneSettings(
    userSettings,
    "FUND_TEMPLATES",
    "FUND",
    activeFund.id,
  );
  const templates = fundTemplates?.value
    ? JSON.parse(fundTemplates.value).map((template) => template.name)
    : [];

  function createTemplate() {
    const initialValues = {
      type: TemplateAddType.NEW_TEMPLATE,
      withValues: true,
    };

    const templates = fundTemplates?.value
      ? JSON.parse(fundTemplates.value).map((template) => template.name)
      : [];

    dispatch(
      modalDialogShow(
        this,
        i18n("arr.fund.addTemplate.create"),
        <TemplateForm
          initialValues={initialValues}
          //@ts-expect-error TODO add templates to props/convert to final form and tsx
          templates={templates}
          onSubmitForm={({ withValues, name, type }: TemplateFormData) => {
            const formData: NodeItem[] = withValues
              ? descItems
              : descItems.map((descItem) => ({
                  ...descItem,
                  data: {
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
                const settings = setSettings(
                  userSettings,
                  fundTemplates.id,
                  fundTemplates,
                );
                dispatch(userDetailsSaveSettings(settings));
                break;
              }
              case TemplateAddType.EXISTS_TEMPLATE: {
                const value = JSON.parse(fundTemplates.value);
                const index = indexById(value, name, "name");

                if (index == null) {
                  console.error("Nebyla nalezena šablona s názvem: " + name);
                } else {
                  value[index] = template;
                  fundTemplates.value = JSON.stringify(value);
                  const settings = setSettings(
                    userSettings,
                    fundTemplates.id,
                    fundTemplates,
                  );
                  dispatch(userDetailsSaveSettings(settings));
                }
                break;
              }
              default:
                break;
            }
            return dispatch(modalDialogHide());
          }}
        />,
      ),
    );
  }

  function applyTemplate() {
    console.warn("Template application not implemented yet");

    // const initialValues = {
    //   replaceValues: false,
    //   name:
    //     templates.indexOf(activeFund.lastUseTemplateName) >= 0
    //       ? activeFund.lastUseTemplateName
    //       : null,
    // };

    // dispatch(
    //   modalDialogShow(
    //     this,
    //     i18n("arr.fund.useTemplate.title"),
    //     <TemplateUseForm
    //       initialValues={initialValues}
    //       //@ts-expect-error TODO add templates to props/convert to final form and tsx
    //       templates={templates}
    //       onSubmitForm={(data) => {
    //         const value = JSON.parse(fundTemplates.value);
    //         const index = indexById(value, data.name, "name");

    //         if (index == null) {
    //           console.error("Nebyla nalezena šablona s názvem: " + data.name);
    //         } else {
    //           const template = value[index];
    //           console.debug("Apply template", template);

    //           const formData = template.formData;
    //           const createItems = [];
    //           const updateItems = [];
    //           const deleteItems = [];
    //           const deleteItemsAdded = {};

    //           const actualFormData = this.createFormData(subNodeForm);

    //           Object.keys(formData).forEach((itemTypeId) => {
    //             this.processItemType(
    //               formData,
    //               itemTypeId,
    //               actualFormData,
    //               data,
    //               deleteItemsAdded,
    //               deleteItems,
    //               updateItems,
    //               createItems,
    //             );
    //           });

    //           Object.keys(deleteItemsAdded).forEach((itemObjectId) => {
    //             let updateItemsTemp = [];
    //             updateItems.forEach((updateItem) => {
    //               if (itemObjectId === updateItem.descItemObjectId) {
    //                 createItems.push({
    //                   ...updateItem,
    //                   descItemObjectId: null,
    //                 });
    //               } else {
    //                 updateItemsTemp.push(updateItem);
    //               }
    //             });
    //             updateItems = updateItemsTemp;
    //           });

    //           if (
    //             createItems.length > 0 ||
    //             updateItems.length > 0 ||
    //             deleteItems.length > 0
    //           ) {
    //             return WebApi.updateDescItems(
    //               fund.versionId,
    //               selectedSubNode.id,
    //               selectedSubNode.version,
    //               createItems,
    //               updateItems,
    //               deleteItems,
    //             ).then(() => {
    //               this.props.dispatch(
    //                 nodeFormActions.fundSubNodeFormTemplateUse(
    //                   fund.versionId,
    //                   routingKey,
    //                   template,
    //                   data.replaceValues,
    //                   true,
    //                 ),
    //               );
    //               return this.props.dispatch(modalDialogHide());
    //             });
    //           } else {
    //             this.props.dispatch(
    //               nodeFormActions.fundSubNodeFormTemplateUse(
    //                 fund.versionId,
    //                 routingKey,
    //                 template,
    //                 data.replaceValues,
    //                 false,
    //               ),
    //             );
    //             return this.props.dispatch(modalDialogHide());
    //           }
    //         }
    //       }}
    //     />,
    //   ),
    // );
  }

  return { templates, createTemplate, applyTemplate };
}
