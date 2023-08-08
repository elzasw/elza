import {i18n} from 'components/shared';
import {addToastr} from 'components/shared/toastr/ToastrActions.jsx';
import {createException} from 'components/ExceptionUtils.jsx';

/**
 *  Downloads file from specified url
 *  @param url {String} url of file
 */
export function downloadFile(url) {
    return dispatch => {
        var link = document.createElement('a');
        link.href = url;
        link.style.cssText = 'display:none';
        if (navigator.userAgent.toLowerCase().indexOf('firefox') > -1) {
            // Workaround for websocket problems in firefox
            link.download = 'unknown-filename';
        }
        document.body.appendChild(link);
        link.click();
        setTimeout(() => {
            link.parentElement.removeChild(link);
        }, 1000);
    };
}

/**
 *  Downloads binary file
 *  @param file {String} binary file
 *  @param name {String} name of the file
 */
export function downloadBlob(file, name) {
    const blob = new Blob([file]);
    var link = document.createElement('a');
    link.href = window.URL.createObjectURL(blob);
    link.download = name;
    document.body.appendChild(link);
    link.click();
    link.parentElement.removeChild(link);
}

/**
 *  Downloads file from specified url using frame to open the link
 *  @param id {String} id for frame
 *  @param url {String} url of file
 */
export function downloadFileInFrame(url, id) {
    return dispatch => {
        const frameId = 'downloadFrame-' + id;
        const timerInterval = 4000;
        const timerTimeout = 60000;

        const createToaster = (title, message, type) => {
            dispatch(addToastr(title, message, type, 'lg', timerInterval));
        };

        if (document.getElementById(frameId)) {
            //Vypíše upozornění pokud existuje frame se stejným id
            createToaster(i18n('download.allreadyDownloading'), '', 'info');
            return;
        }

        const downloadFrame = document.createElement('iframe');
        downloadFrame.src = url;
        downloadFrame.id = frameId;
        downloadFrame.style.display = 'none';
        document.body.appendChild(downloadFrame);

        let counter = 0;
        const timer = setInterval(function() {
            counter++;
            const iframeDoc =
                downloadFrame.contentDocument || (downloadFrame.contentWindow && downloadFrame.contentWindow.document); //načte document framu
            const timedOut = counter > timerTimeout / timerInterval;

            //Pokud je frame načten (začalo stahování) nebo vypršel čas, smaže vytvořený frame a případně vypíše upozornění, že čas vypršel.
            if (timedOut || iframeDoc.readyState === 'complete' || iframeDoc.readyState === 'interactive') {
                timedOut && createToaster(i18n('download.error.title'), i18n('download.error.timeout'), 'warning');
                clearInterval(timer);
                downloadFrame.parentElement.removeChild(downloadFrame);
            }
        }, timerInterval);
    };
}

/**
 *  Requests file through AJAX and saves it to blob. Makes fake download when completed.
 *  @param address {String} file request address
 *  @param filename {String} name of the file with which it will be saved when completed
 *  @param method {String} request method
 *  @param data {Object} request data
 *  @param contentType {String} request contentType
 */
export function downloadAjaxFile(address, filename, method = 'GET', data = null, contentType = 'application/json') {
    return dispatch => {
        var req = new XMLHttpRequest();
        req.open(method, address, true);
        req.responseType = 'blob';

        req.onload = function(event) {
            const {readyState, status} = event.target;
            if (readyState !== 4 || status !== 200) {
                const reader = new FileReader();
                reader.onloadend = e => {
                    // parse the read data to json and create exception
                    const data = JSON.parse(e.srcElement.result);
                    dispatch(createException(data));
                };
                // Read the response blob in the file reader
                reader.readAsText(event.target.response);
                return false;
            }
            var resContentType = req.getResponseHeader('Content-Type');
            var resContentDisp = req.getResponseHeader('Content-Disposition');
            if (resContentDisp && !filename) {
                filename = getFilenameFromDisposition(resContentDisp);
            } else if (!resContentDisp) {
                console.log('Missing content disposition header');
                filename = 'unknown-filename';
            }
            if (!resContentType) {
                console.log('Missing content type header');
            }
            var blob = new Blob([req.response], {type: resContentType});

            if (typeof window.navigator.msSaveBlob !== 'undefined') {
                // IE workaround for HTML7007, Edge workaround for Issue #7260192
                window.navigator.msSaveBlob(blob, filename);
            } else {
                var link = document.createElement('a');
                link.href = window.URL.createObjectURL(blob);
                link.download = filename;
                document.body.appendChild(link);
                link.click();
                link.parentElement.removeChild(link);
            }
        };

        if (method === 'POST' && data) {
            if (contentType === 'application/json') {
                data = JSON.stringify(data);
            }
            req.setRequestHeader('Content-type', contentType);
            req.send(data);
        } else {
            req.send();
        }
    };
}

function getFilenameFromDisposition(contentDisposition) {
    let filename = 'unknown-filename';
    var filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
    var matches = filenameRegex.exec(contentDisposition);
    if (matches !== null && matches[1]) {
        filename = matches[1].replace(/['"]/g, '');
    }
    return filename;
}
