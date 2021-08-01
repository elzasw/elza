import React from 'react';

interface Props {
  name: string;
  description?: string,
}

/**
 * Vyrenderuje název archivní entity včetně popisu.
 * @param name
 * @param description
 * @constructor
 */
const ArchiveEntityName: React.FC<Props> = ({name, description}) => {
  let useDescription = description;
  if (useDescription) {
    if (useDescription.length > 53) {
      useDescription = useDescription.substring(0, 250) + '…';
    }
  }

  return <span>
    {name}{useDescription && <small title={description}> {useDescription}</small>}
  </span>
};

export default ArchiveEntityName;
