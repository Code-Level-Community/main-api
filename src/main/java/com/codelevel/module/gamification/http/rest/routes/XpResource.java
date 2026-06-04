package com.codelevel.module.gamification.http.rest.routes;

import com.codelevel.module.gamification.http.rest.dto.*;
import com.codelevel.module.gamification.persistence.entity.LevelEntity;
import com.codelevel.module.gamification.persistence.entity.XPTransactionEntity;
import com.codelevel.module.gamification.persistence.entity.enums.SourceXPTransaction;
import com.codelevel.module.gamification.persistence.resource.LevelService;
import com.codelevel.module.gamification.persistence.resource.XpService;
import com.codelevel.module.gamification.persistence.resource.dto.AwardXpResult;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Path("/gamification/xp")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class XpResource {

    private final XpService xpService;
    private final LevelService levelService;

    @Inject
    public XpResource(XpService xpService, LevelService levelService) {
        this.xpService = xpService;
        this.levelService = levelService;
    }

    @POST
    @Path("/award")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response award(AwardXpRequest request) {
        AwardXpResult result = xpService.award(
                request.userId(),
                request.amount(),
                SourceXPTransaction.valueOf(request.source()),
                request.sourceId(),
                request.description()
        );
        return Response.status(Response.Status.CREATED).entity(toAwardResponse(result)).build();
    }

    @GET
    @Path("/users/{userId}")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response getXpSummary(@PathParam("userId") UUID userId) {
        long totalXp = xpService.getTotalXp(userId);
        Optional<LevelEntity> level = xpService.getCurrentLevel(userId);
        return Response.ok(new XpSummaryResponse(userId, totalXp, level.map(this::toLevelResponse).orElse(null))).build();
    }

    @GET
    @Path("/users/{userId}/transactions")
    @RolesAllowed({"ROLE_USER", "ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    public Response listTransactions(@PathParam("userId") UUID userId) {
        List<XpTransactionResponse> list = xpService.listTransactions(userId)
                .stream().map(this::toTransactionResponse).toList();
        return Response.ok(list).build();
    }

    @GET
    @Path("/leaderboard")
    @PermitAll
    public Response leaderboard(@QueryParam("limit") @DefaultValue("10") int limit) {
        List<LeaderboardEntryResponse> entries = xpService.getLeaderboardRaw(limit)
                .stream()
                .map(row -> {
                    UUID userId = (UUID) row[0];
                    long totalXp = ((Number) row[1]).longValue();
                    Optional<LevelEntity> level = levelService.getLevelForXp(totalXp);
                    return new LeaderboardEntryResponse(userId, totalXp, level.map(this::toLevelResponse).orElse(null));
                })
                .toList();
        return Response.ok(entries).build();
    }

    private AwardXpResponse toAwardResponse(AwardXpResult result) {
        XPTransactionEntity t = result.transaction();
        return new AwardXpResponse(
                t.getId(), t.getUserId(), t.getXpAmount(), t.getSource().name(),
                t.getDescription(), t.getCreatedAt(),
                result.currentLevel().map(this::toLevelResponse).orElse(null),
                result.leveledUp()
        );
    }

    private XpTransactionResponse toTransactionResponse(XPTransactionEntity e) {
        return new XpTransactionResponse(
                e.getId(), e.getUserId(), e.getXpAmount(),
                e.getSource().name(), e.getDescription(), e.getCreatedAt()
        );
    }

    private LevelResponse toLevelResponse(LevelEntity e) {
        return new LevelResponse(e.getId(), e.getName(), e.getXpRequired(), e.getBadgeIconUrl());
    }
}
