import { useCallback, useState, useRef, PropsWithChildren, createContext, useContext } from 'react';
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
    // useNativeDrag?: boolean;
    disableDrag?: boolean;
    dragWholeWindow?: boolean;
}

interface DraggableWindowContextType {
    handleMouseDown: (e: React.MouseEvent) => void,
    handleMouseMove: (e: MouseEvent) => void,
    handleMouseUp: () => void,
    handleDragStart: (e: React.MouseEvent) => void,
    handleDragEnd: (e: React.MouseEvent) => void,
}

export const DraggableWindowContext = createContext<DraggableWindowContextType>(null);

export const DraggableWindow = ({
    children,
    className,
    initialPosition,
    onDragStop = () => { return; },
    // useNativeDrag = false,
    disableDrag = false,
    dragWholeWindow = false,
}: Props) => {
    const _draggableWindowPosition = useRef(initialPosition || { x: window.innerWidth / 2, y: window.innerHeight / 2 });
    const _draggableWindowDiff = useRef({ x: 0, y: 0 });
    const [position, setPosition] = useState(_draggableWindowPosition.current);
    const [dragging, setDragging] = useState(false);
    const _window = useRef<HTMLDivElement>(null);

    const handleMove = useCallback((e: MouseEvent) => {
        if (/* useNativeDrag || */ disableDrag) return;
        const newPosition = {
            x: e.clientX - _draggableWindowDiff.current.x,
            y: e.clientY - _draggableWindowDiff.current.y,
        }
        if (newPosition.y < 0) {
            newPosition.y = 0;
        }

        _draggableWindowPosition.current = newPosition;

        setPosition(newPosition);
    }, [/* useNativeDrag, */ disableDrag]);

    const handleMouseUp = useCallback(() => {
        setDragging(false);
        onDragStop(_draggableWindowPosition.current);
        document.removeEventListener("mousemove", handleMove);
        document.removeEventListener("mouseup", handleMouseUp);
    }, [handleMove, onDragStop])

    const handleMouseDown = useCallback((e: React.MouseEvent) => {
        if (/* useNativeDrag || */ disableDrag) return;
        const offsetLeft = _window.current?.offsetLeft || 0;
        const offsetTop = _window.current?.offsetTop || 0;
        setDragging(true);

        _draggableWindowDiff.current = { x: e.clientX - offsetLeft, y: e.clientY - offsetTop };
        document.addEventListener("mousemove", handleMove)
        document.addEventListener("mouseup", handleMouseUp)
    }, [handleMove, handleMouseUp, /* useNativeDrag, */ disableDrag])

    const handleDragEnd = useCallback((e: React.MouseEvent) => {
        console.log('#dw - drag end');
        if (/* !useNativeDrag || */ disableDrag) return;
        const newPosition = {
            x: e.clientX - _draggableWindowDiff.current.x,
            y: e.clientY - _draggableWindowDiff.current.y,
        }
        setPosition(newPosition);
        onDragStop(newPosition);
        console.log('position: ', newPosition);

        setDragging(false);
    }, [onDragStop/* , useNativeDrag */, disableDrag])

    const handleDragStart = useCallback((e: React.MouseEvent) => {
        console.log('#dw - drag start');
        if (/* !useNativeDrag || */ disableDrag) return;
        const offsetLeft = _window.current?.offsetLeft || 0;
        const offsetTop = _window.current?.offsetTop || 0;
        _draggableWindowDiff.current = { x: e.clientX - offsetLeft, y: e.clientY - offsetTop };

        console.log('position: ', _draggableWindowDiff.current);
        setDragging(true);
    }, [/* useNativeDrag,  */disableDrag])

    return <DraggableWindowContext.Provider value={{
        handleDragStart,
        handleDragEnd,
        handleMouseDown,
        handleMouseUp,
        handleMouseMove: handleMove
    }}>
        <div>
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
                // draggable={useNativeDrag && !disableDrag}
                onMouseDown={dragWholeWindow ? handleMouseDown : undefined}
                onDragStart={dragWholeWindow ? handleDragStart : undefined}
                onDragEnd={dragWholeWindow ? handleDragEnd : undefined}
            >
                {/* x:{_draggableWindowPosition.current.x} y:{_draggableWindowPosition.current.y} */}
                {children}
            </div>
            {dragging && <div style={{ position: "fixed", top: 0, left: 0, width: "100vw", height: "100vh", zIndex: 1000 }} />}
        </div>
    </DraggableWindowContext.Provider>
}

export function DraggableWindowDragger(props: React.HTMLProps<HTMLDivElement>) {
    const { handleDragStart, handleDragEnd, handleMouseDown } = useContext(DraggableWindowContext);
    return <div
        {...props}
        className={classNames("dragger", props.className)}
        onMouseDown={handleMouseDown}
        onDragStart={handleDragStart}
        onDragEnd={handleDragEnd}
    />
}
