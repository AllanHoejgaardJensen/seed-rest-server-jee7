package dk.sample.rest.bank.customer.exposure.rs;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import dk.nykredit.time.CurrentTime;
import dk.sample.rest.bank.customer.exposure.rs.model.EventsMetadataRepresentation;
import dk.sample.rest.bank.customer.persistence.CustomerArchivist;
import dk.sample.rest.common.core.logging.LogDuration;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

/**
 * REST exposure of metadata for events that are related to the customer.
 */
@Stateless
@PermitAll
@Path("/customer-events-metadata")
@DeclareRoles("tx-system")
@Api(value = "/customer-events-metadata",
    tags = {"metadata"})
public class CustomerEventFeedMetadataServiceExposure {
    private final Map<String, EventMetadataProducerMethod> eventMetadataProducers = new HashMap<>();

    @EJB
    private CustomerArchivist archivist;

    public CustomerEventFeedMetadataServiceExposure() {
        eventMetadataProducers.put("application/hal+json", this::getMetaDataSG1V1);
        eventMetadataProducers.put("application/hal+json;concept=metadata;v=1", this::getMetaDataSG1V1);
    }

    @GET
    @Produces({"application/hal+json", "application/hal+json;concept=metadata;v=1"})
    @ApiOperation(
        value = "metadata for the events endpoint", response = EventsMetadataRepresentation.class,
        authorizations = {
            @Authorization(value = "oauth2", scopes = {}),
            @Authorization(value = "oauth2-cc", scopes = {}),
            @Authorization(value = "oauth2-ac", scopes = {}),
            @Authorization(value = "oauth2-rop", scopes = {}),
            @Authorization(value = "Bearer")
        },
        notes = " the events are signalled by this resource as this this is the authoritative resource for all events that " +
            "subscribers to the customer service should be able to listen for and react to. In other words this is the authoritative" +
            "feed for the customer service",
        tags = {"events"},
        produces = "application/hal+json,  application/hal+json;concept=metadata;v=1",
        nickname = "getCustomerEventMetadata"
    )
    public Response getMetadata(@Context UriInfo uriInfo, @Context Request request,
                                @HeaderParam("Accept") String accept,
                                @HeaderParam("X-Log-Token") String xLogToken) {
        return eventMetadataProducers.getOrDefault(accept, this::handleUnsupportedContentType).getResponse(uriInfo, request, xLogToken);
    }

    @LogDuration(limit = 50)
    public Response getMetaDataSG1V1(UriInfo uriInfo, Request request, String xLogToken) {
        EventsMetadataRepresentation em = new EventsMetadataRepresentation("", uriInfo);
        CacheControl cc = new CacheControl();
        int maxAge = 4 * 7 * 24 * 60 * 60;
        cc.setMaxAge(maxAge);
        String logToken = (xLogToken != null && !"".equals(xLogToken.trim())) ? xLogToken : UUID.randomUUID().toString();
        return Response.ok()
            .entity(em)
            .cacheControl(cc).expires(Date.from(CurrentTime.now().plusSeconds(maxAge)))
            .type("application/hal+json;concept=metadata;v=1")
            .header("X-Log-Token", logToken)
            .header("X-RateLimit-Limit", "-1")
            .header("X-RateLimit-Limit-24h", "-1")
            .header("X-RateLimit-Remaining", "-1")
            .header("X-RateLimit-Reset", "-1")
            .build();
    }

    Response handleUnsupportedContentType(UriInfo uriInfo, Request request, String xLogToken) {
        return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build();
    }

    interface EventMetadataProducerMethod {
        Response getResponse(UriInfo uriInfo, Request request, String xLogToken);
    }


}
