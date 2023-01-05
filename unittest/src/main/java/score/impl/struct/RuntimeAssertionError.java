package score.impl.struct;

public class RuntimeAssertionError extends RuntimeException {

    public RuntimeAssertionError(String msg, Throwable t) {
        super(msg, t);
    }
    public static RuntimeException unexpected(Throwable t) {
        throw new RuntimeAssertionError("Unexpected throwable", t);
    }
}
