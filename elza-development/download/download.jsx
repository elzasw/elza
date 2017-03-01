import {i18n} from 'components/index.jsx';
import {addToastr} from "components/shared/toastr/ToastrActions.jsx";

/**
 *  Funkce pro stažení souboru
 *  @param id {String} Identifikátor pro frame
 *  @param url {String} URL stahovaného souboru
 */
export function downloadFile(id,url) {
    return (dispatch) => {
        var frameId = "downloadFrame-"+id;
        var timerInterval = 4000;
        var timerTimeout = 60000;

        var createToaster = (title,message,type) => {
            dispatch(addToastr(title, message, type, "lg", timerInterval));
        };

        if(document.getElementById(frameId)){ //Vypíše upozornění pokud existuje frame se stejným id
            createToaster("Požadavek na stažení souboru již byl odeslán", "", "info");
            return;
        }

        var downloadFrame = document.createElement('iframe');
        downloadFrame.src = url;
        downloadFrame.id = frameId;
        downloadFrame.style.display="none";
        document.body.appendChild(downloadFrame);

        var counter = 0;
        var timer = setInterval(function () {
            counter++;
            var iframeDoc = downloadFrame.contentDocument || (downloadFrame.contentWindow && downloadFrame.contentWindow.document); //načte document framu
            var timedOut = counter > timerTimeout/timerInterval;

            //Pokud je frame načten (začalo stahování) nebo vypršel čas, smaže vytvořený frame a případně vypíše upozornění, že čas vypršel.
            if (timedOut || iframeDoc.readyState == 'complete' || iframeDoc.readyState == 'interactive') {
                timedOut && createToaster("Chyba stahování", "Vypršel časový limit na obsluhu požadavku", "warning");
                clearInterval(timer);
                downloadFrame.parentElement.removeChild(downloadFrame);
                return;
            }
        }, timerInterval);
    }
}
