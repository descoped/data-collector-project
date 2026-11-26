package io.descoped.dc.api.node;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.nio.charset.Charset;
import java.util.Objects;

/**
 * A part of a multipart request body: a text part, or a form part with a file name.
 */
public class BodyPart {

    /**
     * The part's name.
     */
    public final String name;
    /**
     * The part's file name, or {@code null} for a text part.
     */
    public final String filename;
    /**
     * The part's value: a string or bytes.
     */
    public final Object value;
    /**
     * The charset of the part's value.
     */
    public final Charset charset;

    /**
     * Creates a text part.
     *
     * @param name    the name
     * @param value   the value
     * @param charset the charset of the value
     */
    public BodyPart(String name, Object value, Charset charset) {
        this.name = name;
        this.filename = null;
        this.value = value;
        this.charset = charset;
    }

    /**
     * Creates a form part with a file name.
     *
     * @param name     the name
     * @param filename the file name
     * @param value    the value
     * @param charset  the charset of the value
     */
    public BodyPart(String name, String filename, Object value, Charset charset) {
        this.name = name;
        this.filename = filename;
        this.value = value;
        this.charset = charset;
    }

    /**
     * Whether this is a text part: it has no file name.
     *
     * @return {@code true} for a text part
     */
    @JsonIgnore
    public boolean isTextPart() {
        return filename == null;
    }

    /**
     * Whether this is a form part: it has a file name.
     *
     * @return {@code true} for a form part
     */
    @JsonIgnore
    public boolean isFormPart() {
        return filename != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BodyPart part = (BodyPart) o;
        return Objects.equals(name, part.name) &&
                Objects.equals(filename, part.filename) &&
                Objects.equals(value, part.value) &&
                Objects.equals(charset, part.charset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, filename, value, charset);
    }

    @Override
    public String toString() {
        return "BodyPart{" +
                "name='" + name + '\'' +
                ", filename='" + filename + '\'' +
                ", value=" + value +
                ", charset=" + charset +
                '}';
    }
}
