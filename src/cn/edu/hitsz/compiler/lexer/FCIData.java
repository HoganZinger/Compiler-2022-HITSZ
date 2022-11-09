package cn.edu.hitsz.compiler.lexer;


import java.io.BufferedReader;
import java.io.IOException;
import java.text.StringCharacterIterator;


/**
 * @author hogan
 */

public class FCIData {
    static char DONE = StringCharacterIterator.DONE;
    private String line = null;
    private StringCharacterIterator iterator = null;
    private final BufferedReader reader;

    FCIData(BufferedReader reader) {
        this.reader = reader;
    }

    public char current() {
        try {
            if (iterator != null) {
                if (iterator.current() != StringCharacterIterator.DONE) {
                    return iterator.current();
                } else {
                    line = null;
                }
            }
            if (line == null) {
                line = reader.readLine();
                if (line == null) {
                    return DONE;
                }
            }
            iterator = new StringCharacterIterator(line);
            return iterator.current();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public FCIData next() {
        try {
            if (iterator != null) {
                if (iterator.current() != StringCharacterIterator.DONE) {
                    iterator.next();
                } else {
                    line = null;
                }
            }
            if (line == null) {
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }
}

