package org.orph2020.pst.apiimpl.rest;

import jakarta.inject.Inject;
import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.*;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.orph2020.pst.common.json.ObjectIdentifier;
import org.orph2020.pst.apiimpl.CurrentUserChecks;

import java.util.List;

@Path("proposals/{proposalCode}/technicalGoals")
@Tag(name = "proposals-technicalGoals")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("default-roles-orppst")
public class TechnicalGoalResource extends ObjectResourceBase{

    @Inject
    CurrentUserChecks currentUserChecks;

    // technicalGoals
    //if we were following the design pattern we should return a list of TechnicalGoal identifiers
    // - problem is there is no natural name so cast id to string
    @GET
    @Operation(summary = "get the list of TechnicalGoals associated with the given ObservingProposal")
    public List<ObjectIdentifier> getTechnicalGoals(@PathParam("proposalCode") Long proposalCode)
    {
        currentUserChecks.assertCurrentUserIsInvestigator(proposalCode);
        return getObjectIdentifiers("SELECT t._id,cast(t._id as string) FROM ObservingProposal o Inner Join o.technicalGoals t WHERE o._id = "+proposalCode);
    }

    @GET
    @Path("{technicalGoalId}")
    @Operation(summary = "get a specific TechnicalGoal for the given ObservingProposal")
    public TechnicalGoal getTechnicalGoal(@PathParam("proposalCode") Long proposalCode,
                                          @PathParam("technicalGoalId") Long techGoalId)
    {
        currentUserChecks.assertCurrentUserIsInvestigator(proposalCode);
        return findChildByQuery(ObservingProposal.class, TechnicalGoal.class, "technicalGoals",
                proposalCode, techGoalId);
    }

    @POST
    @Operation(summary = "add a new technical goal to the given ObservingProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @Transactional
    public TechnicalGoal addTechnicalGoal(@PathParam("proposalCode") Long proposalCode,
                                             TechnicalGoal technicalGoal)
            throws WebApplicationException
    {
        currentUserChecks.assertCurrentUserIsInvestigator(proposalCode);
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);

        //use copy constructor in case the front-end is attempting to clone the technical goal,
        //for a completely new technical goal this is inefficient but livable.
        return addNewChildObject(observingProposal, new TechnicalGoal(technicalGoal),
                observingProposal::addToTechnicalGoals);
    }


    @DELETE
    @Path("{technicalGoalId}")
    @Operation(summary = "remove the Technical Goal specified by 'technicalGoalId' from the given ObservingProposal")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeTechnicalGoal(@PathParam("proposalCode") Long proposalCode,
                                        @PathParam("technicalGoalId") Long technicalGoalId)
            throws WebApplicationException
    {
        currentUserChecks.assertCurrentUserIsInvestigator(proposalCode);
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);

        //we've just found the ObservingProposal so may as well use it to find the TechnicalGoal
        //rather than doing a 'findChildByQuery()'
        TechnicalGoal technicalGoal = observingProposal
                .getTechnicalGoals()
                .stream()
                .filter(o -> technicalGoalId.equals(o.getId())).findAny()
                .orElseThrow(() -> new WebApplicationException(
                        String.format(NON_ASSOCIATE_ID, "Technical Goal", technicalGoalId,
                                "ObservingProposal", proposalCode)
                ));

