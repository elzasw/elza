import React, { useState, useEffect, useCallback } from 'react';
import { Autocomplete } from '..';

interface AsyncAutocompleteProps<T> {
    getItems: (searchText: string) => Promise<T[]>;
}

export const AsyncAutocomplete = <T,>({getItems, ...otherProps}:AsyncAutocompleteProps<T>) => {
    const [items, setItems] = useState<T[]>([]);

    const requestItems = useCallback(async (searchText: string) => {
        const response = await getItems(searchText)
        setItems(response);
    }, [getItems])

    useEffect(() => { requestItems(""); }, [])

    return <Autocomplete {...otherProps} items={items} onSearchChange={requestItems}/>
}
