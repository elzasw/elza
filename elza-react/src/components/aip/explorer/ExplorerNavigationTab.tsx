import { AREA_EXPLORER_ITEM } from "actions/aip/exp";
import { FC } from "react";
import { useSelector } from "react-redux";
import { storeFromArea } from "shared/utils";
import { AppState } from "typings/store";

const ExplorerNavigationTab: FC = () => {
    const {data} = useSelector((state: AppState) => storeFromArea(state, AREA_EXPLORER_ITEM));

    if(!data) return <></>

    let parents = ["Balíček"];
    let curr = data;
    while(curr != null) {
        parents.push(curr.label);
        curr = curr.parentFolder;
    }

   
    return (
        <>{parents.reverse().map(parent => (
            <>{parent} </>
        ))}</>
    );
}
export default ExplorerNavigationTab;

