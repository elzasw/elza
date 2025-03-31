import { useCallback, useState, useRef, PropsWithChildren } from 'react';
import "./DraggableWindow.scss";
import classNames from 'classnames';

export interface Position {
    x: number;
    y: number;
}

export interface Props extends PropsWithChildren {
    className?: string;
    initialPosition?: Position;
    onDragStop?: (position: Position) => void;
    useNativeDrag?: boolean;
    disableDrag?: boolean;
}

export const DraggableWindow = ({
    children,
    className,
    initialPosition,
    onDragStop = () => { return; },
    useNativeDrag = false,
    disableDrag = false,
}: Props) => {
    const _draggableWindowPosition = useRef(initialPosition || { x: window.innerWidth / 2, y: window.innerHeight / 2 });
    const _draggableWindowDiff = useRef({ x: 0, y: 0 });
    const [position, setPosition] = useState(_draggableWindowPosition.current);
    const [dragging, setDragging] = useState(false);
    const _window = useRef<HTMLDivElement>(null);

    const handleMove = useCallback((e: MouseEvent) => {
        const newPosition = {
            x: e.clientX - _draggableWindowDiff.current.x,
            y: e.clientY - _draggableWindowDiff.current.y,
        }

        _draggableWindowPosition.current = newPosition;

        setPosition(newPosition);
    }, []);

    const handleMouseUp = useCallback(() => {
        setDragging(false);
        onDragStop(_draggableWindowPosition.current);
        document.removeEventListener("mousemove", handleMove);
        document.removeEventListener("mouseup", handleMouseUp);
    }, [handleMove, onDragStop])

    const handleMouseDown = useCallback((e: React.MouseEvent) => {
        const offsetLeft = _window.current?.offsetLeft || 0;
        const offsetTop = _window.current?.offsetTop || 0;
        setDragging(true);

        _draggableWindowDiff.current = { x: e.clientX - offsetLeft, y: e.clientY - offsetTop };
        document.addEventListener("mousemove", handleMove)
        document.addEventListener("mouseup", handleMouseUp)
    }, [handleMove, handleMouseUp])

    const handleDragEnd = useCallback((e: React.MouseEvent) => {
        const newPosition = {
            x: e.clientX - _draggableWindowDiff.current.x,
            y: e.clientY - _draggableWindowDiff.current.y,
        }
        setPosition(newPosition);
        onDragStop(newPosition);

        setDragging(false);
    }, [onDragStop])

    const handleDragStart = useCallback((e: React.MouseEvent) => {
        const offsetLeft = _window.current?.offsetLeft || 0;
        const offsetTop = _window.current?.offsetTop || 0;
        _draggableWindowDiff.current = { x: e.clientX - offsetLeft, y: e.clientY - offsetTop };

        setDragging(true);
    }, [])

    return <div>
        <div
            className={classNames({
                "draggable-window": true,
                "drag-disable": disableDrag,
            }, className)}
            ref={_window}
            style={{
                top: `${position.y}px`,
                left: `${position.x}px`,
            }}
            onMouseDown={!useNativeDrag && !disableDrag ? handleMouseDown : undefined}
            draggable={useNativeDrag && !disableDrag}
            onDragStart={useNativeDrag && !disableDrag ? handleDragStart : undefined}
            onDragEnd={useNativeDrag && !disableDrag ? handleDragEnd : undefined}
        >
            {/* x:{_draggableWindowPosition.current.x} y:{_draggableWindowPosition.current.y} */}
            {children}
        </div>
        {dragging && <div style={{ position: "fixed", top: 0, left: 0, width: "100vw", height: "100vh", zIndex: 1000 }} />}
    </div>
}
