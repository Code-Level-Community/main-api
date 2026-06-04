package com.codelevel.module.community.persistence.resource;

import com.codelevel.module.community.domain.QuestionContent;
import com.codelevel.module.community.domain.QuestionTitle;
import com.codelevel.module.community.http.rest.dto.QuestionCreateRequest;
import com.codelevel.module.community.persistence.entity.QuestionEntity;
import com.codelevel.module.community.persistence.entity.VoteEntity;
import com.codelevel.module.community.persistence.entity.enums.VotableType;
import com.codelevel.module.community.persistence.entity.enums.VoteType;
import com.codelevel.module.identity.persistence.resource.CreateUpdate;
import com.codelevel.shared.exception.BusinessRuleException;
import com.codelevel.shared.exception.ResourceAlreadyExists;
import com.codelevel.shared.exception.ResourceNotFound;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class QuestionService implements CreateUpdate {

    @Transactional
    public QuestionEntity create(QuestionCreateRequest request, UUID userId) {
        var title = new QuestionTitle(request.title());
        var content = new QuestionContent(request.content());

        QuestionEntity entity = new QuestionEntity();
        entity.setUserId(userId);
        entity.setCourseId(request.courseId());
        entity.setTitle(title.value());
        entity.setContent(content.value());
        saveOrUpdate(entity);
        return entity;
    }

    public QuestionEntity getById(Long id) {
        return QuestionEntity.<QuestionEntity>findByIdOptional(id)
                .orElseThrow(() -> new ResourceNotFound("Question not found"));
    }

    public List<QuestionEntity> listAll() {
        return QuestionEntity.listAll();
    }

    public List<QuestionEntity> listByCourse(Long courseId) {
        return QuestionEntity.findByCourseId(courseId);
    }

    @Transactional
    public QuestionEntity vote(Long questionId, UUID userId, VoteType voteType) {
        QuestionEntity question = getById(questionId);

        VoteEntity.findByUserAndVotable(userId, VotableType.QUESTION, questionId).ifPresent(v -> {
            throw new ResourceAlreadyExists("You have already voted on this question");
        });

        VoteEntity vote = new VoteEntity();
        vote.setUserId(userId);
        vote.setVotableType(VotableType.QUESTION);
        vote.setVotableId(questionId);
        vote.setVoteType(voteType);
        saveOrUpdate(vote);

        if (voteType == VoteType.UPVOTE) {
            question.setUpVotes(question.getUpVotes() + 1);
        } else {
            question.setDownVotes(question.getDownVotes() + 1);
        }

        question.setUpdatedAt(LocalDateTime.now());
        saveOrUpdate(question);
        return question;
    }

    @Transactional
    public void delete(Long id, UUID userId, boolean isAdmin) {
        QuestionEntity question = getById(id);

        if (!isAdmin && !question.getUserId().equals(userId)) {
            throw new BusinessRuleException("You do not have permission to delete this question");
        }

        question.delete();
    }
}
