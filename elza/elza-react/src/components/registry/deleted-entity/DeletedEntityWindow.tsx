import {
  Button,
  DataGrid,
  DataGridBody,
  DataGridCell,
  DataGridHeader,
  DataGridHeaderCell,
  DataGridRow,
  createTableColumn,
} from "@fluentui/react-components";
import { ChevronLeftRegular, ChevronRightRegular } from "@fluentui/react-icons";
import { extSystemListFetchIfNeeded } from "actions/admin/extSystem";
import { Api } from "api";
import { DeletedEntity } from "elza-api";
import { useEffect, useState } from "react";
import { Modal } from "react-bootstrap";
import { useIntl } from "react-intl";
import { useSelector } from "react-redux";
import { Link } from "react-router-dom";
import { AppState } from "typings/store";
import { useThunkDispatch } from "utils/hooks";
import { urlEntity } from "../../../constants";
import { messages } from "./messages";

const PAGE_SIZE = 200;

interface DeletedEntityEx extends DeletedEntity {
  extSystemName?: string;
}

const _columns = [
  createTableColumn<DeletedEntityEx>({
    columnId: "id",
    renderCell: ({ accessPointId }) => (
      <Link to={urlEntity(accessPointId)}>{accessPointId}</Link>
    ),
  }),
  createTableColumn<DeletedEntityEx>({
    columnId: "name",
    renderCell: ({ name }) => name,
  }),
  createTableColumn<DeletedEntityEx>({
    columnId: "description",
    renderCell: ({ description }) => description,
  }),
  createTableColumn<DeletedEntityEx>({
    columnId: "extSystem",
    renderCell: ({ extSystemId, bindingValue, extSystemName }) =>
      extSystemId ? `${extSystemName || extSystemId}: ${bindingValue}` : "",
  }),
  createTableColumn<DeletedEntityEx>({
    columnId: "deleteDate",
    renderCell: ({ deleteDate }) => {
      const _deleteDate = new Date(deleteDate);
      return `${_deleteDate.toLocaleDateString()}, ${_deleteDate.toLocaleTimeString()}`;
    },
  }),
  createTableColumn<DeletedEntityEx>({
    columnId: "replacedBy",
    renderCell: ({ replacedBy }) => (
      <Link to={urlEntity(replacedBy)}>{replacedBy}</Link>
    ),
  }),
];

export function DeletedEntityWindow() {
  const [deletedEntities, setDeletedEntities] = useState<DeletedEntityEx[]>([]);
  const [totalCount, setTotalCount] = useState<number>(0);
  const [page, setPage] = useState<number>(0);

  const extSystems = useSelector(({ app }: AppState) => app.extSystemList.rows);
  const dispatch = useThunkDispatch();

  const { formatMessage } = useIntl();

  useEffect(() => {
    dispatch(extSystemListFetchIfNeeded());
  }, []);

  useEffect(() => {
    (async () => {
      const { data } = await Api.accesspoints.accessPointGetInvalidatedEntities(
        page,
        PAGE_SIZE,
      );
      const entitiesWithExtSystems: DeletedEntityEx[] = data.page.map(
        (entity) => {
          const extSystem = extSystems.find(
            ({ id }) => entity.extSystemId == id,
          );
          return { ...entity, extSystemName: extSystem?.name };
        },
      );
      setDeletedEntities(entitiesWithExtSystems);
      setTotalCount(data.totalCount);
    })();
  }, [page]);

  const pageCount = Math.ceil(totalCount / PAGE_SIZE) || 1;
  const columnSizingOptions = {
    id: {
      defaultWidth: 50,
    },
    replacedBy: {
      defaultWidth: 50,
    },
  };

  return (
    <Modal.Body
      style={{ height: "200px", display: "flex", flexDirection: "column" }}
    >
      <div>
        <Button
          icon={<ChevronLeftRegular />}
          onClick={() => setPage(page > 0 ? page - 1 : 0)}
        />
        <span style={{ margin: "0 5px" }}>
          {page + 1}/{pageCount}
        </span>
        <Button
          icon={<ChevronRightRegular />}
          onClick={() =>
            setPage(page < pageCount - 1 ? page + 1 : pageCount - 1)
          }
        />
      </div>
      <div className={`fund-search`} style={{ overflow: "auto" }}>
        <DataGrid
          items={deletedEntities}
          columns={_columns}
          style={{ minWidth: "700px" }}
          resizableColumns={true}
          columnSizingOptions={columnSizingOptions}
        >
          <DataGridHeader>
            <DataGridRow>
              {({ columnId }) => (
                <DataGridHeaderCell>
                  {formatMessage(messages[columnId])}
                </DataGridHeaderCell>
              )}
            </DataGridRow>
          </DataGridHeader>
          <DataGridBody<DeletedEntityEx>>
            {({ item, rowId }) => (
              <DataGridRow<DeletedEntityEx> key={rowId}>
                {({ renderCell }) => (
                  <DataGridCell>{renderCell(item)}</DataGridCell>
                )}
              </DataGridRow>
            )}
          </DataGridBody>
        </DataGrid>
      </div>
    </Modal.Body>
  );
}
