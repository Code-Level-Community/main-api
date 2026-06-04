package com.codelevel.module.community.http.rest.routes;

import com.codelevel.module.community.http.rest.dto.AnswerCreateRequest;
import com.codelevel.module.community.http.rest.dto.AnswerResponse;
import com.codelevel.module.community.http.rest.dto.CommunityVoteRequest;
import com.codelevel.module.community.persistence.entity.AnswerEntity;
import com.codelevel.module.community.persistence.entity.enums.VoteType;
import com.codelevel.module.community.persistence.resource.AnswerService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

@Path("/community/answers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AnswerResource {

    private final AnswerService answerService;
    private final JsonWebToken jwt;

    @Inject
    public AnswerResource(AnswerService answerService, JsonWebToken jwt) {
        this.answerService = answerService;
        this.jwt = jwt;
    }

    @POST
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response create(AnswerCreateRequest request) {
        AnswerEntity entity = answerService.create(request, extractUserId());
        return Response.status(Response.Status.CREATED).entity(toResponse(entity)).build();
    }

    @GET
    @Path("/by-question/{questionId}")
    @PermitAll
    public Response listByQuestion(@PathParam("questionId") Long questionId) {
        List<AnswerResponse> list = answerService.listByQuestion(questionId)
                .stream().map(this::toResponse).toList();
        return Response.ok(list).build();
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public Response getById(@PathParam("id") Long id) {
        AnswerEntity entity = answerService.getById(id);
        return Response.ok(toResponse(entity)).build();
    }

    @POST
    @Path("/{id}/vote")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response vote(@PathParam("id") Long id, CommunityVoteRequest request) {
        VoteType voteType = VoteType.valueOf(request.voteType());
        AnswerEntity entity = answerService.vote(id, extractUserId(), voteType);
        return Response.ok(toResponse(entity)).build();
    }

    @POST
    @Path("/{id}/accept")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response accept(@PathParam("id") Long id) {
        AnswerEntity entity = answerService.accept(id, extractUserId());
        return Response.ok(toResponse(entity)).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response delete(@PathParam("id") Long id) {
        answerService.delete(id, extractUserId(), isAdmin());
        return Response.noContent().build();
    }

    private UUID extractUserId() {
        return UUID.fromString(jwt.getSubject());
    }

    private boolean isAdmin() {
        return jwt.getGroups().contains("ROLE_ADMIN");
    }

    private AnswerResponse toResponse(AnswerEntity e) {
        return new AnswerResponse(
                e.getId(),
                e.getQuestionId(),
                e.getUserId(),
                e.getContent(),
                e.getUpVotes(),
                e.getDownVotes(),
                e.isAccepted(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
