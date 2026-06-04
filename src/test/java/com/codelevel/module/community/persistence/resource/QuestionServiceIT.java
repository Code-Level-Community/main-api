package com.codelevel.module.community.persistence.resource;

import com.codelevel.module.community.http.rest.dto.QuestionCreateRequest;
import com.codelevel.module.community.persistence.entity.QuestionEntity;
import com.codelevel.module.community.persistence.entity.enums.VoteType;
import com.codelevel.shared.exception.BusinessRuleException;
import com.codelevel.shared.exception.ResourceAlreadyExists;
import com.codelevel.shared.exception.ResourceNotFound;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class QuestionServiceIT {

    @Inject
    QuestionService questionService;

    private static UUID uid(long n) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", n));
    }

    private QuestionCreateRequest validRequest() {
        return new QuestionCreateRequest(
                1L,
                "Como configurar o Spring Boot com Docker?",
                "Estou tentando configurar minha aplicação Spring Boot para rodar em um container Docker, mas não consigo conectar ao banco de dados."
        );
    }

    @Test
    @TestTransaction
    void shouldCreateQuestionWithValidData() {
        QuestionEntity entity = questionService.create(validRequest(), uid(1));

        assertNotNull(entity.getId());
        assertEquals("Como configurar o Spring Boot com Docker?", entity.getTitle());
        assertEquals(uid(1), entity.getUserId());
        assertEquals(0L, entity.getUpVotes());
        assertEquals(0L, entity.getDownVotes());
        assertEquals(0L, entity.getAnswersCount());
        assertFalse(entity.isHasAcceptedAnswer());
        assertNotNull(entity.getCreatedAt());
    }

    @Test
    @TestTransaction
    void shouldThrowWhenTitleIsBlank() {
        var request = new QuestionCreateRequest(1L, "", "Conteúdo de pergunta com mais de vinte caracteres.");
        assertThrows(BusinessRuleException.class, () -> questionService.create(request, uid(1)));
    }

    @Test
    @TestTransaction
    void shouldThrowWhenTitleIsTooShort() {
        var request = new QuestionCreateRequest(1L, "Abc", "Conteúdo de pergunta com mais de vinte caracteres.");
        assertThrows(BusinessRuleException.class, () -> questionService.create(request, uid(1)));
    }

    @Test
    @TestTransaction
    void shouldThrowWhenContentIsTooShort() {
        var request = new QuestionCreateRequest(1L, "Pergunta válida aqui?", "Curta demais");
        assertThrows(BusinessRuleException.class, () -> questionService.create(request, uid(1)));
    }

    @Test
    @TestTransaction
    void shouldUpvoteQuestion() {
        QuestionEntity entity = questionService.create(validRequest(), uid(1));

        QuestionEntity voted = questionService.vote(entity.getId(), uid(2), VoteType.UPVOTE);

        assertEquals(1L, voted.getUpVotes());
        assertEquals(0L, voted.getDownVotes());
    }

    @Test
    @TestTransaction
    void shouldDownvoteQuestion() {
        QuestionEntity entity = questionService.create(validRequest(), uid(1));

        QuestionEntity voted = questionService.vote(entity.getId(), uid(2), VoteType.DOWNVOTE);

        assertEquals(0L, voted.getUpVotes());
        assertEquals(1L, voted.getDownVotes());
    }

    @Test
    @TestTransaction
    void shouldThrowWhenVotingTwice() {
        QuestionEntity entity = questionService.create(validRequest(), uid(1));
        questionService.vote(entity.getId(), uid(2), VoteType.UPVOTE);

        assertThrows(ResourceAlreadyExists.class,
                () -> questionService.vote(entity.getId(), uid(2), VoteType.DOWNVOTE));
    }

    @Test
    @TestTransaction
    void shouldListByCourse() {
        questionService.create(validRequest(), uid(1));
        questionService.create(new QuestionCreateRequest(1L, "Outra pergunta sobre Docker?", "Descrição com mais de vinte caracteres sobre Docker."), uid(2));
        questionService.create(new QuestionCreateRequest(2L, "Pergunta de outro curso?", "Descrição com mais de vinte caracteres neste curso."), uid(3));

        List<QuestionEntity> course1Questions = questionService.listByCourse(1L);

        assertTrue(course1Questions.size() >= 2);
        assertTrue(course1Questions.stream().allMatch(q -> q.getCourseId().equals(1L)));
    }

    @Test
    @TestTransaction
    void shouldDeleteByOwner() {
        QuestionEntity entity = questionService.create(validRequest(), uid(1));
        Long id = entity.getId();

        questionService.delete(id, uid(1), false);

        assertThrows(ResourceNotFound.class, () -> questionService.getById(id));
    }

    @Test
    @TestTransaction
    void shouldDeleteByAdmin() {
        QuestionEntity entity = questionService.create(validRequest(), uid(1));
        Long id = entity.getId();

        questionService.delete(id, uid(999), true);

        assertThrows(ResourceNotFound.class, () -> questionService.getById(id));
    }

    @Test
    @TestTransaction
    void shouldThrowWhenDeletingOtherUsersQuestion() {
        QuestionEntity entity = questionService.create(validRequest(), uid(1));

        assertThrows(BusinessRuleException.class,
                () -> questionService.delete(entity.getId(), uid(999), false));
    }

    @Test
    @TestTransaction
    void shouldThrowResourceNotFoundForNonexistentQuestion() {
        assertThrows(ResourceNotFound.class, () -> questionService.getById(999999L));
    }
}
