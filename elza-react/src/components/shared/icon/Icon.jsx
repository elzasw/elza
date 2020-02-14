import React from "react";
import FontIcon from "components/shared/icon/FontIcon.jsx";
import * as Glyphs from "components/shared/icon/glyphs";
import * as Utils from "components/shared/icon/utils/IconUtils";
import "./Icon.scss";

const iconMap = {
    "folder": Utils.inCircle(Glyphs.Folder),
    "circle": Glyphs.Circle,
    "serie": Utils.inCircle(Glyphs.Serie),
    "sitemap": Utils.inCircle(Glyphs.Sitemap),
    "fileText": Utils.inCircle(Glyphs.FileText),
    "fileTextPart": Utils.inCircle(Glyphs.FileTextPart),
    "triangleExclamation": Utils.inCircle(Glyphs.TriangleExclamation)
}

const Icon = (props) => {
    const {glyph, ...otherProps} = props;

    if(iconMap[glyph]){
        let Glyph = iconMap[glyph];
        return (
            <svg className="svg-icon" fill={props.fill} viewBox="0 0 20 20">
                <Glyph secondaryStyle={props.secondaryStyle}/>
            </svg>
        )
    } else {
        return <FontIcon glyph={glyph} {...otherProps}/>
    }
}

export default Icon;



