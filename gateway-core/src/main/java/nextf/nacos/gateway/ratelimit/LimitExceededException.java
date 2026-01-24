package nextf.nacos.gateway.ratelimit;

public class LimitExceededException extends Exception {
    protected LimitExceededException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public LimitExceededException(Throwable cause) {
        super(cause);
    }

    public LimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }

    public LimitExceededException(String message) {
        super(message);
    }

    public LimitExceededException() {
        super();
    }
}
