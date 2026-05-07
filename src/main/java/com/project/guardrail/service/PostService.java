package com.project.guardrail.service;

import com.project.guardrail.dto.request.CreatePostRequest;
import com.project.guardrail.dto.response.PostResponse;
import com.project.guardrail.entity.Bot;
import com.project.guardrail.entity.Post;
import com.project.guardrail.entity.User;
import com.project.guardrail.entity.enums.AuthorType;
import com.project.guardrail.repository.BotRepository;
import com.project.guardrail.repository.PostRepository;
import com.project.guardrail.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    private final UserRepository userRepository;
    private final BotRepository botRepository;

    private final ViralityService viralityService;

    @Transactional
    public PostResponse createPost(CreatePostRequest request) {

        validateAuthor(request.getAuthorId(),request.getAuthorType());

        Post post = Post.builder()
                .authorId(request.getAuthorId())
                .authorType(request.getAuthorType())
                .content(request.getContent())
                .createdAt(LocalDateTime.now())
                .build();

        Post savedPost =
                postRepository.save(post);

        return mapToResponse(savedPost);
    }

    public PostResponse getPost(Long postId) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() ->
                        new RuntimeException("Post not found"));

        return mapToResponse(post);
    }

    private void validateAuthor(Long authorId,AuthorType authorType) {

        if (authorType == AuthorType.USER) {

            User user = userRepository.findById(authorId)
                    .orElseThrow(() ->
                            new RuntimeException("User not found"));

        } else {

            Bot bot = botRepository.findById(authorId)
                    .orElseThrow(() ->
                            new RuntimeException("Bot not found"));
        }
    }

    private PostResponse mapToResponse(Post post) {

        Long viralityScore =
                viralityService.getViralityScore(post.getId());

        return PostResponse.builder()
                .id(post.getId())
                .authorId(post.getAuthorId())
                .authorType(post.getAuthorType())
                .content(post.getContent())
                .viralityScore(viralityScore)
                .createdAt(post.getCreatedAt())
                .build();
    }
}