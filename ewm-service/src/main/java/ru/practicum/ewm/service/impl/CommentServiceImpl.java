package ru.practicum.ewm.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.CommentDto;
import ru.practicum.ewm.dto.NewCommentDto;
import ru.practicum.ewm.dto.UpdateCommentDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CommentMapper;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.CommentRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.service.CommentService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CommentMapper commentMapper;

    // Публичное получение одобренных комментариев
    @Override
    public List<CommentDto> getEventComments(Long eventId, int from, int size) {
        eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Event with id=%d was not found", eventId)));
        PageRequest page = PageRequest.of(from / size, size);
        return commentRepository.findByEventIdAndStatus(eventId, CommentStatus.APPROVED, page)
                .stream()
                .map(commentMapper::toDto)
                .collect(Collectors.toList());
    }

    // Добавление комментария пользователем
    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot comment on unpublished event");
        }
        Comment comment = Comment.builder()
                .text(newCommentDto.getText())
                .author(author)
                .event(event)
                .created(LocalDateTime.now())
                .edited(false)
                .status(CommentStatus.APPROVED)
                .build();
        comment = commentRepository.save(comment);
        return commentMapper.toDto(comment);
    }

    // Редактирование своего комментария
    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto updateCommentDto) {
        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException("Comment not found or you are not the author"));
        if (comment.getStatus() == CommentStatus.DELETED) {
            throw new ConflictException("Cannot edit deleted comment");
        }
        comment.setText(updateCommentDto.getText());
        comment.setEdited(true);
        return commentMapper.toDto(comment);
    }

    // Удаление (пометка как удалённый) автором
    @Override
    @Transactional
    public void deleteCommentByUser(Long userId, Long commentId) {
        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException("Comment not found or you are not the author"));
        comment.setStatus(CommentStatus.DELETED);
    }

    // Администратор: получение комментариев с фильтрацией
    @Override
    public List<CommentDto> getCommentsByAdmin(Long eventId, String status, int from, int size) {
        PageRequest page = PageRequest.of(from / size, size);
        CommentStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            statusEnum = CommentStatus.valueOf(status.toUpperCase());
        }
        return commentRepository.findAllForAdmin(eventId, statusEnum, page)
                .stream()
                .map(commentMapper::toDto)
                .collect(Collectors.toList());
    }

    // Администратор: изменение статуса комментария
    @Override
    @Transactional
    public CommentDto updateCommentStatusByAdmin(Long commentId, String newStatus) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found"));
        CommentStatus statusEnum = CommentStatus.valueOf(newStatus.toUpperCase());
        comment.setStatus(statusEnum);
        return commentMapper.toDto(comment);
    }
}