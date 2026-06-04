package com.codelevel.module.community.persistence.resource;

import com.codelevel.module.community.http.rest.dto.AnswerCreateRequest;
import com.codelevel.module.community.http.rest.dto.QuestionCreateRequest;
import com.codelevel.module.community.persistence.entity.AnswerEntity;
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
class AnswerServiceIT {

    @Inject
    QuestionService questionService;

    @Inject
    AnswerService answerService;

    private static UUID uid(long n) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", n));
    }

    private QuestionEntity createQuestion(UUID userId) {
        return questionService.create(new QuestionCreateRequest(
                1L,
                "Como configurar o Spring Boot com Docker?",
                "Estou tentando configurar minha aplicação Spring Boot para rodar em container Docker."
        ), userId);
    }

    @Test
    @TestTransaction
    void shouldCreateAnswerWithValidData() {
        QuestionEntity question = createQuestion(uid(1));

        AnswerEntity answer = answerService.create(
                new AnswerCreateRequest(question.getId(), "Você precisa configurar o host do banco de dados para o nome do serviço Docker."),
                uid(2)
        );

        assertNotNull(answer.getId());
        assertEquals(question.getId(), answer.getQuestionId());
        assertEquals(uid(2), answer.getUserId());
        assertEquals(0L, answer.getUpVotes());
        assertFalse(answer.isAccepted());
        assertNotNull(answer.getCreatedAt());
    }

    @Test
    @TestTransaction
    void shouldIncrementAnswersCountOnCreate() {
        QuestionEntity question = createQuestion(uid(1));

        answerService.create(
                new AnswerCreateRequest(question.getId(), "Você precisa configurar o host do banco de dados."),
                uid(2)
        );

        QuestionEntity updated = questionService.getById(question.getId());
        assertEquals(1L, updated.getAnswersCount());
    }

    @Test
    @TestTransaction
    void shouldThrowWhenContentIsTooShort() {
        QuestionEntity question = createQuestion(uid(1));

        assertThrows(BusinessRuleException.class,
                () -> answerService.create(new AnswerCreateRequest(question.getId(), "Curto"), uid(2)));
    }

    @Test
    @TestTransaction
    void shouldThrowWhenQuestionNotFound() {
        assertThrows(ResourceNotFound.class,
                () -> answerService.create(new AnswerCreateRequest(999999L, "Conteúdo da resposta válida aqui."), uid(1)));
    }

    @Test
    @TestTransaction
    void shouldListByQuestion() {
        QuestionEntity question = createQuestion(uid(1));
        answerService.create(new AnswerCreateRequest(question.getId(), "Primeira resposta com conteúdo suficiente."), uid(2));
        answerService.create(new AnswerCreateRequest(question.getId(), "Segunda resposta com conteúdo suficiente."), uid(3));

        List<AnswerEntity> answers = answerService.listByQuestion(question.getId());

        assertEquals(2, answers.size());
        assertTrue(answers.stream().allMatch(a -> a.getQuestionId().equals(question.getId())));
    }

    @Test
    @TestTransaction
    void shouldUpvoteAnswer() {
        QuestionEntity question = createQuestion(uid(1));
        AnswerEntity answer = answerService.create(
                new AnswerCreateRequest(question.getId(), "Resposta válida com conteúdo suficiente aqui."),
                uid(2)
        );

        AnswerEntity voted = answerService.vote(answer.getId(), uid(3), VoteType.UPVOTE);

        assertEquals(1L, voted.getUpVotes());
        assertEquals(0L, voted.getDownVotes());
    }

    @Test
    @TestTransaction
    void shouldThrowWhenVotingAnswerTwice() {
        QuestionEntity question = createQuestion(uid(1));
        AnswerEntity answer = answerService.create(
                new AnswerCreateRequest(question.getId(), "Resposta válida com conteúdo suficiente aqui."),
                uid(2)
        );

        answerService.vote(answer.getId(), uid(3), VoteType.UPVOTE);

        assertThrows(ResourceAlreadyExists.class,
                () -> answerService.vote(answer.getId(), uid(3), VoteType.UPVOTE));
    }

    @Test
    @TestTransaction
    void shouldAcceptAnswerByQuestionOwner() {
        QuestionEntity question = createQuestion(uid(1));
        AnswerEntity answer = answerService.create(
                new AnswerCreateRequest(question.getId(), "Resposta válida com conteúdo suficiente aqui."),
                uid(2)
        );

        AnswerEntity accepted = answerService.accept(answer.getId(), uid(1));

        assertTrue(accepted.isAccepted());

        QuestionEntity updatedQuestion = questionService.getById(question.getId());
        assertTrue(updatedQuestion.isHasAcceptedAnswer());
    }

    @Test
    @TestTransaction
    void shouldThrowWhenAcceptingAnswerByNonOwner() {
        QuestionEntity question = createQuestion(uid(1));
        AnswerEntity answer = answerService.create(
                new AnswerCreateRequest(question.getId(), "Resposta válida com conteúdo suficiente aqui."),
                uid(2)
        );

        assertThrows(BusinessRuleException.class,
                () -> answerService.accept(answer.getId(), uid(99)));
    }

    @Test
    @TestTransaction
    void shouldReplaceAcceptedAnswerWhenAcceptingNew() {
        QuestionEntity question = createQuestion(uid(1));
        AnswerEntity first = answerService.create(
                new AnswerCreateRequest(question.getId(), "Primeira resposta com conteúdo suficiente."),
                uid(2)
        );
        AnswerEntity second = answerService.create(
                new AnswerCreateRequest(question.getId(), "Segunda resposta com conteúdo suficiente."),
                uid(3)
        );

        answerService.accept(first.getId(), uid(1));
        answerService.accept(second.getId(), uid(1));

        AnswerEntity updatedFirst = answerService.getById(first.getId());
        AnswerEntity updatedSecond = answerService.getById(second.getId());

        assertFalse(updatedFirst.isAccepted());
        assertTrue(updatedSecond.isAccepted());
    }

    @Test
    @TestTransaction
    void shouldDeleteByOwner() {
        QuestionEntity question = createQuestion(uid(1));
        AnswerEntity answer = answerService.create(
                new AnswerCreateRequest(question.getId(), "Resposta válida com conteúdo suficiente aqui."),
                uid(2)
        );
        Long answerId = answer.getId();

        answerService.delete(answerId, uid(2), false);

        assertThrows(ResourceNotFound.class, () -> answerService.getById(answerId));
    }

    @Test
    @TestTransaction
    void shouldDecrementAnswersCountOnDelete() {
        QuestionEntity question = createQuestion(uid(1));
        AnswerEntity answer = answerService.create(
                new AnswerCreateRequest(question.getId(), "Resposta válida com conteúdo suficiente aqui."),
                uid(2)
        );

        answerService.delete(answer.getId(), uid(2), false);

        QuestionEntity updated = questionService.getById(question.getId());
        assertEquals(0L, updated.getAnswersCount());
    }

    @Test
    @TestTransaction
    void shouldThrowWhenDeletingOtherUsersAnswer() {
        QuestionEntity question = createQuestion(uid(1));
        AnswerEntity answer = answerService.create(
                new AnswerCreateRequest(question.getId(), "Resposta válida com conteúdo suficiente aqui."),
                uid(2)
        );

        assertThrows(BusinessRuleException.class,
                () -> answerService.delete(answer.getId(), uid(99), false));
    }
}
