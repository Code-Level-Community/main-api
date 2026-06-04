package com.codelevel.module.gamification.http.rest.routes;

import com.codelevel.module.gamification.http.rest.dto.AchievementResponse;
import com.codelevel.module.gamification.http.rest.dto.CheckAchievementsRequest;
import com.codelevel.module.gamification.http.rest.dto.CreateAchievementRequest;
import com.codelevel.module.gamification.http.rest.dto.UserAchievementResponse;
import com.codelevel.module.gamification.persistence.entity.AchievementEntity;
import com.codelevel.module.gamification.persistence.entity.UserAchievementEntity;
import com.codelevel.module.gamification.persistence.entity.enums.TriggerAchievement;
import com.codelevel.module.gamification.persistence.resource.AchievementService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/gamification/achievements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AchievementResource {

    private final AchievementService achievementService;

    @Inject
    public AchievementResource(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @POST
    @RolesAllowed("ROLE_ADMIN")
    public Response create(CreateAchievementRequest request) {
        AchievementEntity entity = achievementService.create(
                request.name(), request.slug(), request.description(), request.iconUrl(),
                TriggerAchievement.valueOf(request.triggerType()),
                request.triggerCriteria(), request.xpReward()
        );
        return Response.status(Response.Status.CREATED).entity(toResponse(entity)).build();
    }

    @GET
    @PermitAll
    public Response list() {
        List<AchievementResponse> list = achievementService.list().stream().map(this::toResponse).toList();
        return Response.ok(list).build();
    }

    @GET
    @Path("/users/{userId}")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response getUserAchievements(@PathParam("userId") UUID userId) {
        List<UserAchievementResponse> list = achievementService.getUserAchievements(userId)
                .stream().map(this::toUserResponse).toList();
        return Response.ok(list).build();
    }

    @POST
    @Path("/check")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response checkAndUnlock(CheckAchievementsRequest request) {
        List<UserAchievementResponse> unlocked = achievementService.checkAndUnlock(
                request.userId(),
                TriggerAchievement.valueOf(request.triggerType()),
                request.currentValue()
        ).stream().map(this::toUserResponse).toList();
        return Response.ok(unlocked).build();
    }

    private AchievementResponse toResponse(AchievementEntity e) {
        return new AchievementResponse(
                e.getId(), e.getName(), e.getSlug(), e.getDescription(),
                e.getIconUrl(), e.getTriggerType().name(), e.getTriggerCriteria(),
                e.getXpReward(), e.getCreatedAt()
        );
    }

    private UserAchievementResponse toUserResponse(UserAchievementEntity e) {
        return new UserAchievementResponse(e.getId(), e.getUserId(), e.getAchievementId(), e.getUnlockedAt());
    }
}
