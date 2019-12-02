package cz.tacr.elza.dataexchange.input.reader;

import javax.xml.namespace.QName;

/**
 * Class can dynamically build path to the xml element
 * 
 * XmlElementPathBuilder is working only with local path (without namespace
 * prefixes), this may change in future.
 */
public class XmlElementPath {

    private final StringBuilder sb;

    private String path;

    /**
     * Copy path inner state.
     */
    public XmlElementPath(XmlElementPath src) {
        this.sb = new StringBuilder(src.sb);
    }

    public XmlElementPath(int capacity) {
        this.sb = new StringBuilder(capacity);
    }

    @Override
    public String toString() {
        return path != null ? path : (path = sb.toString());
    }

    /**
     * Enter XML element. Path is updated on this child or sibling.
     */
    public void enterElement(QName qName) {
        String name = qName.getLocalPart();
        int length = name.length();
        sb.ensureCapacity(sb.length() + length + 1);
        sb.append('/');
        for (int i = 0; i < length; i++) {
            sb.append(name.charAt(i));
        }
        path = null;
    }

    /**
     * Leave XML element. Path is updated on parent path.
     */
    public void leaveElement(QName qName) {
        String name = qName.getLocalPart();
        // check that we are inside correct element
        int startPos = sb.length() - name.length() - 1;
        // check separator
        if (sb.charAt(startPos) != '/') {
            throw new IllegalStateException("Incorrect path, value: " + sb.toString() + ", expected element: " + name);
        }
        for (int i = 0; i < name.length(); i++) {
            if (sb.charAt(startPos + 1 + i) != name.charAt(i)) {
                throw new IllegalStateException("Incorrect path, value: " + sb.toString() + ", expected element: "
                        + name);
            }
        }

        // path match -> can be shorten
        sb.setLength(sb.length() - name.length() - 1);
        path = null;
    }

    /**
     * Test if specified path is equal or children path.
     */
    public boolean matchPath(CharSequence path) {
        if (path == null || path.length() < sb.length()) {
            return false;
        }
        if (path.length() > sb.length() && path.charAt(sb.length()) != '/') {
            return false;
        }
        for (int i = sb.length() - 1; i >= 0; i--) {
            if (sb.charAt(i) != path.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Test if specified path is equal or children path.
     */
    public boolean matchPath(XmlElementPath path) {
        if (path == null) {
            return false;
        }
        return matchPath(path.sb);
    }

    /**
     * Test if specified path is equal.
     */
    public boolean equalPath(CharSequence path) {
        if (path == null || path.length() != sb.length()) {
            return false;
        }
        for (int i = sb.length() - 1; i >= 0; i--) {
            if (sb.charAt(i) != path.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Test if specified path is equal.
     */
    public boolean equalPath(XmlElementPath path) {
        if (path == null) {
            return false;
        }
        return equalPath(path.sb);
    }

    /**
     * @return Copy inner state of path.
     */
    public XmlElementPath copy() {
        return new XmlElementPath(this);
    }
}
