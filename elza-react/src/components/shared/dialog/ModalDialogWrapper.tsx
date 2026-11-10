import { PropsWithChildren, useEffect, useRef, useState} from 'react';
import ReactDOM from 'react-dom';
import {Modal} from 'react-bootstrap';
import {setInputFocus} from 'components/Utils.jsx';
import { FluentProvider, PortalMountNodeProvider } from '@fluentui/react-components';

/**
 * Obal modálního dialogu
 */
export interface Props extends PropsWithChildren {
    className: string;
    title?: React.ReactNode;
    onHide?: () => void;
    closeOnClickOutside?: boolean;
    visible?: boolean;
}

export const ModalDialogWrapper = ({
    title,
    className,
    children,
    onHide = () => {return;},
    closeOnClickOutside = false,
    visible = true,
}: Props) => {
    const modalBody = useRef<HTMLDivElement>(null);
    const hide = useRef(false);
    // Fluent portals (dialogs, menus, popovers) opened from the content are mounted inside this
    // bootstrap modal: its focus trap moves focus back to the modal whenever the focused element
    // lies outside of it, so a Fluent dialog in the default body portal could not keep focus.
    const [portalMountNode, setPortalMountNode] = useState<HTMLDivElement | null>(null);

    useEffect(()=>{
        if (modalBody) {
            const el = ReactDOM.findDOMNode(modalBody.current);
            if (el) {
                setInputFocus(el, false);
            }
        }
    },[])

    const handleHide = () => {
        if (!hide.current) {
            onHide && onHide();
            hide.current = true;
        }
    };

    return (
        <Modal
            backdrop={closeOnClickOutside ? undefined : 'static'}
            className={`${visible ? 'dialog-visible' : 'dialog-hidden'} ${className}`}
            show={true}
            onHide={handleHide}
        >
            <FluentProvider style={{ flex: 1, display: "flex", flexDirection: "column"}}>

                {title !== null && (
                    <Modal.Header closeButton style={{flexShrink: 0}}>
                        <Modal.Title>{title}</Modal.Title>
                    </Modal.Header>
                )}

                <div style={{flexGrow: 1, minHeight: 0, minWidth: 0}} ref={modalBody} className="modal-body-container">
                    <PortalMountNodeProvider value={portalMountNode ?? undefined}>
                        {children}
                    </PortalMountNodeProvider>
                </div>
                <div ref={setPortalMountNode} />
            </FluentProvider>
        </Modal>
    );
}
