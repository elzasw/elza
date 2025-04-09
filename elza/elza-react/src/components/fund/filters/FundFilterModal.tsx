import { Button } from "@fluentui/react-components";
import { DraggableWindow } from "components/shared";
import { Position } from "components/shared/draggable-window";
import { useState } from "react";
import { FundFilterInstitutionForm } from "./FundFilterInstitution";
import { useIntl } from "react-intl";
import { messages } from "./messages";
import { FondsFilterField } from "elza-api";

export interface FilterChange {
  name: FondsFilterField;
  value: string;
}

interface Props {
  filterName: FondsFilterField;
  onFilterChange: (data: FilterChange) => void;
  onClose: () => void;
  initialPosition?: Position;
}

export function FundFilterModal({
  filterName,
  onFilterChange,
  onClose = () => { console.warn("'onClose' not defined") },
  initialPosition,
}: Props) {
  const [value, setValue] = useState<string>("");
  const { formatMessage } = useIntl();

  if (!filterName) {
    return <></>
  }


  const handleFilterChange = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    console.log("#fp", filterName);

    onFilterChange({
      name: filterName,
      value,
    });
  }

  function handleClose() {
    onClose();
  }

  return <DraggableWindow disableDrag={true} initialPosition={initialPosition}>
    <div style={{
      padding: '10px 12px',
      // fontWeight: 'bold',
      // fontSize: '2em',
      background: 'white',
      borderRadius: '5px',
      boxShadow: '0 0 6px 0 #0003',
      zIndex: 10,
    }} >
      <div>
        {formatMessage(messages[filterName])}
        &nbsp;
        {/* <Input value={value} onChange={({ currentTarget }) => setValue(currentTarget.value)} /> */}
        <FundFilterInstitutionForm onChange={(value) => setValue(value)} />
      </div>
      <div style={{ display: "flex", justifyContent: "flex-end", marginTop: "10px" }}>
        <Button appearance="primary" size="small" onClick={handleFilterChange}>Potvrdit</Button>
        <Button size="small" onClick={handleClose}>Zavrit</Button>
      </div>
    </div>
  </DraggableWindow>;
}

