package org.orph2020.pst.apiimpl;

import org.ivoa.dm.proposal.prop.AbstractProposal;
import org.ivoa.dm.proposal.prop.Investigator;
import org.ivoa.dm.proposal.prop.InvestigatorKind;
import org.ivoa.dm.proposal.prop.Person;
import org.ivoa.dm.proposal.management.ProposalCycle;
import org.ivoa.dm.proposal.management.TacRole;
import org.ivoa.dm.proposal.management.CommitteeMember;
import org.orph2020.pst.apiimpl.entities.SubjectMap;
import org.orph2020.pst.apiimpl.rest.SubjectMapResource;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Set;
import java.util.List;

/**
 * Checks if the current user has a specific role on a proposal or cycle.
 * Throw a WebApplicationException with the Forbidden status if the user does not have the required role.
 * If the user has the obs_administrator role, the check always passes.
 */
@ApplicationScoped
public class CurrentUserChecks {
    @Inject
    SubjectMapResource subjectMapResource;

    @Inject
    JsonWebToken userInfo;

    @Inject
    SecurityContext securityContext;

    private boolean currentUserHasRoleOnProposal(AbstractProposal proposal, Set<InvestigatorKind> anyOf) {
        Person currentUser = subjectMapResource.subjectMap(userInfo.getSubject()).getPerson();
        List<Investigator> investigators = proposal.getInvestigators();
        return investigators.stream().anyMatch(investigator -> investigator.getPerson() == currentUser && anyOf.contains(investigator.getType()));
    }

    private boolean currentUserHasRoleOnCycle(ProposalCycle cycle, Set<TacRole> anyOf) {
        Person currentUser = subjectMapResource.subjectMap(userInfo.getSubject()).getPerson();
        List<CommitteeMember> members = cycle.getTac().getMembers();
        return members.stream().anyMatch(member -> member.getMember().getPerson() == currentUser && anyOf.contains(member.getRole()));

    }

    private AbstractProposal findProposal(Long proposalId) {
        return subjectMapResource.findObject(AbstractProposal.class, proposalId);
    }

    private ProposalCycle findCycle(Long cycleId) {
        return subjectMapResource.findObject(ProposalCycle.class, cycleId);
    }

    public void assertCurrentUserIsInvestigator(Long proposalId) throws WebApplicationException {
        if (securityContext.isUserInRole("obs_administration")) {
            return;
        }
        AbstractProposal proposal = findProposal(proposalId);
        if (!currentUserHasRoleOnProposal(proposal, Set.of(InvestigatorKind.PI, InvestigatorKind.COI))) {
            throw new WebApplicationException("You are not an investigator on this proposal", Response.Status.FORBIDDEN);
        }
    }

    public void assertCurrentUserIsPi(Long proposalId) throws WebApplicationException {
        if (securityContext.isUserInRole("obs_administration")) {
            return;
        }
        AbstractProposal proposal = findProposal(proposalId);

        // the contact author will have all the privileges of the PI
        Person currentUser = subjectMapResource.subjectMap(userInfo.getSubject()).getPerson();
        List<Investigator> investigators = proposal.getInvestigators();
        if (investigators.stream().anyMatch(investigator -> investigator.getPerson() == currentUser && investigator.getIsContactAuthor())) {
            return;
        }

        if (!currentUserHasRoleOnProposal(proposal, Set.of(InvestigatorKind.PI))) {
            throw new WebApplicationException("You are not the PI on this proposal", Response.Status.FORBIDDEN);
        }
    }

    public void assertCurrentUserIsPerson(Long personId) throws WebApplicationException {
        if (securityContext.isUserInRole("obs_administration")) {
            return;
        }
        SubjectMap subjectMap = subjectMapResource.findSubjectMap(personId);
        if (subjectMap == null || !subjectMap.uid.equals(userInfo.getSubject())) {
            throw new WebApplicationException("You are not this person", Response.Status.FORBIDDEN);
        }
    }

    public void assertCurrentUserHasKeycloakUid(String uid) throws WebApplicationException {
        if (securityContext.isUserInRole("obs_administration")) {
            return;
        }
        if (!uid.equals(userInfo.getSubject())) {
            throw new WebApplicationException("You are not this user", Response.Status.FORBIDDEN);
        }
    }

    public void assertCurrentUserIsTacMember(Long cycleId) throws WebApplicationException {
        if (securityContext.isUserInRole("obs_administration")) {
            return;
        }
        ProposalCycle cycle = findCycle(cycleId);
        if (!currentUserHasRoleOnCycle(cycle, Set.of(TacRole.CHAIR, TacRole.SCIENCEREVIEWER, TacRole.TECHNICALREVIEWER))) {
            throw new WebApplicationException("You are not a reviewer on this proposal", Response.Status.FORBIDDEN);
        }
    }

    public void assertCurrentUserIsTacChair(Long cycleId) throws WebApplicationException {
        if (securityContext.isUserInRole("obs_administration")) {
            return;
        }
        ProposalCycle cycle = findCycle(cycleId);
        if (!currentUserHasRoleOnCycle(cycle, Set.of(TacRole.CHAIR))) {
            throw new WebApplicationException("You are not a TAC chair on this cycle", Response.Status.FORBIDDEN);
        }
    }

}
