import { Spinner } from "@fluentui/react-components";
import { FormItemType } from "elza-api";
import { ReactNode, useMemo } from "react";
import { DescItemTypeRef } from "typings/store";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { DescItemTypeFields } from "./DescItemTypeFields";
import { FormItemGroup } from "./FormItemGroup";
import { GroupColumns } from "./GroupColumns";
import { FormItem } from "./formItems";
import { useStyles } from "./styles";
import { buildGroupsForm } from "./utils";

interface Props {
  formItems: FormItem[];
  forcedFormItems: FormItem[];
  addedFormItems: FormItem[];
  itemTypes: FormItemType[];
  columnCount: number;
  plain?: boolean;
  /** Show a spinner while the grouped view is empty. NodeEdit relies on this because it has no
   * separate loading flag; wrappers with their own loading guard (StructureEdit) leave it off. */
  spinnerWhenEmpty?: boolean;
  addEmptyDescItem: (typeId: number, specId?: number, position?: number) => string | void;
  deleteDescItem: (item: any, localId: string) => Promise<void>;
  createDescItem: (item: any, localId: string) => Promise<any>;
  updateDescItem: (item: any, localId?: string) => void | Promise<void>;
  exportCsv?: (item: any) => void;
  importCsv?: (item: any, file: File) => Promise<void>;
  fondsVersionId?: number;
  nodeId?: number;
  nodeVersionId?: number;
  nodeSetting?: any;
  isFirstNode?: boolean;
  handleCopyFromPrev?: (descItemTypeId: number) => void;
  handleCopyToggle?: (descItemTypeId: number) => void;
  getOpenInDataGridHref?: (descItemTypeId: number) => string;
  onOpenInDataGrid?: (descItemTypeId: number) => void;
  hideCopyButtons?: boolean;
  renderExtraActions?: (typeRef: DescItemTypeRef) => ReactNode;
  autoFocusLocalId?: string;
  onAutoFocusTaken?: () => void;
}

export type { Props as ItemFormBodyProps };

const noop = () => {};

/**
 * Shared render body for the desc-item forms: builds the grouped view of the
 * combined item lists and lays it out in columns. Data loading, mutations and
 * form-specific chrome (toolbars, add-type buttons) live in the wrapper that
 * owns the corresponding data hook (NodeEdit, StructureEdit, OutputEdit).
 */
export function ItemFormBody({
  formItems,
  forcedFormItems,
  addedFormItems,
  itemTypes,
  columnCount,
  plain = false,
  spinnerWhenEmpty = false,
  addEmptyDescItem,
  deleteDescItem,
  createDescItem,
  updateDescItem,
  exportCsv,
  importCsv,
  fondsVersionId,
  nodeId,
  nodeVersionId,
  nodeSetting,
  isFirstNode = true,
  handleCopyFromPrev = noop,
  handleCopyToggle = noop,
  getOpenInDataGridHref,
  onOpenInDataGrid,
  hideCopyButtons,
  renderExtraActions,
  autoFocusLocalId,
  onAutoFocusTaken,
}: Props) {
  const styles = useStyles();
  const itemTypeRefs = useAppSelector(({ refTables }) => refTables.descItemTypes.itemsMap);
  const groupRefs = useAppSelector(({ refTables }) => refTables.groups.data);

  const groups = useMemo(() => {
    if (!groupRefs) {
      return [];
    }
    return buildGroupsForm(
      [...formItems, ...forcedFormItems, ...addedFormItems],
      itemTypes,
      groupRefs,
      itemTypeRefs,
    );
  }, [groupRefs, itemTypeRefs, formItems, forcedFormItems, addedFormItems, itemTypes]);

  if (groups.length === 0 && spinnerWhenEmpty) {
    return (
      <div className={styles.spinnerPadding}>
        <Spinner />
      </div>
    );
  }

  return (
    <GroupColumns groups={groups} columnCount={columnCount}>
      {({ group, descItemTypes }) => (
        <FormItemGroup key={group.code} group={group} plain={plain}>
          {descItemTypes.map(({ typeRef, typeForm, typeWidth, descItems }) => (
            <DescItemTypeFields
              key={typeRef.id}
              typeRef={typeRef}
              typeForm={typeForm}
              typeWidth={typeWidth}
              descItems={descItems}
              fondsVersionId={fondsVersionId}
              nodeId={nodeId}
              nodeVersionId={nodeVersionId}
              nodeSetting={nodeSetting}
              isFirstNode={isFirstNode}
              handleCopyFromPrev={handleCopyFromPrev}
              handleCopyToggle={handleCopyToggle}
              getOpenInDataGridHref={getOpenInDataGridHref}
              onOpenInDataGrid={onOpenInDataGrid}
              addEmptyDescItem={addEmptyDescItem}
              deleteDescItem={deleteDescItem}
              createDescItem={createDescItem}
              updateDescItem={updateDescItem}
              exportCsv={exportCsv}
              importCsv={importCsv}
              hideCopyButtons={hideCopyButtons}
              renderExtraActions={renderExtraActions}
              autoFocusLocalId={autoFocusLocalId}
              onAutoFocusTaken={onAutoFocusTaken}
            />
          ))}
        </FormItemGroup>
      )}
    </GroupColumns>
  );
}
