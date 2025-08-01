import { downloadFile } from "actions/global/download";
import { Api, getFullPath } from "api";
import { i18n } from "components";
import { createException } from "components/ExceptionUtils";
import { addToastrInfo, removeToastr, addToastrSuccess } from "components/shared/toastr/ToastrActions";
import { RequestProcessState, IoApiAxiosParamCreator } from "elza-api";

// opakovane dotazovani na stav exportu, konci stazenim souboru ci hlaskou o neuspechu
export function downloadExportFile(fileId: number, interval = 4000, toastKey = undefined) {
  return async (dispatch, getState) => {
    // toastKey obsahuje key posledne vytvoreneho toastu, coz je info toast o generovani
    if (!toastKey) {
      // pokud toastKey neni predan, vytvorim info toast
      dispatch(addToastrInfo(i18n('export.generating'), undefined, undefined, null));
      const { toastr } = getState();
      // ziskani lastKey ze statu, pomoci nehoz toast odstranime pri (ne)uspechu exportu
      toastKey = toastr.lastKey;
    }
    try {
      // ziskani stavu exportu, overrideErrorHandler: true pro zabraneni vychoziho zobrazeni chybove hlasky
      const { data } = await Api.io.ioGetExportStatus(fileId, { overrideErrorHandler: true });
      // pri stavu "Finished" muzeme soubor stahnout
      if (data.state === RequestProcessState.Finished) {
        // odstraneni info toastu pomoci toastKey o generovani exportu
        dispatch(removeToastr(toastKey));
        // hlaska o uspesnem exportu
        dispatch(addToastrSuccess(i18n('export.success'), undefined, undefined, 4000));
        // ziskani cesty k souboru
        const { url } = await IoApiAxiosParamCreator().ioGetExportFile(fileId);
        // stazeni souboru
        dispatch(downloadFile(getFullPath(url)));
      } else {
        // pri jinych stavech (PENDING/PREPARING) - opetovne zavolani funkce s danym intervalem
        setTimeout(() => dispatch(downloadExportFile(fileId, interval, toastKey)), interval);
      }
    } catch (error) {
      // pri chybe/neuspechu odstranim info toast a vypisi informace o chybe
      const code = error.response.data.code;
      dispatch(removeToastr(toastKey));
      dispatch(
        createException({
          ...error.response.data,
          code: code === 'CANT_EXPORT_DELETED_AP' ? 'CANT_EXPORT_DELETED_AP' : 'GENERATING_EXPORT_FAILED',
        }),
      );
    }
  };
}
