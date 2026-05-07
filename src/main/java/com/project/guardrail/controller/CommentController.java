package com.project.guardrail.controller;

import com.project.guardrail.dto.request.CreateCommentRequest;
import com.project.guardrail.dto.response.CommentResponse;
import com.project.guardrail.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentResponse> createComment
            (@PathVariable Long postId, @Valid @RequestBody CreateCommentRequest request) {

        CommentResponse response = commentService.createComment(postId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CommentResponse>> getAllComments(@PathVariable Long postId) {

        List<CommentResponse> responses = commentService.getAllComments(postId);

        return ResponseEntity.ok(responses);
    }

}
