package com.codelevel.module.community.persistence.resource;

import com.codelevel.module.community.domain.AnswerContent;
import com.codelevel.module.community.http.rest.dto.AnswerCreateRequest;
import com.codelevel.module.community.persistence.entity.AnswerEntity;
import com.codelevel.module.community.persistence.entity.QuestionEntity;
import com.codelevel.module.community.persistence.entity.VoteEntity;
import com.codelevel.module.community.persistence.entity.enums.VotableType;
import com.codelevel.module.community.persistence.entity.enums.VoteType;
import com.codelevel.module.identity.persistence.resource.CreateUpdate;
import com.codelevel.shared.exception.BusinessRuleException;
import com.codelevel.shared.exception.ResourceAlreadyExists;
import com.codelevel.shared.exception.ResourceNotFound;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AnswerService implements CreateUpdate {

    private final QuestionService questionService;

    @Inject
    public AnswerService(QuestionService questionService) {
        this.questionService = questionService;
    }

    @Transactional
    public AnswerEntity create(AnswerCreateRequest request, UUID userId) {
        var content = new AnswerContent(request.content());
        QuestionEntity question = questionService.getById(request.questionId());

        AnswerEntity entity = new AnswerEntity();
        entity.setQuestionId(question.getId());
        entity.setUserId(userId);
        entity.setContent(content.value());
        saveOrUpdate(entity);

        question.setAnswersCount(question.getAnswersCount() + 1);
        question.setUpdatedAt(LocalDateTime.now());
        saveOrUpdate(question);

        return entity;
    }

    public AnswerEntity getById(Long id) {
        return AnswerEntity.<AnswerEntity>findByIdOptional(id)
                .orElseThrow(() -> new ResourceNotFound("Answer not found"));
    }

    public List<AnswerEntity> listByQuestion(Long questionId) {
        questionService.getById(questionId);
        return AnswerEntity.findByQuestionId(questionId);
    }

    @Transactional
    public AnswerEntity vote(Long answerId, UUID userId, VoteType voteType) {
        AnswerEntity answer = getById(answerId);

        VoteEntity.findByUserAndVotable(userId, VotableType.ANSWER, answerId).ifPresent(v -> {
            throw new ResourceAlreadyExists("You have already voted on this answer");
        });

        VoteEntity vote = new VoteEntity();
        vote.setUserId(userId);
        vote.setVotableType(VotableType.ANSWER);
        vote.setVotableId(answerId);
        vote.setVoteType(voteType);
        saveOrUpdate(vote);

        if (voteType == VoteType.UPVOTE) {
            answer.setUpVotes(answer.getUpVotes() + 1);
        } else {
            answer.setDownVotes(answer.getDownVotes() + 1);
        }

        answer.setUpdatedAt(LocalDateTime.now());
        saveOrUpdate(answer);
        return answer;
    }

    @Transactional
    public AnswerEntity accept(Long answerId, UUID userId) {
        AnswerEntity answer = getById(answerId);
        QuestionEntity question = questionService.getById(answer.getQuestionId());

        if (!question.getUserId().equals(userId)) {
            throw new BusinessRuleException("Only the question author can accept an answer");
        }

        AnswerEntity.findAcceptedByQuestionId(question.getId()).ifPresent(previous -> {
            previous.setAccepted(false);
            saveOrUpdate(previous);
        });

        answer.setAccepted(true);
        answer.setUpdatedAt(LocalDateTime.now());
        saveOrUpdate(answer);

        question.setHasAcceptedAnswer(true);
        question.setUpdatedAt(LocalDateTime.now());
        saveOrUpdate(question);

        return answer;
    }

    @Transactional
    public void delete(Long id, UUID userId, boolean isAdmin) {
        AnswerEntity answer = getById(id);

        if (!isAdmin && !answer.getUserId().equals(userId)) {
            throw new BusinessRuleException("You do not have permission to delete this answer");
        }

        QuestionEntity question = questionService.getById(answer.getQuestionId());
        question.setAnswersCount(Math.max(0L, question.getAnswersCount() - 1));
        if (answer.isAccepted()) {
            question.setHasAcceptedAnswer(false);
        }
        question.setUpdatedAt(LocalDateTime.now());
        saveOrUpdate(question);

        answer.delete();
    }
}
