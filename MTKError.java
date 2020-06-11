package io.methinks.android.rtc;

public class MTKError {
    protected Domain errorDomain;
    protected ErrorCode errorCode;
    protected String errorMessage;
    protected Exception exception;

    public MTKError(ErrorCode errorCode) {
        this.errorMessage = "(null description)";
        this.errorCode = errorCode;
    }

    protected MTKError(Domain errorDomain, int errorCode, String msg) {
        this.errorMessage = msg == null ? "(null description)" : msg;
        this.errorDomain = errorDomain;
        this.errorCode = ErrorCode.fromTypeCode(errorCode);
    }

    protected MTKError(Domain errorDomain, int errorCode, Exception exception) {
        this.errorMessage = "(null description)";
        this.errorDomain = errorDomain;
        this.errorCode = ErrorCode.fromTypeCode(errorCode);
        this.exception = exception;
    }

    public Domain getErrorDomain() {
        return this.errorDomain;
    }

    public ErrorCode getErrorCode() {
        return this.errorCode;
    }

    public String getMessage() {
        return this.errorMessage;
    }

    public Exception getException() {
        return this.exception;
    }

    public static enum Domain {
        SessionErrorDomain,
        PublisherErrorDomain,
        SubscriberErrorDomain;

        private Domain() {
        }
    }

    public static enum ErrorCode {
        UnknownError(-1),
        AuthorizationFailure(1004),
        InvalidSessionId(1005),
        ConnectionFailed(1006),
        NoMessagingServer(1503),
        ConnectionRefused(1023),
        SessionStateFailed(1020),
        P2PSessionMaxParticipants(1403),
        SessionConnectionTimeout(1021),
        SessionInternalError(2000),
        SessionInvalidSignalType(1461),
        SessionSignalDataTooLong(1413),
        SessionSignalTypeTooLong(1414),
        ConnectionDropped(1022),
        SessionDisconnected(1010),
        PublisherInternalError(2000),
        PublisherWebRTCError(1610),
        PublisherUnableToPublish(1500),
        PublisherUnexpectedPeerConnectionDisconnection(1710),
        PublisherCannotAccessCamera(1650),
        PublisherCameraAccessDenied(1670),
        ConnectionTimedOut(1542),
        SubscriberSessionDisconnected(1541),
        SubscriberWebRTCError(1600),
        SubscriberServerCannotFindStream(1604),
        SubscriberStreamLimitExceeded(1605),
        SubscriberInternalError(2000),
        UnknownPublisherInstance(2003),
        UnknownSubscriberInstance(2004),
        SessionNullOrInvalidParameter(1011),
        VideoCaptureFailed(3000),
        CameraFailed(3010),
        VideoRenderFailed(4000),
        SessionSubscriberNotFound(1112),
        SessionPublisherNotFound(1113),
        PublisherTimeout(1541),
        SessionBlockedCountry(1026),
        SessionConnectionLimitExceeded(1027),
        SessionUnexpectedGetSessionInfoResponse(2001),
        SessionIllegalState(1015),
        ServiceIntentIsNull(1016);

        private int code;

        private ErrorCode(int code) {
            this.code = code;
        }

        public int getErrorCode() {
            return this.code;
        }

        public static ErrorCode fromTypeCode(int id) {
            ErrorCode[] var1 = values();
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                ErrorCode code = var1[var3];
                if (code.getErrorCode() == id) {
                    return code;
                }
            }

            return UnknownError;
        }
    }
}
