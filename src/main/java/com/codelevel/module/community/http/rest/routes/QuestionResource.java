package com.codelevel.module.community.http.rest.routes;

import com.codelevel.module.community.http.rest.dto.CommunityVoteRequest;
import com.codelevel.module.community.http.rest.dto.QuestionCreateRequest;
import com.codelevel.module.community.http.rest.dto.QuestionResponse;
import com.codelevel.module.community.persistence.entity.QuestionEntity;
import com.codelevel.module.community.persistence.entity.enums.VoteType;
import com.codelevel.module.community.persistence.resource.QuestionService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

@Path("/community/questions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class QuestionResource {

    private final QuestionService questionService;
    private final JsonWebToken jwt;

    @Inject
    public QuestionResource(QuestionService questionService, JsonWebToken jwt) {
        this.questionService = questionService;
        this.jwt = jwt;
    }

    @POST
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response create(QuestionCreateRequest request) {
        QuestionEntity entity = questionService.create(request, extractUserId());
        return Response.status(Response.Status.CREATED).entity(toResponse(entity)).build();
    }

    @GET
    @PermitAll
    public Response listAll(@QueryParam("courseId") Long courseId) {
        List<QuestionResponse> list;
        if (courseId != null) {
            list = questionService.listByCourse(courseId).stream().map(this::toResponse).toList();
        } else {
            list = questionService.listAll().stream().map(this::toResponse).toList();
        }
        return Response.ok(list).build();
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public Response getById(@PathParam("id") Long id) {
        QuestionEntity entity = questionService.getById(id);
        return Response.ok(toResponse(entity)).build();
    }

    @POST
    @Path("/{id}/vote")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response vote(@PathParam("id") Long id, CommunityVoteRequest request) {
        VoteType voteType = VoteType.valueOf(request.voteType());
        QuestionEntity entity = questionService.vote(id, extractUserId(), voteType);
        return Response.ok(toResponse(entity)).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response delete(@PathParam("id") Long id) {
        questionService.delete(id, extractUserId(), isAdmin());
        return Response.noContent().build();
    }

    private UUID extractUserId() {
        return UUID.fromString(jwt.getSubject());
    }

    private boolean isAdmin() {
        return jwt.getGroups().contains("ROLE_ADMIN");
    }

    private QuestionResponse toResponse(QuestionEntity e) {
        return new QuestionResponse(
                e.getId(),
                e.getUserId(),
                e.getCourseId(),
                e.getTitle(),
                e.getContent(),
                e.getUpVotes(),
                e.getDownVotes(),
                e.getAnswersCount(),
                e.isHasAcceptedAnswer(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
