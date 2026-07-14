import { useCallback, useState, useRef, useEffect, useSyncExternalStore, PropsWithChildren, createContext, useContext } from 'react';
import "./DraggableWindow.scss";
import classNames from 'classnames';

export interface Position {
    x: number;
    y: number;
}

const PINNED_MARGIN = 8;

// Windows sit above the toastr/context-menu layer (highest is toastr at 2001)
// but below the modal/floating-menu layer (9999). Each window's z-index is its
// position in this stack order, so the most recently focused window is on top.
const BASE_Z_INDEX = 2100;

let stackOrder: number[] = [];
const stackListeners = new Set<() => void>();

const emitStackChange = () => stackListeners.forEach(listener => listener());

const registerWindow = (id: number) => {
    stackOrder = [...stackOrder, id];
    emitStackChange();
};

const unregisterWindow = (id: number) => {
    stackOrder = stackOrder.filter(windowId => windowId !== id);
    emitStackChange();
};

const raiseWindow = (id: number) => {
    const alreadyOnTop = stackOrder[stackOrder.length - 1] === id;
    if (alreadyOnTop) return;
    stackOrder = [...stackOrder.filter(windowId => windowId !== id), id];
    emitStackChange();
};

const subscribeStack = (listener: () => void) => {
    stackListeners.add(listener);
    return () => { stackListeners.delete(listener); };
};

let nextWindowId = 0;

export interface Props extends PropsWithChildren {
    className?: string;
    initialPosition?: Position;
    onDragStop?: (position: Position) => void;
    // useNativeDrag?: boolean;
    disableDrag?: boolean;
    dragWholeWindow?: boolean;
    enablePinBottom?: boolean;
}

export interface ResizeInfo {
    width: number;
    height: number;
    minWidth: number;
    minHeight: number;
}

type ResizeEdge = "n" | "s" | "e" | "w" | "ne" | "nw" | "se" | "sw";

const RESIZE_HANDLE_SIZE = 8;

const RESIZE_EDGES: { edge: ResizeEdge, cursor: string, style: React.CSSProperties }[] = [
    { edge: "n", cursor: "ns-resize", style: { top: 0, left: RESIZE_HANDLE_SIZE, right: RESIZE_HANDLE_SIZE, height: RESIZE_HANDLE_SIZE } },
    { edge: "s", cursor: "ns-resize", style: { bottom: 0, left: RESIZE_HANDLE_SIZE, right: RESIZE_HANDLE_SIZE, height: RESIZE_HANDLE_SIZE } },
    { edge: "e", cursor: "ew-resize", style: { top: RESIZE_HANDLE_SIZE, bottom: RESIZE_HANDLE_SIZE, right: 0, width: RESIZE_HANDLE_SIZE } },
    { edge: "w", cursor: "ew-resize", style: { top: RESIZE_HANDLE_SIZE, bottom: RESIZE_HANDLE_SIZE, left: 0, width: RESIZE_HANDLE_SIZE } },
    { edge: "ne", cursor: "nesw-resize", style: { top: 0, right: 0, width: RESIZE_HANDLE_SIZE, height: RESIZE_HANDLE_SIZE } },
    { edge: "nw", cursor: "nwse-resize", style: { top: 0, left: 0, width: RESIZE_HANDLE_SIZE, height: RESIZE_HANDLE_SIZE } },
    { edge: "se", cursor: "nwse-resize", style: { bottom: 0, right: 0, width: RESIZE_HANDLE_SIZE, height: RESIZE_HANDLE_SIZE } },
    { edge: "sw", cursor: "nesw-resize", style: { bottom: 0, left: 0, width: RESIZE_HANDLE_SIZE, height: RESIZE_HANDLE_SIZE } },
];

// Pinned windows are anchored to the bottom-right, so only the top edge, left
// edge, and top-left corner can resize them.
const PINNED_RESIZE_EDGES: ResizeEdge[] = ["n", "w", "nw"];