        return deleteChildObject(observingProposal, technicalGoal, observingProposal::removeFromTechnicalGoals);
    }

    //TechnicalGoal::PerformanceParameters

    @PUT
    @Path("{technicalGoalId}/performanceParameters")
    @Operation(summary = "replace the PerformanceParameters of the TechnicalGoal referred to by the 'technicalGoalId")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public PerformanceParameters replacePerformanceParameters(@PathParam("proposalCode") Long proposalCode,
                                                      @PathParam("technicalGoalId") Long technicalGoalId,
                                                      PerformanceParameters replacementParameters)
        throws WebApplicationException
    {
        currentUserChecks.assertCurrentUserIsInvestigator(proposalCode);
        TechnicalGoal currentGoal = findChildByQuery(ObservingProposal.class, TechnicalGoal.class,
                "technicalGoals", proposalCode, technicalGoalId);

        currentGoal.setPerformance(replacementParameters);

        return currentGoal.getPerformance();
    }

    //TechnicalGoal::EVNSpectralLine(List<EVNSpectralLine>)

    @POST
    @Path("{technicalGoalId}/spectralLine")
    @Operation(summary = "add a new spectral line to the TechnicalGoal referred to by the 'technicalGoalId'")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public EVNSpectralLine addSpectralLine(@PathParam("proposalCode") Long proposalCode,
                                           @PathParam("technicalGoalId") Long technicalGoalId,
                                           EVNSpectralLine spectralLine)
        throws WebApplicationException
    {
        currentUserChecks.assertCurrentUserIsInvestigator(proposalCode);
        TechnicalGoal goal = findChildByQuery(ObservingProposal.class, TechnicalGoal.class,
                                              "technicalGoals", proposalCode, technicalGoalId);
        // apparently need to manually lock when adding or removing rows with an _ORDER column
        em.lock(goal, LockModeType.PESSIMISTIC_WRITE);

        return addNewChildObject(goal, spectralLine, goal::addToSpectralLine);
    }

    @DELETE
    @Path("{technicalGoalId}/spectralLine/{spectralLineId}/")
    @Operation(summary = "remove the SpectralLine with 'spectralLineId' from the given TechnicalGoal")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeSpectralLine(@PathParam("proposalCode") Long proposalCode,
                                       @PathParam("technicalGoalId") Long technicalGoalId,
                                       @PathParam("spectralLineId") Long spectralLineId)
            throws WebApplicationException
    {
        currentUserChecks.assertCurrentUserIsInvestigator(proposalCode);
        TechnicalGoal goal = findChildByQuery(ObservingProposal.class, TechnicalGoal.class,
                "technicalGoals", proposalCode, technicalGoalId);
        // apparently need to manually lock when adding or removing rows with an _ORDER column
        em.lock(goal, LockModeType.PESSIMISTIC_WRITE);

        EVNSpectralLine spectralLine =
                findChildByQuery(TechnicalGoal.class, EVNSpectralLine.class,
                        "spectralLine", technicalGoalId, spectralLineId);

        return deleteChildObject(goal, spectralLine, goal::removeFromSpectralLine);
    }

    @PUT
    @Path("{technicalGoalId}/spectalLine/{spectralLineId}/")
    @Operation(summary = "replace the EVNSpectralLine with 'spectralLineId' in the given TechnicalGoal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public EVNSpectralLine replaceSpectralLine(
            @PathParam("proposalCode") Long proposalCode,
            @PathParam("technicalGoalId") Long technicalGoalId,
            @PathParam("spectralLineId") Long spectralLineId,
            EVNSpectralLine replacementLine
    )
            throws WebApplicationException
    {
        currentUserChecks.assertCurrentUserIsInvestigator(proposalCode);
        TechnicalGoal goal = findChildByQuery(ObservingProposal.class, TechnicalGoal.class,
                "technicalGoals", proposalCode, technicalGoalId);

        EVNSpectralLine spectralLine =
                findChildByQuery(TechnicalGoal.class, EVNSpectralLine.class,
                                 "spectralLine", technicalGoalId, spectralLineId);

        spectralLine.updateUsing(replacementLine);

        return spectralLine;
    }

    @PUT
    @Path("{technicalGoalId}/correlatorParameters/")
    @Operation(summary = "replace the CorrelatorParameters in the given TechnicalGoal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public CorrelatorParameters replaceCorrelatorParameters(
            @PathParam("proposalCode") Long proposalCode,
            @PathParam("technicalGoalId") Long technicalGoalId,
            CorrelatorParameters replacementParameters
    )
            throws WebApplicationException
    {
        currentUserChecks.assertCurrentUserIsInvestigator(proposalCode);
        TechnicalGoal goal = findChildByQuery(ObservingProposal.class, TechnicalGoal.class,
                "technicalGoals", proposalCode, technicalGoalId);

        CorrelatorParameters existingParameters = goal.getCorrelatorParameters();
        if (existingParameters != null) {
            // explicitly remove old pulsar gates so they don't become orphaned rows
            // the generated @OneToMany composition with CascadeType.ALL but no orphanRemoval = true
            // means they won't be deleted automatically
            for (PulsarGate gate : existingParameters.getPulsarGates()) {
                em.remove(gate);
            }
            existingParameters.updateUsing(replacementParameters);
        } else {
            goal.setCorrelatorParameters(replacementParameters);
        }

        return goal.getCorrelatorParameters();
    }

}
