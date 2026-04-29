package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.CommentDto;
import ru.practicum.ewm.dto.NewCommentDto;
import ru.practicum.ewm.dto.UpdateCommentDto;

import java.util.List;

public interface CommentService {
    // Public
    List<CommentDto> getEventComments(Long eventId, int from, int size);

    // Private
    CommentDto addComment(Long userId, Long eventId, NewCommentDto newCommentDto);

    CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto updateCommentDto);

    void deleteCommentByUser(Long userId, Long commentId);

    // Admin
    List<CommentDto> getCommentsByAdmin(Long eventId, String status, int from, int size);

    CommentDto updateCommentStatusByAdmin(Long commentId, String newStatus);
}