import { makeStyles, shorthands, mergeClasses, tokens } from '@fluentui/react-components';
import { Children, useState } from 'react';
import { PropsWithChildren } from 'react';
import { ReOrderDotsVerticalRegular } from "@fluentui/react-icons";
import { getNodeChildren } from '../ArrUtils';
import { useUserSettings } from 'contexts/user';
import { FIELD_HEIGHT } from '../../../constants';

const useStyles = makeStyles({
    dropTargetContainer: {
        position: 'relative',
    },
    dropTarget: {
        // width: '100%',
        width: 'calc(100% - 32px)',
        border: 'var(--primary-border)',
        ...shorthands.borderStyle('dashed'),
        ...shorthands.borderWidth(0),
        height: 0,
        opacity: 0,
        borderRadius: '4px',
        transition: 'height 500ms cubic-bezier(.19,1,.22,1), opacity 500ms cubic-bezier(.19,1,.22,1), margin 500ms cubic-bezier(.19,1,.22,1)',
        // background: 'green',
    },
    dropTargetActive: {
        ...shorthands.borderWidth('2px'),
        height: '10px',
        margin: '2px 0',
        opacity: 1,
        // background: 'rgba(0, 200, 0, 0.3)', // TEMP debug
    },
    dropTargetHovered: {
        height: '32px',
        opacity: 1,
        // background: 'rgba(0, 150, 0, 0.6)', // TEMP debug
        transition: 'height 500ms cubic-bezier(.19,1,.22,1) 200ms, opacity 500ms cubic-bezier(.19,1,.22,1), margin 500ms cubic-bezier(.19,1,.22,1)',
    },
    dropTargetArea: {
        position: 'absolute',
        // width: '100%',
        width: 'calc(100% + 18px)',
        marginLeft: '-18px',
        zIndex: 1000,
        // background: 'rgba(0, 0, 255, 0.2)', // TEMP debug
        top: '-5px',
        // transform: 'translateY(-50%)',
        height: '20px',
        transition: 'height 500ms cubic-bezier(.19,1,.22,1), opacity 500ms cubic-bezier(.19,1,.22,1)',
    },
    dropTargetAreaHovered: {
        height: '42px',
        // background: 'rgba(0, 0, 255, 0.4)', // TEMP debug
        transition: 'height 500ms cubic-bezier(.19,1,.22,1) 200ms, opacity 500ms cubic-bezier(.19,1,.22,1)',
    },
    draggerContainer: {
        width: "5px",
        position: 'relative',
        margin: '2px 0',
    },
    draggerArea: {
        position: 'absolute',
        // background: 'rgba(128, 0, 128, 0.3)', // TEMP debug
        width: '10px',
        height: '100%',
        right: '0px',
        zIndex: 501,
        transition: 'width 500ms cubic-bezier(.19,1,.22,1)',
        // opacity: 0,
    },
    draggerAreaHovered: {
        width: '25px',
        // background: 'rgba(128, 0, 128, 0.6)', // TEMP debug
    },
    dragger: {
        border: 'var(--primary-border)',
        position: 'absolute',
        width: '40px',
        top: '0',
        // left: '-8px',
        left: '0px',
        height: '100%',
        background: 'var(--shade-0)',
        zIndex: 0,
        borderRadius: '4px 0 0 4px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'flex-start',
        transition: 'left 500ms cubic-bezier(.19,1,.22,1), opacity 500ms cubic-bezier(.19,1,.22,1)',
        opacity: 0.5,
        borderRight: 'none',
        padding: '4px',
    },
    draggerHovered: {
        left: '-18px',
        opacity: 1,
        // background: 'rgba(255, 165, 0, 0.8)', // TEMP debug
    }
})

interface DropTargetProps{
    isActive: boolean;
    isVisible: boolean;
    index: number;
    onDrop: (index: number) => void;
}

function DropTarget({onDrop, index, isActive, isVisible}: DropTargetProps) {
    const styles = useStyles();
    const [isHovered, setIsHovered] = useState(false);

    function handleDragEnter() {
        setIsHovered(true);
    }

    function handleDragLeave(e: React.DragEvent) {
        if (e.currentTarget.contains(e.relatedTarget as Node)) return;
        setIsHovered(false);
    }

    function handleDrop() {
        onDrop(index);
        setIsHovered(false);
    }

    if (!isVisible) {
        return <></>
    }

    return <div className={styles.dropTargetContainer}>
        {isActive && <div
            className={mergeClasses(
                styles.dropTargetArea,
                isHovered && isActive && styles.dropTargetAreaHovered,
            )}
            onDrop={handleDrop}
            onDragOver={(e) => e.preventDefault()}
            onDragEnter={handleDragEnter}
            onDragLeave={handleDragLeave}
            />}
        <div className={mergeClasses(
            styles.dropTarget,
            isActive && styles.dropTargetActive,
            isHovered && isActive && styles.dropTargetHovered,
        )}>

        </div>
    </div>
}

