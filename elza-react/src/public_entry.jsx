/*eslint-disable */
if (!__DEV__ && serverContextPath != null) {
    __webpack_public_path__ = serverContextPath + '/';
} else if (__DEV__) {
    __webpack_public_path__ = 'http://localhost:3000/'
}
/*eslint-enable */