interface DraggableWindowContextType {
    handleMouseDown: (e: React.MouseEvent) => void,
    handleMouseMove: (e: MouseEvent) => void,
    handleMouseUp: () => void,
    handleDragStart: (e: React.MouseEvent) => void,
    handleDragEnd: (e: React.MouseEvent) => void,
    enablePinBottom: boolean,
    pinnedBottom: boolean,
    togglePinBottom: () => void,
    registerResizable: (handlers: { onResizeStart: () => ResizeInfo, onResize: (width: number, height: number) => void }) => void,
    isResizingRef: React.MutableRefObject<boolean>,
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
    enablePinBottom = false,
}: Props) => {
    const _draggableWindowPosition = useRef(initialPosition || { x: window.innerWidth / 2, y: window.innerHeight / 2 });
    const _draggableWindowDiff = useRef({ x: 0, y: 0 });
    const [position, setPosition] = useState(_draggableWindowPosition.current);
    const [dragging, setDragging] = useState(false);
    const [pinnedBottom, setPinnedBottom] = useState(false);
    const _window = useRef<HTMLDivElement>(null);
    const _resizeHandlers = useRef<{ onResizeStart: () => ResizeInfo, onResize: (width: number, height: number) => void } | null>(null);
    const _isResizing = useRef(false);
    const [resizable, setResizable] = useState(false);
    const _windowId = useRef<number | null>(null);
    if (_windowId.current === null) {
        _windowId.current = nextWindowId++;
    }
    const windowId = _windowId.current;

    useEffect(() => {
        registerWindow(windowId);
        return () => unregisterWindow(windowId);
    }, [windowId]);

    const stackIndex = useSyncExternalStore(subscribeStack, () => stackOrder.indexOf(windowId));
    const zIndex = BASE_Z_INDEX + Math.max(0, stackIndex);

    const bringToFront = useCallback(() => raiseWindow(windowId), [windowId]);

    const dragDisabled = disableDrag || pinnedBottom;

    const clampPosition = useCallback((pos: Position): Position => {
        const width = _window.current?.offsetWidth || 0;
        const height = _window.current?.offsetHeight || 0;
        const maxX = Math.max(0, window.innerWidth - width);
        const maxY = Math.max(0, window.innerHeight - height);
        return {
            x: Math.min(Math.max(0, pos.x), maxX),
            y: Math.min(Math.max(0, pos.y), maxY),
        };
    }, []);

    useEffect(() => {
        const handleWindowResize = () => {
            if (pinnedBottom) return;
            const clamped = clampPosition(_draggableWindowPosition.current);
            _draggableWindowPosition.current = clamped;
            setPosition(clamped);
        };
        window.addEventListener("resize", handleWindowResize);
        return () => window.removeEventListener("resize", handleWindowResize);
    }, [clampPosition, pinnedBottom]);

    const togglePinBottom = useCallback(() => {
        setPinnedBottom(pinned => !pinned);
    }, []);

    const registerResizable = useCallback((handlers: { onResizeStart: () => ResizeInfo, onResize: (width: number, height: number) => void }) => {
        _resizeHandlers.current = handlers;
        setResizable(true);
    }, []);

    const handleResizeStart = useCallback((edge: ResizeEdge) => (event: React.MouseEvent) => {
        if (!_resizeHandlers.current) return;
        event.preventDefault();
        event.stopPropagation();

        // When pinned the bottom-right corner is fixed by CSS, so resizing the top
        // or left edge only changes the size — the origin must not move.
        const moveOrigin = !pinnedBottom;

        _isResizing.current = true;
        const { width: startWidth, height: startHeight, minWidth, minHeight } = _resizeHandlers.current.onResizeStart();
        const startPosition = { ..._draggableWindowPosition.current };
        const startX = event.clientX;
        const startY = event.clientY;

        const movesLeft = edge.includes("w");
        const movesTop = edge.includes("n");
        const changesWidth = edge.includes("w") || edge.includes("e");
        const changesHeight = edge.includes("n") || edge.includes("s");

        const onMove = (moveEvent: MouseEvent) => {
            const deltaX = moveEvent.clientX - startX;
            const deltaY = moveEvent.clientY - startY;

            let newWidth = startWidth;
            let newHeight = startHeight;
            const newPosition = { ...startPosition };

            if (changesWidth) {
                newWidth = Math.max(minWidth, startWidth + (movesLeft ? -deltaX : deltaX));
                if (movesLeft && moveOrigin) {
                    newPosition.x = startPosition.x + (startWidth - newWidth);
                }
            }
            if (changesHeight) {
                newHeight = Math.max(minHeight, startHeight + (movesTop ? -deltaY : deltaY));
                if (movesTop && moveOrigin) {
                    newPosition.y = Math.max(0, startPosition.y + (startHeight - newHeight));
                }
            }

            _resizeHandlers.current?.onResize(newWidth, newHeight);
            _draggableWindowPosition.current = newPosition;
            setPosition(newPosition);
        };
        const onUp = () => {
            document.removeEventListener("mousemove", onMove);
            document.removeEventListener("mouseup", onUp);
            onDragStop(_draggableWindowPosition.current);
            // Clear on the next frame so the ResizeObserver's trailing echo of the
            // final size is still ignored instead of overwriting the state we set.
            requestAnimationFrame(() => { _isResizing.current = false; });
        };
        document.addEventListener("mousemove", onMove);
        document.addEventListener("mouseup", onUp);
    }, [pinnedBottom, onDragStop]);

    const handleMove = useCallback((e: MouseEvent) => {
        if (/* useNativeDrag || */ dragDisabled) return;
        const newPosition = {
            x: e.clientX - _draggableWindowDiff.current.x,
            y: e.clientY - _draggableWindowDiff.current.y,
        }
        if (newPosition.y < 0) {
            newPosition.y = 0;
        }

        _draggableWindowPosition.current = newPosition;

        setPosition(newPosition);
    }, [/* useNativeDrag, */ dragDisabled]);

    const handleMouseUp = useCallback(() => {
        setDragging(false);
        onDragStop(_draggableWindowPosition.current);
        document.removeEventListener("mousemove", handleMove);
        document.removeEventListener("mouseup", handleMouseUp);
    }, [handleMove, onDragStop])

    const handleMouseDown = useCallback((e: React.MouseEvent) => {
        if (/* useNativeDrag || */ dragDisabled) return;
        const offsetLeft = _window.current?.offsetLeft || 0;
        const offsetTop = _window.current?.offsetTop || 0;
        setDragging(true);

        _draggableWindowDiff.current = { x: e.clientX - offsetLeft, y: e.clientY - offsetTop };
        document.addEventListener("mousemove", handleMove)
        document.addEventListener("mouseup", handleMouseUp)
    }, [handleMove, handleMouseUp, /* useNativeDrag, */ dragDisabled])

    const handleDragEnd = useCallback((e: React.MouseEvent) => {
        console.log('#dw - drag end');
        if (/* !useNativeDrag || */ dragDisabled) return;
        const newPosition = {
            x: e.clientX - _draggableWindowDiff.current.x,
            y: e.clientY - _draggableWindowDiff.current.y,
        }
        setPosition(newPosition);
        onDragStop(newPosition);
        console.log('position: ', newPosition);

        setDragging(false);
    }, [onDragStop/* , useNativeDrag */, dragDisabled])

    const handleDragStart = useCallback((e: React.MouseEvent) => {
        console.log('#dw - drag start');
        if (/* !useNativeDrag || */ dragDisabled) return;
        const offsetLeft = _window.current?.offsetLeft || 0;
        const offsetTop = _window.current?.offsetTop || 0;
        _draggableWindowDiff.current = { x: e.clientX - offsetLeft, y: e.clientY - offsetTop };

        console.log('position: ', _draggableWindowDiff.current);
        setDragging(true);
    }, [/* useNativeDrag,  */dragDisabled])

    return <DraggableWindowContext.Provider value={{
        handleDragStart,
        handleDragEnd,
        handleMouseDown,
        handleMouseUp,
        handleMouseMove: handleMove,
        enablePinBottom,
        pinnedBottom,
        togglePinBottom,
        registerResizable,
        isResizingRef: _isResizing,
    }}>
        <div>
            <div
                className={classNames({
                    "draggable-window": true,
                    "drag-disable": dragDisabled,
                }, className)}
                ref={_window}
                onMouseDownCapture={bringToFront}
                style={pinnedBottom ? {
                    top: "auto",
                    bottom: 0,
                    left: "auto",
                    right: `${PINNED_MARGIN}px`,
                    zIndex,
                } : {
                    top: `${position.y}px`,
                    left: `${position.x}px`,
                    zIndex,
                }}
                // draggable={useNativeDrag && !disableDrag}
                onMouseDown={dragWholeWindow ? handleMouseDown : undefined}
                onDragStart={dragWholeWindow ? handleDragStart : undefined}
                onDragEnd={dragWholeWindow ? handleDragEnd : undefined}
            >
                {resizable && RESIZE_EDGES
                    .filter(({ edge }) => !pinnedBottom || PINNED_RESIZE_EDGES.includes(edge))
                    .map(({ edge, cursor, style }) => (
                        <div
                            key={edge}
                            onMouseDown={handleResizeStart(edge)}
                            style={{ position: "absolute", zIndex: 10002, cursor, ...style }}
                        />
                    ))}
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
