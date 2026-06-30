import { Api } from "api";
import { downloadExportFile } from "actions/global/downloadExportFile";
import type { AccessPointBatchExportParams } from "elza-api";

/**
 * Trigger an asynchronous CSV export of access points matching the given filter.
 *
 * Returns the request id and chains into {@link downloadExportFile} which polls /io/export-status
 * and downloads /io/file once the export is finished. The filter shape mirrors the scalar
 * parameters of the registry-list search (apTypeId, scopeId, state, revState, search, ...);
 * the advanced SearchFilterVO ("Použít rozšířený filtr" / "Moje úkoly") is not yet supported.
 */
export function batchExportAccessPoints(params: AccessPointBatchExportParams) {
    return async (dispatch) => {
        const { data: fileId } = await Api.accesspoints.accessPointBatchExport(params);
        dispatch(downloadExportFile(fileId));
    };
}
