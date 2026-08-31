package dev.fauza.tdeh.codec;

/**
 * Raised when a hologram entry in {@code data.yml} cannot be read. The message names the offending
 * key so an operator who hand-edited the file can find it, and the loader skips only that hologram
 * rather than dropping the whole file.
 */
public class CodecException extends RuntimeException {

    public CodecException(String message) {
        super(message);
    }

    public CodecException(String message, Throwable cause) {
        super(message, cause);
    }
}
