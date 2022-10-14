// Checks if given string is in uuid format
export const isUuid = (string?: string) => {
    if(string == null){return false;}
    const uuidExp = /^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$/gi;
    return uuidExp.test(string);
}

export const isInteger = (string?: string) => {
    if(string == null){return false;}
    const integerExp = /^[0-9]+$/gi;
    return integerExp.test(string);
}
