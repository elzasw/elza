export const getFileName = (name: string): string => {
    return name ? name.substring(name.lastIndexOf("/") + 1) : "-";
}

export const turncate = (str: string ): string => {
    if (str.length <= 20) {
        return str;
    }
    return str.slice(0, 17) + '...';
}