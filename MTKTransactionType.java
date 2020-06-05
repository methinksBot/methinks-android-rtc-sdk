package io.methinks.android.mtkrtc;

public enum MTKTransactionType {
    info,
    success,
    ack,
    reate,
    attach,
    detach,
    message,
    create,
    destroy,
    trickle,
    keepalive,
    event,
    error,
    webrtcup,
    hangup,
    detached,
    media,
    plugin_handle_message,
    plugin_handle_webrtc_message;

    @Override
    public String toString() {
        return name();
    }

    enum MessageType{
        configure,
        rtp_forward,
        stop_rtp_forward;

        @Override
        public String toString() {
            return name();
        }
    }
}
