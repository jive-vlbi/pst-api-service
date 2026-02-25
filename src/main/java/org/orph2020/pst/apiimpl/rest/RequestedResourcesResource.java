package org.orph2020.pst.apiimpl.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.Observation;
import org.ivoa.dm.proposal.prop.ObservingProposal;
import org.ivoa.dm.proposal.prop.RequestedResources;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.orph2020.pst.common.json.ObjectIdentifier;

import java.util.List;

@Path("proposals/{proposalCode}/resources")
@Tag(name = "proposals-resources")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("default-roles-orppst")
public class RequestedResourcesResource extends ObjectResourceBase {

    @GET
    @Operation(summary = "get the list of RequestedResources associated with the given ObservingProposal")
    public List<ObjectIdentifier> getRequestedResources(@PathParam("proposalCode") Long proposalCode)
    {
        return getObjectIdentifiers("SELECT r._id,coalesce(r.name,cast(r._id as string)) FROM ObservingProposal o Inner Join o.requestedResources r WHERE o._id = "+proposalCode);
    }

    @GET
    @Path("{requestedResourcesId}")
    @Operation(summary = "get a specific RequestedResources for the given ObservingProposal")
    public RequestedResources getRequestedResource(@PathParam("proposalCode") Long proposalCode,
                                                 @PathParam("requestedResourcesId") Long requestedResourcesId)
    {
        return findChildByQuery(ObservingProposal.class, RequestedResources.class, "requestedResources",
                proposalCode, requestedResourcesId);
    }

    @POST
    @Operation(summary = "add a new requested resource to the given ObservingProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @Transactional
    public RequestedResources addRequestedResource(@PathParam("proposalCode") Long proposalCode,
                                                  RequestedResources requestedResource)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);

        return addNewChildObject(observingProposal, new RequestedResources(requestedResource),
                observingProposal::addToRequestedResources);
    }

    @DELETE
    @Path("{requestedResourcesId}")
    @Operation(summary = "remove the RequestedResources specified by 'requestedResourcesId' from the given ObservingProposal")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeRequestedResource(@PathParam("proposalCode") Long proposalCode,
                                          @PathParam("requestedResourcesId") Long requestedResourcesId)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);

        RequestedResources requestedResource = observingProposal
                .getRequestedResources()
                .stream()
                .filter(o -> requestedResourcesId.equals(o.getId())).findAny()
                .orElseThrow(() -> new WebApplicationException(
                        String.format(NON_ASSOCIATE_ID, "Requested Resource", requestedResourcesId,
                                "ObservingProposal", proposalCode)
                ));

        return deleteChildObject(observingProposal, requestedResource, observingProposal::removeFromRequestedResources);
    }

    @PUT
    @Path("{requestedResourcesId}/name")
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(summary = "update a RequestedResources' name")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateRequestedResourceName(@PathParam("proposalCode") Long proposalCode,
                                              @PathParam("requestedResourcesId") Long requestedResourcesId,
                                              String replacementName)
            throws WebApplicationException
    {
        RequestedResources requestedResource = findChildByQuery(ObservingProposal.class, RequestedResources.class,
                "requestedResources", proposalCode, requestedResourcesId);

        requestedResource.setName(replacementName);

        return responseWrapper(requestedResource, 201);
    }

    @PUT
    @Path("{requestedResourcesId}/planObsConfig")
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(summary = "update a RequestedResources' planObsConfig")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateRequestedResourcePlanObsConfig(@PathParam("proposalCode") Long proposalCode,
                                                        @PathParam("requestedResourcesId") Long requestedResourcesId,
                                                        String replacementPlanObsConfig)
            throws WebApplicationException
    {
        RequestedResources requestedResource = findChildByQuery(ObservingProposal.class, RequestedResources.class,
                "requestedResources", proposalCode, requestedResourcesId);

        requestedResource.setPlanObsConfig(replacementPlanObsConfig);

        return responseWrapper(requestedResource, 201);
    }

    @PUT
    @Path("{requestedResourcesId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "update the entire RequestedResources specified by the 'requestedResourcesId'")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateRequestedResource(@PathParam("proposalCode") Long proposalCode,
                                           @PathParam("requestedResourcesId") Long requestedResourcesId,
                                           RequestedResources replacementRequestedResource)
            throws WebApplicationException
    {
        RequestedResources requestedResource = findChildByQuery(ObservingProposal.class, RequestedResources.class,
                "requestedResources", proposalCode, requestedResourcesId);

        requestedResource.updateUsing(replacementRequestedResource);

        return responseWrapper(requestedResource, 201);
    }
}
