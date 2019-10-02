const getItemName = (item) => {
    if (!item.name || item.name.length <= 0) {
        return "id: "+item.id;
    } 

    return item.name;
}

export default getItemName;
