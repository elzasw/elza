import { MenuCheckedValueChangeData, MenuCheckedValueChangeEvent } from "@fluentui/react-components";
import PublicationListColSelector from "./PublicationListColSelector";
import { PublishButton } from "../PublishButton";
import { PublicationType } from "elza-api";
import { useToolbarStyles } from "../styles";

type PublicationToolbarProps = {
    columns: string[];
    onColsChange: (e: MenuCheckedValueChangeEvent, data: MenuCheckedValueChangeData) => void;
    onPublish: () => void;
    publicationTypes: PublicationType[];
    fundId: number;
};

const PublicationToolbar = ({ columns, onColsChange, onPublish, publicationTypes, fundId }: PublicationToolbarProps) => {
    const classes = useToolbarStyles();

    return (
        <div className={classes.root}>
            <PublishButton onPublish={onPublish} types={publicationTypes} fundId={fundId} />
            <div style={{ flex: 1 }} />
            <PublicationListColSelector
                columns={columns}
                onChange={onColsChange}
            />
        </div>
    );
};

export default PublicationToolbar;
