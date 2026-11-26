package io.descoped.dc.api.node;

/**
 * The encoding of a request body.
 */
public enum FormEncoding {
    /**
     * Plain text.
     */
    TEXT_PLAIN("text/plain"),
    /**
     * URL encoded form data.
     */
    APPLICATION_X_WWW_FORM_URLENCODED("application/x-www-form-urlencoded"),
    /**
     * Multipart form data.
     */
    MULTIPART_FORM_DATA("multipart/form-data");

    private final String mimeType;

    FormEncoding(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * The encoding's MIME type.
     *
     * @return the MIME type
     */
    public String getMimeType() {
        return mimeType;
    }
}
