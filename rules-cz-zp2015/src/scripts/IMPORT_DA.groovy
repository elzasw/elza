package scripts


return generate(CLASS_NAME)

static String generate(final String className) {

    String result = null

    if (className.equals("Abstract") || className.equals("Unittitle")) {
        result = "ZP2015_CONTENT"
    } else if (className.equals("Unitdatestructured")) {
        result = "ZP2015_UNIT_DATE"
    }

    return result
}
