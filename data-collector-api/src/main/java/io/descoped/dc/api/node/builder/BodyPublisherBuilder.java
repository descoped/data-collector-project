package io.descoped.dc.api.node.builder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.descoped.dc.api.node.Base;
import io.descoped.dc.api.node.BodyPart;
import io.descoped.dc.api.node.BodyPublisher;
import io.descoped.dc.api.node.BodyPublisherProducer;
import io.descoped.dc.api.node.FormEncoding;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds a {@link BodyPublisher}, the request body of a post or put: plain text, URL-encoded form data or multipart
 * form data. Each method sets the body's encoding; the last one called decides it.
 */
@JsonDeserialize(using = NodeBuilderDeserializer.class)
public class BodyPublisherBuilder extends OperationPublisherBuilder {

    @JsonIgnore
    FormEncoding encoding;
    @JsonProperty("plainTextData")
    BodyPublisherProducerBuilder plainText;
    @JsonProperty("urlEncodedData")
    BodyPublisherProducerBuilder urlEncodedData;
    @JsonProperty("partsData")
    List<BodyPart> parts = new ArrayList<>();

    /**
     * Creates the builder of an empty body.
     */
    public BodyPublisherBuilder() {
        super(BuilderType.BodyPublisher);
    }

    /**
     * Sets a plain text body.
     *
     * @param text the text
     * @return this builder
     */
    public BodyPublisherBuilder plainText(String text) {
        this.plainText = new StringBodyPublisherProducerBuilder(text);
        this.encoding = FormEncoding.TEXT_PLAIN;
        return this;
    }

    /**
     * Sets a plain text body whose content a producer gives.
     *
     * @param bodyPublisherProducerBuilder the producer's builder
     * @return this builder
     */
    public BodyPublisherBuilder plainText(BodyPublisherProducerBuilder bodyPublisherProducerBuilder) {
        this.plainText = bodyPublisherProducerBuilder;
        this.encoding = FormEncoding.TEXT_PLAIN;
        return this;
    }

    /**
     * Sets a URL-encoded form body.
     *
     * @param data the URL-encoded data
     * @return this builder
     */
    public BodyPublisherBuilder urlEncoded(String data) {
        this.urlEncodedData = new StringBodyPublisherProducerBuilder(data);
        this.encoding = FormEncoding.APPLICATION_X_WWW_FORM_URLENCODED;
        return this;
    }

    /**
     * Sets a URL-encoded form body whose content a producer gives.
     *
     * @param bodyPublisherProducerBuilder the producer's builder
     * @return this builder
     */
    public BodyPublisherBuilder urlEncoded(BodyPublisherProducerBuilder bodyPublisherProducerBuilder) {
        this.urlEncodedData = bodyPublisherProducerBuilder;
        this.encoding = FormEncoding.APPLICATION_X_WWW_FORM_URLENCODED;
        return this;
    }

    /**
     * Adds a text part to a multipart form body, in UTF-8.
     *
     * @param name  the part's name
     * @param value the part's text
     * @return this builder
     */
    public BodyPublisherBuilder textPart(String name, String value) {
        this.encoding = FormEncoding.MULTIPART_FORM_DATA;
        this.parts.add(new BodyPart(name, value, StandardCharsets.UTF_8));
        return this;
    }

    /**
     * Adds a file part to a multipart form body.
     *
     * @param name     the part's name
     * @param filename the file's name
     * @param value    the file's content
     * @return this builder
     */
    public BodyPublisherBuilder formPart(String name, String filename, byte[] value) {
        this.encoding = FormEncoding.MULTIPART_FORM_DATA;
        this.parts.add(new BodyPart(name, filename, value, StandardCharsets.UTF_8));
        return this;
    }

    /**
     * Adds a file part to a multipart form body, with text content in UTF-8.
     *
     * @param name     the part's name
     * @param filename the file's name
     * @param value    the file's content
     * @return this builder
     */
    public BodyPublisherBuilder formPart(String name, String filename, String value) {
        this.encoding = FormEncoding.MULTIPART_FORM_DATA;
        this.parts.add(new BodyPart(name, filename, value, StandardCharsets.UTF_8));
        return this;
    }

    @SuppressWarnings("unchecked") // the caller names the node type it builds
    @Override
    public <R extends Base> R build(BuildContext buildContext) {
        BodyPublisherProducer plainTextProducer = plainText == null ? null : plainText.build(buildContext);
        BodyPublisherProducer urlEncodedDataProducer = urlEncodedData == null ? null : urlEncodedData.build(buildContext);
        return (R) new BodyPublisherNode(encoding, plainTextProducer, urlEncodedDataProducer, parts);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BodyPublisherBuilder that = (BodyPublisherBuilder) o;
        return encoding == that.encoding &&
                Objects.equals(plainText, that.plainText) &&
                Objects.equals(urlEncodedData, that.urlEncodedData) &&
                Objects.equals(parts, that.parts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), encoding, plainText, urlEncodedData, parts);
    }

    @Override
    public String toString() {
        return "BodyPublisherBuilder{" +
                "encoding=" + encoding +
                ", plainText='" + plainText + '\'' +
                ", urlEncodedData='" + urlEncodedData + '\'' +
                ", parts=" + parts +
                '}';
    }

    static class BodyPublisherNode extends LeafNode implements BodyPublisher {

        private final FormEncoding encoding;
        private final BodyPublisherProducer plainText;
        private final BodyPublisherProducer urlEncodedData;
        private final List<BodyPart> parts;

        public BodyPublisherNode(FormEncoding encoding, BodyPublisherProducer plainText, BodyPublisherProducer urlEncodedData, List<BodyPart> parts) {
            this.encoding = encoding;
            this.plainText = plainText;
            this.urlEncodedData = urlEncodedData;
            this.parts = parts;
        }

        @Override
        public FormEncoding getEncoding() {
            return encoding;
        }

        @Override
        public BodyPublisherProducer getPlainText() {
            return plainText;
        }

        @Override
        public BodyPublisherProducer getUrlEncodedData() {
            return urlEncodedData;
        }

        @Override
        public List<BodyPart> getParts() {
            return parts;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BodyPublisherNode that = (BodyPublisherNode) o;
            return encoding == that.encoding &&
                    Objects.equals(plainText, that.plainText) &&
                    Objects.equals(urlEncodedData, that.urlEncodedData) &&
                    Objects.equals(parts, that.parts);
        }

        @Override
        public int hashCode() {
            return Objects.hash(encoding, plainText, urlEncodedData, parts);
        }

        @Override
        public String toString() {
            return "BodyPublisherNode{" +
                    "encoding=" + encoding +
                    ", plainText='" + plainText + '\'' +
                    ", urlEncodedData='" + urlEncodedData + '\'' +
                    ", parts=" + parts +
                    '}';
        }
    }

}
