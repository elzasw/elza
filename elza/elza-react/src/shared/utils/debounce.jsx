export default function debounce(fn, delay) {
    let timer = null;
    return function() {
        // console.log('debouncetimer', timer);
        return new Promise((resolve, reject) => {
            let context = this,
                args = arguments;
            clearTimeout(timer);
            timer = setTimeout(function() {
                resolve(fn.apply(context, args));
            }, delay);
        });
    };
}
