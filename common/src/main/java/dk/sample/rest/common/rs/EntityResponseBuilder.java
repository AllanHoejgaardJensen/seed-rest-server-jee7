package dk.sample.rest.common.rs;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import dk.sample.rest.common.persistence.jpa.AbstractAuditable;

/**
 * Builder to build {@link Response} instances based on assumptions on the returned concept.
 *
 * @param <E> Entity type that this builder expects
 * @param <R> Representation type that this builder expects
 */
public class EntityResponseBuilder<E, R> {

    /**
     * Hal+json media type string.
     */
    public static final String APPLICATION_HAL_JSON = "application/hal+json";

    /**
     * Hal+json media type.
     */
    public static final MediaType APPLICATION_HAL_JSON_TYPE = MediaType.valueOf(APPLICATION_HAL_JSON);


    private String name;
    private String version;
    private Integer maxAge;

    /**
     * mediatypes for servers with support for content-type parameters
     */
    private boolean supportsContentTypeParameter = true;
    private final E entity;
    private final Function<E, R> mapper;

    /**
     * Construct new builder giving the entity and a mapper able to map the entity to a concrete representation. If the
     * given entity is an implementation of {@link AbstractAuditable} the last modified time from this will be use in
     * the <code>last-modified</code> header.
     */
    public EntityResponseBuilder(E entity, Function<E, R> mapper) {
        this.entity = entity;
        this.mapper = mapper;
    }

    /**
     * Sets the concept name used in the <code>concept</code> content type parameter.
     */
    public EntityResponseBuilder<E, R> name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the concept version used in the <code>v</code> content type parameter.
     */
    public EntityResponseBuilder<E, R> version(String version) {
        this.version = version;
        return this;
    }

    /**
     * Sets the concept version used in the <code>v</code> content type parameter.
     */
    public EntityResponseBuilder<E, R> contentTypeParameters(boolean supported) {
        this.supportsContentTypeParameter = supported;
        return this;
    }

    /**
     * Sets the max age in seconds.
     */
    public EntityResponseBuilder<E, R> maxAge(int maxAge) {
        this.maxAge = maxAge;
        return this;
    }

    /**
     * Build a response given a concrete request. If the request contain an <code>if-modified-since</code> or
     * <code>if-none-match</code> header this will be checked against the entity given to the builder returning
     * a response with status not modified if appropriate.
     */
    public Response build(Request req) {
        EntityTag eTag = new EntityTag(Integer.toString(entity.hashCode()));
        Date lastModified = entity instanceof AbstractAuditable ? ((AbstractAuditable) entity).getLastModifiedTime() : Date.from(Instant.now());
        Response.ResponseBuilder notModifiedBuilder = req.evaluatePreconditions(lastModified, eTag);
        if (notModifiedBuilder != null) {
            return notModifiedBuilder.build();
        }

        Map<String, String> parameters = new ConcurrentHashMap<>();
        if (name != null) {
            parameters.put("concept", name);
        }
        if (version != null) {
            parameters.put("v", version);
        }
        MediaType type = getMediaType(parameters, supportsContentTypeParameter);

        Response.ResponseBuilder b = Response.ok(mapper.apply(entity))
                .type(type)
                .tag(eTag)
                .lastModified(lastModified);

        if (maxAge != null) {
            CacheControl cc = new CacheControl();
            cc.setMaxAge(maxAge);
            b.cacheControl(cc).expires(Date.from(Instant.now().plusSeconds(maxAge)));
        }

        return b.build();
    }

    /**
     * transforms content parameters delivered as "concept" and "v" e.g.
     *
     *  "concept", "Account"
     *  "v", "1.0.0"
     *
     *  into:
     *
     *  application/hal+json;concept=account;v=1.0.0
     *
     *  as content-type for situations where the use of the accept content-type parameter is supported
     *  and into:
     *
     *  application/hal+json+account+v1+0+0
     *
     *  as content-type for situations where the use of the accept content-type parameter is NOT supported
     *
     */
    public static MediaType getMediaType(Map<String, String> parameters, boolean supportsContentTypeParameter) {
        if (supportsContentTypeParameter) {
            return new MediaType("application", "hal+json", parameters);
        } else {
            return new MediaType("application", "hal+json+" + parameters.get("concept").toLowerCase()
                    + "+v" + parameters.get("v").toLowerCase().replace('.', '+'));
        }
    }

}