interface DraggerProps {
    index: number;
    isVisible: boolean;
    isActive: boolean;
    compact?: boolean;
    onDragIndexChange: (index: number | undefined) => void;
}

function Dragger({ index, isVisible, isActive, compact, onDragIndexChange }: DraggerProps) {
    const [isHovered, setIsHovered] = useState(false);
    const styles = useStyles();
    const height = compact ? FIELD_HEIGHT.small : FIELD_HEIGHT.medium;

    if (!isVisible) {
        return <></>
    }

    function handleDragStart(e: React.DragEvent<HTMLDivElement>, index: number) {
        const dragImage = e.currentTarget.parentElement.parentElement;
        // Defer out of the dragstart tick: a synchronous re-render here can replace
        // the source node mid-gesture, which cancels the drag.
        e.dataTransfer.setDragImage(dragImage, 0, 0);
        setTimeout(() => {
            onDragIndexChange(index);
        }, 0);
    }

    function handleMouseDown(index: number) {
        // onDragIndexChange(index);
        // addEventListener('mouseup', () => {
        //     onDragIndexChange(undefined);
        // }, { once: true });
    }

    function handleDragEnd() {
        onDragIndexChange(undefined);
    }

    function handleMouseEnter() {
        setIsHovered(true)
    }
    function handleMouseLeave(e: React.MouseEvent<HTMLDivElement>) {
        if (e.currentTarget.contains(e.relatedTarget as Node)) return;
        setIsHovered(false);
    }

    return <div className={styles.draggerContainer} style={{opacity: isActive ? 0.3 : undefined, height: height + 'px'}}>
        <div
            className={mergeClasses(
                styles.draggerArea,
                isHovered && styles.draggerAreaHovered,
            )}
            onMouseEnter={handleMouseEnter}
            onMouseOut={handleMouseLeave}
            onMouseDown={() => handleMouseDown(index)}
            onDragStart={(e) => handleDragStart(e, index)}
            onDragEnd={handleDragEnd}
            draggable={true}
        />
        <div
            //onMouseDown={() => handleMouseDown(index)}
            //onDragStart={(e) => handleDragStart(e, index)}
            //onDragEnd={handleDragEnd}
            className={mergeClasses(
                styles.dragger,
                isHovered && styles.draggerHovered,
            )}
        >
            <ReOrderDotsVerticalRegular />
        </div>
    </div>
}

interface Props {
    isItemDraggable?: (index: number) => boolean;
    canPlaceBeforeItem?: (index: number) => boolean;
    onChangeOrder?: (originalIndex: number, newIndex: number) => void;
}

export function DraggableList({
    children,
    isItemDraggable = () => true,
    canPlaceBeforeItem = () => true,
    onChangeOrder = () => { return; }
}: PropsWithChildren<Props>) {
    const childrenCount = Children.count(children);
    const [draggedIndex, setDraggedIndex] = useState<number>();
    const [lastIndex, setLastIndex] = useState<number>();
    const { settings } = useUserSettings();
    const compact = settings.compact;

    function handleDrop(index: number) {
        onChangeOrder(lastIndex, index);
    }

    function handleDragIndexChange(index: number | undefined) {
        setDraggedIndex(index);
        if (index != undefined) {
            setLastIndex(index);
        }
    }

    return (
        <>
            {Children.map(children, (child, index) => {
                return (
                    <>
                        <DropTarget
                        onDrop={handleDrop}
                        index={index}
                        isVisible={childrenCount > 1 && canPlaceBeforeItem(index)}
                        isActive={
                            draggedIndex != undefined
                            && (index < draggedIndex || index > draggedIndex + 1)
                        }
                        />
                        <div style={{ width: '100%', display: 'flex' }}>
                            <Dragger
                            isVisible={childrenCount > 1 && isItemDraggable(index)}
                            isActive={draggedIndex === index}
                            compact={compact}
                            index={index}
                            onDragIndexChange={handleDragIndexChange}
                            />

                            <div style={{
                                flex: 1,
                                opacity: draggedIndex === index ? '0.3' : undefined,
                            }}>{child}</div>
                        </div>
                    </>
                );
            })}
            <DropTarget
            onDrop={handleDrop}
            index={childrenCount}
            isVisible={childrenCount > 1}
            isActive={
                draggedIndex < childrenCount - 1
                && draggedIndex != undefined
            }
            />
        </>
    );
}
