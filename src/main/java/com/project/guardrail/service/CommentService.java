package com.project.guardrail.service;

import com.project.guardrail.dto.request.CreateCommentRequest;
import com.project.guardrail.dto.response.CommentResponse;
import com.project.guardrail.entity.Comment;
import com.project.guardrail.entity.Post;
import com.project.guardrail.entity.enums.AuthorType;
import com.project.guardrail.repository.CommentRepository;
import com.project.guardrail.repository.PostRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private static final int MAX_DEPTH = 20;

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    private final RedisGuardrailService redisGuardrailService;
    private final ViralityService viralityService;
    private final NotificationService notificationService;

    @Transactional
    public CommentResponse createComment(Long postId, CreateCommentRequest request) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() ->
                        new RuntimeException("Post not found"));

        Comment parentComment = null;

        int depthLevel = 1;

        if (request.getParentCommentId() != null) {

            parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() ->
                            new RuntimeException("Parent comment not found"));

            if (!parentComment.getPost().getId().equals(postId)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Parent comment does not belong to this post"
                );
            }

            depthLevel = parentComment.getDepthLevel() + 1;

            validateDepth(depthLevel);
        }

        handleBotGuardrails(postId, request);

        Comment comment = Comment.builder()
                .post(post)
                .authorId(request.getAuthorId())
                .authorType(request.getAuthorType())
                .content(request.getContent())
                .parentComment(parentComment)
                .depthLevel(depthLevel)
                .createdAt(LocalDateTime.now())
                .build();

        Comment savedComment = commentRepository.save(comment);

        updateVirality(postId, request.getAuthorType());

        return mapToResponse(savedComment);
    }

    private void validateDepth(int depthLevel) {
        if (depthLevel > MAX_DEPTH) {
            throw new RuntimeException("Maximum comment depth exceeded");
        }
    }

    private void handleBotGuardrails(Long postId, CreateCommentRequest request) {
        if (request.getAuthorType() == AuthorType.BOT) {

            boolean canReply = redisGuardrailService.incrementBotReplyCount(postId);

            if (!canReply) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Bot reply limit exceeded for this post");
            }

            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("Post not found"));

            if (post.getAuthorType() == AuthorType.USER) {
                boolean cooldownOk = redisGuardrailService.checkCooldown(request.getAuthorId(), post.getAuthorId());

                if (!cooldownOk) {
                    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Bot is on cooldown for this human");
                }

                String message = "Bot " + request.getAuthorId() + " replied to your post";
                notificationService.handleBotNotification(post.getAuthorId(), message);
            }
        }
    }

    private void updateVirality(Long postId, AuthorType authorType) {
        if (authorType == AuthorType.USER) {
            viralityService.addHumanCommentScore(postId);
        } else if (authorType == AuthorType.BOT) {
            viralityService.addBotReplyScore(postId);
        }
    }

    private CommentResponse mapToResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .authorId(comment.getAuthorId())
                .authorType(comment.getAuthorType())
                .content(comment.getContent())
                .depthLevel(comment.getDepthLevel())
                .parentCommentId(
                        comment.getParentComment() != null
                                ? comment.getParentComment().getId()
                                : null
                )
                .createdAt(comment.getCreatedAt())
                .build();
    }

    public List<CommentResponse> getAllComments(Long postId) {
        List<Comment> comments = commentRepository.findByPostId(postId);

        return comments.stream()
                .map(this::mapToResponse)
                .toList();
    }
}

