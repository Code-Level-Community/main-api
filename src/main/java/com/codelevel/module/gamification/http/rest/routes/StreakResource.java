package com.codelevel.module.gamification.http.rest.routes;

import com.codelevel.module.gamification.http.rest.dto.StreakResponse;
import com.codelevel.module.gamification.http.rest.dto.UserAchievementResponse;
import com.codelevel.module.gamification.persistence.entity.UserStreakEntity;
import com.codelevel.module.gamification.persistence.entity.enums.SourceXPTransaction;
import com.codelevel.module.gamification.persistence.entity.enums.TriggerAchievement;
import com.codelevel.module.gamification.persistence.resource.AchievementService;
import com.codelevel.module.gamification.persistence.resource.StreakService;
import com.codelevel.module.gamification.persistence.resource.XpService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Path("/gamification/streaks")
@Produces(MediaType.APPLICATION_JSON)
public class StreakResource {

    private final StreakService streakService;
    private final AchievementService achievementService;
    private final XpService xpService;
    private final org.eclipse.microprofile.jwt.JsonWebToken jwt;

    @Inject
    public StreakResource(StreakService streakService, AchievementService achievementService,
                          XpService xpService, org.eclipse.microprofile.jwt.JsonWebToken jwt) {
        this.streakService = streakService;
        this.achievementService = achievementService;
        this.xpService = xpService;
        this.jwt = jwt;
    }

    @POST
    @Path("/activity")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response recordActivity() {
        UUID userId = extractUserId();
        UserStreakEntity streak = streakService.recordActivity(userId);

        achievementService.checkAndUnlock(userId, TriggerAchievement.STREAK_DAYS, streak.getCurrentStreakDays());

        return Response.ok(toResponse(streak)).build();
    }

    @POST
    @Path("/freeze")
    @Transactional
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response freeze() {
        UUID userId = extractUserId();

        // Validate streak exists before spending XP to avoid inconsistent state
        streakService.getStreak(userId).orElseThrow(
                () -> new com.codelevel.shared.exception.ResourceNotFound(
                        "Streak not found. Record an activity before activating the freeze."));

        long sourceId = LocalDate.now().toEpochDay();
        xpService.spend(userId, 100L, SourceXPTransaction.STREAK_FREEZE_COST, sourceId,
                "Streak freeze activated on " + LocalDate.now());

        UserStreakEntity streak = streakService.activateFreeze(userId);
        return Response.ok(toResponse(streak)).build();
    }

    @GET
    @Path("/my")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response getMyStreak() {
        UUID userId = extractUserId();
        return streakService.getStreak(userId)
                .map(s -> Response.ok(toResponse(s)).build())
                .orElse(Response.noContent().build());
    }

    @GET
    @Path("/users/{userId}")
    @PermitAll
    public Response getUserStreak(@PathParam("userId") UUID userId) {
        return streakService.getStreak(userId)
                .map(s -> Response.ok(toResponse(s)).build())
                .orElse(Response.noContent().build());
    }

    private UUID extractUserId() {
        return UUID.fromString(jwt.getSubject());
    }

    private StreakResponse toResponse(UserStreakEntity e) {
        boolean activeToday = e.getLastActivityDate() != null && e.getLastActivityDate().equals(LocalDate.now());
        boolean freezeActive = e.getFreezeUsedAt() != null && !e.getFreezeUsedAt().isBefore(LocalDate.now());
        return new StreakResponse(
                e.getUserId(), e.getCurrentStreakDays(), e.getLongestStreakDays(),
                e.getLastActivityDate(), activeToday, freezeActive
        );
    }
}
