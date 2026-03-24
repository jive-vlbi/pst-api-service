package org.orph2020.pst.apiimpl;

import org.ivoa.dm.proposal.management.*;
import org.ivoa.dm.proposal.prop.*;
import org.orph2020.pst.apiimpl.rest.SubjectMapResource;

import org.eclipse.microprofile.jwt.JsonWebToken;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.Set;
import java.util.List;

public class CurrentUserChecks {
    public static boolean currentUserHasRoleOnProposal(JsonWebToken userInfo, SubjectMapResource subjectMapResource, AbstractProposal proposal, Set<InvestigatorKind> anyOf) {
        Person currentUser = subjectMapResource.subjectMap(userInfo.getSubject()).getPerson();
        List<Investigator> investigators = proposal.getInvestigators();
        return investigators.stream().anyMatch(investigator -> investigator.getPerson() == currentUser && anyOf.contains(investigator.getType()));
    }

    public static void assertCurrentUserIsInvestigator(JsonWebToken userInfo, SubjectMapResource subjectMapResource, AbstractProposal proposal) throws WebApplicationException {
        if (!currentUserHasRoleOnProposal(userInfo, subjectMapResource, proposal, Set.of(InvestigatorKind.PI, InvestigatorKind.COI))) {
            throw new WebApplicationException("You are not an investigator on this proposal", Response.Status.FORBIDDEN);
        }
    }

    public static void assertCurrentUserIsPI(JsonWebToken userInfo, SubjectMapResource subjectMapResource, AbstractProposal proposal) throws WebApplicationException {
        if (!currentUserHasRoleOnProposal(userInfo, subjectMapResource, proposal, Set.of(InvestigatorKind.PI))) {
            throw new WebApplicationException("You are not a PI on this proposal", Response.Status.FORBIDDEN);
        }
    }

    public static void assertCurrentUserIsReviewer(JsonWebToken userInfo, SubjectMapResource subjectMapResource, ProposalCycle cycle) throws WebApplicationException {
        Person currentUser = subjectMapResource.subjectMap(userInfo.getSubject()).getPerson();
        boolean isReviewer = cycle.getTac().getMembers().stream().anyMatch(member -> member.getMember().getPerson() == currentUser);
        if (!isReviewer) {
            throw new WebApplicationException("You are not a reviewer on this proposal", Response.Status.FORBIDDEN);
        }
    }
    
}
