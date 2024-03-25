import React, { useRef, useEffect } from 'react';
import { Modal } from "react-bootstrap";
import { useThunkDispatch } from 'utils/hooks';
import { addToastrSuccess } from 'components/shared/toastr/ToastrActions';
import { ExternalSystem } from 'typings/store';
import { Button } from 'components/ui';
import { ModalDialogWrapper } from 'components/shared';
import i18n from 'components/i18n';
import { MODAL_DIALOG_VARIANT } from '../../../../../../constants';

export interface Props {
    geometry: string;
    extSystem: ExternalSystem;
    onChange?: (geometry: string) => void;
    onClose?: () => void;
    allowedGeometryTypes?: string[];
    title?: string;
}

export const MapEditor = ({
    geometry,
    extSystem,
    onChange = () => console.warn("'onChange' not defined"),
    onClose = () => console.warn("'onClose' not defined"),
    allowedGeometryTypes,
    title,
}: Props) => {
    const iframeRef = useRef<HTMLIFrameElement>(null);
    const queryStartIndex = extSystem.url?.indexOf("?");
    const queryString = queryStartIndex != undefined && queryStartIndex >= 0 ? extSystem.url?.substring(queryStartIndex + 1) : undefined;
    const baseUrl = queryStartIndex != undefined && queryStartIndex >= 0 ? extSystem.url?.substring(0, queryStartIndex) : extSystem.url;

    useEffect(() => {
        const receiveMessage = (e: MessageEvent) => {
            const { current: iframe } = iframeRef;
            if (!extSystem.url) { return; }

            if (e.data?.type === "MapViewInit") {
                console.log("#### init message received", e.data)
                if (iframe && iframe.contentWindow) {
                    iframe?.contentWindow?.postMessage({
                        type: 'MapGeometry',
                        geometry,
                        requestId: 'test',
                        geometryFormat: "WKT",
                        initialRegion: geometry,
                    }, extSystem.url)
                }
            }
            else if (e.data?.type === "MapError") {
                throw Error(e.data.message);
            }
            else if (e.data?.type === "MapGeometry") {
                console.log("#### geometry message received", e.data)
                onChange(e.data.geometry);
            }
        }

        window.addEventListener("message", receiveMessage);

        return () => {
            window.removeEventListener("message", receiveMessage);
        }
    }, [geometry])

    const getQueryParams = ({ origin, allowedGeometryTypes, apiKey }: { origin: string, allowedGeometryTypes?: string[], apiKey?: string }) => {
        const query = new URLSearchParams(queryString);
        query.set("SOURCE_ORIGIN", origin);
        query.set("MODE", "EDIT");

        if (allowedGeometryTypes && allowedGeometryTypes.length > 0) {
            query.set("ALLOWED_GEOMETRY_TYPES", allowedGeometryTypes.join(","))
        }
        if (apiKey) {
            query.set("API_KEY", apiKey)
        }

        return query;
    }

    return <>
        <ModalDialogWrapper
            className={MODAL_DIALOG_VARIANT.FULLSCREEN}
            title={title || i18n('ap.coordinate.map-editor.title')}
            onHide={onClose}
        >
            <iframe
                title="elza-map-iframe"
                className={'border-0 float-left'}
                ref={iframeRef}
                src={`${baseUrl}?${getQueryParams({ origin: window.location.origin, apiKey: extSystem.apiKeyValue, allowedGeometryTypes })}`}
                style={{
                    height: 500,
                    width: "100%",
                    flexGrow: 1,
                }}
            />
        </ModalDialogWrapper>
        {/* <Modal.Footer> */}
        {/*     <Button type="submit" variant="outline-secondary" > */}
        {/*         Ok */}
        {/*     </Button> */}
        {/* </Modal.Footer> */}
    </>


}
