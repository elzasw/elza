package cz.tacr.elza.common.string;

import java.util.List;

public class PrepareForCompare {

    final StringBuilder sb = new StringBuilder();
    final private List<String> input;

    boolean wordStarted = false;
    boolean firstWordFinished = false;

    public PrepareForCompare(List<String> input) {
        this.input = input;
    }

    @Override
    public String toString() {
        for (String s : input) {
            append(s);
        }
        return sb.toString();
    }

    private void append(String s) {
        // process chars
        for(int i=0; i<s.length(); i++) {
            char c = s.charAt(i);
            boolean isDigit = Character.isDigit(c);
            boolean isAlphabetic = Character.isAlphabetic(c);
            // Convert to lowercase
            if(isAlphabetic) {
                c = Character.toLowerCase(c);
            }

            // check if word should be started
            if (isDigit || isAlphabetic) {
                // check if previous word exists
                if (!wordStarted && firstWordFinished) {
                    sb.append(' ');
                }
                wordStarted = true;
                sb.append(c);
            } else {
                // something else -> finish word
                if (wordStarted) {
                    if (!firstWordFinished) {
                        firstWordFinished = true;
                    }
                    wordStarted = false;
                }
            }
        }
        // close last word
        if (wordStarted) {
            if (!firstWordFinished) {
                firstWordFinished = true;
            }
            wordStarted = false;
        }
    }

    public static String prepare(List<String> input) {
        PrepareForCompare pfc = new PrepareForCompare(input);
        return pfc.toString();
    }
}
