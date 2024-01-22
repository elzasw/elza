import {useCallback, useState, useRef, PropsWithChildren} from 'react';

let _draggableWindowPosition = {x:window.innerWidth/2, y:window.innerHeight/2};
let _draggableWindowDiff = {x:0, y:0};

interface Props extends PropsWithChildren {
    className: string;
}

export const DraggableWindow = ({
    children,
    className
}: Props) => {
    const [position, setPosition] = useState(_draggableWindowPosition);
    const [dragging, setDragging] = useState(false);
    const window = useRef<HTMLDivElement>(null);

    const handleMove = useCallback((e: MouseEvent) => {
        e.preventDefault();
        e.stopPropagation();

        const newPosition = {
            x: e.clientX - _draggableWindowDiff.x,
            y: e.clientY - _draggableWindowDiff.y,
        }

        _draggableWindowPosition = newPosition;

        setPosition(newPosition);
    }, []);

    const handleMouseUp = useCallback(() => {
        setDragging(false);
        document.removeEventListener("mousemove", handleMove);
        document.removeEventListener("mouseup", handleMouseUp);
    },[handleMove])

    const handleMouseDown = useCallback((e:React.MouseEvent)=>{
        e.preventDefault();
        e.stopPropagation();
        const offsetLeft = window.current?.offsetLeft || 0;
        const offsetTop = window.current?.offsetTop || 0;
        setDragging(true);

        _draggableWindowDiff = {x: e.clientX - offsetLeft, y: e.clientY - offsetTop};
        document.addEventListener("mousemove", handleMove)
        document.addEventListener("mouseup", handleMouseUp)
    },[handleMove, handleMouseUp])

    return <div
        className={`draggable-window ${className}`}
        ref={window}
        style={{
            top: `${position.y}px`,
            left: `${position.x}px`,
        }}
        onMouseDown={handleMouseDown}
    >
        {dragging && <div style={{position: "fixed", top: 0, left: 0, width: "100vw", height: "100vh", zIndex: 1000}}/>}
        {children}
    </div>
}
