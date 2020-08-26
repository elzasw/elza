import React, { FC } from "react";
import { FileGridItem } from "./FileGridItem";
import { File } from "./types";
import "./FileGrid.scss";

export const FileGrid:FC<{
    items: File[];
    onDownload: (fileId: number) => void;
}> = ({
    items,
    onDownload,
}) => {
    return <div className="file-grid">
        {items.map((file)=>{
            return <FileGridItem 
                file={file}
                onDownload={onDownload}
            />
        })}
    </div>
}
