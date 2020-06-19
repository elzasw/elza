
export function bindParams(url: string, params: Record<string, any>): string {
    let newUrl = url;
    Object.keys(params).forEach(prop => {
        const value = params[prop];
        newUrl = newUrl.replace('{' + prop + '}', value);
    });
    return newUrl;
}
