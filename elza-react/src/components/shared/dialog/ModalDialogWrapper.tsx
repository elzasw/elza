import { PropsWithChildren, useEffect, useRef} from 'react';
import ReactDOM from 'react-dom';
import {Modal} from 'react-bootstrap';
import {setInputFocus} from 'components/Utils.jsx';

/**
 * Obal modálního dialogu
 */
export interface Props extends PropsWithChildren {
    className: string;
    title?: string;
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
            maskClosable={false}
        >
            {title !== null && (
                <Modal.Header closeButton>
                    <Modal.Title>{title}</Modal.Title>
                </Modal.Header>
            )}

            <div ref={modalBody} className="modal-body-container">
                {children}
            </div>
        </Modal>
    );
}
