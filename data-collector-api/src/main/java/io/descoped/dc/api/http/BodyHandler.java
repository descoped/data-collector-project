package io.descoped.dc.api.http;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Flow;

/**
 * A subscriber that receives a response body and makes it into a value.
 *
 * @param <T> the value's type
 */
public interface BodyHandler<T> extends Flow.Subscriber<List<ByteBuffer>> {

    /**
     * Body outcome
     *
     * @return whatever outcome that was processed by the subscriber
     */
    T body();

}
