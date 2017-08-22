import {i18n} from 'components/shared';
import {addToastr} from "components/shared/toastr/ToastrActions.jsx";

/**
 *  Funkce pro stažení souboru
 *  @param id {String} Identifikátor pro frame
 *  @param url {String} URL stahovaného souboru
 */
export function downloadFile(id,url) {
    return (dispatch) => {
        const frameId = "downloadFrame-"+id;
        const timerInterval = 4000;
        const timerTimeout = 60000;

        const createToaster = (title,message,type) => {
            dispatch(addToastr(title, message, type, "lg", timerInterval));
        };

        if(document.getElementById(frameId)) { //Vypíše upozornění pokud existuje frame se stejným id
            createToaster(i18n("download.allreadyDownloading"), "", "info");
            return;
        }

        const downloadFrame = document.createElement('iframe');
        downloadFrame.src = url;
        downloadFrame.id = frameId;
        downloadFrame.style.display="none";
        document.body.appendChild(downloadFrame);

        let counter = 0;
        const timer = setInterval(function () {
            counter++;
            const iframeDoc = downloadFrame.contentDocument || (downloadFrame.contentWindow && downloadFrame.contentWindow.document); //načte document framu
            const timedOut = counter > timerTimeout/timerInterval;

            //Pokud je frame načten (začalo stahování) nebo vypršel čas, smaže vytvořený frame a případně vypíše upozornění, že čas vypršel.
            if (timedOut || iframeDoc.readyState == 'complete' || iframeDoc.readyState == 'interactive') {
                timedOut && createToaster(i18n("download.error.title"), i18n("download.error.timeout"), "warning");
                clearInterval(timer);
                downloadFrame.parentElement.removeChild(downloadFrame);
            }
        }, timerInterval);
    }
}
