package ru.practicum.ewm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.*;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.ForbiddenException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.*;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.*;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.EndpointHit;
import ru.practicum.stats.dto.ViewStats;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import ru.practicum.ewm.service.EventService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRequestRepository requestRepository;
    private final StatsClient statsClient;

    private final EventMapper eventMapper;
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final LocationMapper locationMapper;
    private final ParticipationRequestMapper requestMapper;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== Private endpoints ====================

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findByInitiatorId(userId, pageable);
        if (events.isEmpty()) return List.of();

        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        Map<Long, Long> viewsMap = getViewsForEvents(eventIds);
        Map<Long, Long> confirmedMap = getConfirmedRequestsForEvents(eventIds);

        return events.stream()
                .map(e -> eventMapper.toShortDto(e, viewsMap.getOrDefault(e.getId(), 0L)))
                .peek(dto -> dto.setConfirmedRequests(confirmedMap.getOrDefault(dto.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto addEvent(Long userId, NewEventDto newEventDto) {
        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        LocalDateTime now = LocalDateTime.now();
        if (newEventDto.getEventDate().isBefore(now.plusHours(2))) {
            throw new ForbiddenException("Event date must be at least 2 hours from now");
        }

        Event event = eventMapper.toEntity(newEventDto, category, initiator);
        event.setCreatedOn(now);
        event.setState(EventState.PENDING);
        event.setConfirmedRequests(0L);
        event.setParticipantLimit(Optional.ofNullable(newEventDto.getParticipantLimit()).orElse(0));
        event.setPaid(Optional.ofNullable(newEventDto.getPaid()).orElse(false));
        event.setRequestModeration(Optional.ofNullable(newEventDto.getRequestModeration()).orElse(true));
        event.setLocation(locationMapper.toEntity(newEventDto.getLocation()));

        event = eventRepository.save(event);
        return eventMapper.toFullDto(event, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getUserEventById(Long userId, Long eventId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        Long views = getViewsForEvent(eventId);
        return eventMapper.toFullDto(event, views);
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        LocalDateTime now = LocalDateTime.now();
        if (updateRequest.getEventDate() != null && updateRequest.getEventDate().isBefore(now.plusHours(2))) {
            throw new ForbiddenException("Event date must be at least 2 hours from now");
        }

        // Update fields
        if (updateRequest.getAnnotation() != null) event.setAnnotation(updateRequest.getAnnotation());
        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            event.setCategory(category);
        }
        if (updateRequest.getDescription() != null) event.setDescription(updateRequest.getDescription());
        if (updateRequest.getEventDate() != null) event.setEventDate(updateRequest.getEventDate());
        if (updateRequest.getLocation() != null) event.setLocation(locationMapper.toEntity(updateRequest.getLocation()));
        if (updateRequest.getPaid() != null) event.setPaid(updateRequest.getPaid());
        if (updateRequest.getParticipantLimit() != null) event.setParticipantLimit(updateRequest.getParticipantLimit());
        if (updateRequest.getRequestModeration() != null) event.setRequestModeration(updateRequest.getRequestModeration());
        if (updateRequest.getTitle() != null) event.setTitle(updateRequest.getTitle());

        if (updateRequest.getStateAction() != null) {
            switch (updateRequest.getStateAction()) {
                case "SEND_TO_REVIEW":
                    event.setState(EventState.PENDING);
                    break;
                case "CANCEL_REVIEW":
                    event.setState(EventState.CANCELED);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown state action: " + updateRequest.getStateAction());
            }
        }

        event = eventRepository.save(event);
        Long views = getViewsForEvent(eventId);
        return eventMapper.toFullDto(event, views);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventRequestsByOwner(Long userId, Long eventId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!eventRepository.existsByIdAndInitiatorId(eventId, userId)) {
            throw new NotFoundException("Event not found or not owned by user");
        }
        return requestRepository.findByEventIdAndEventInitiatorId(eventId, userId).stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest requestUpdate) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event not found or not owned"));

        if (event.getParticipantLimit() == 0 || !event.getRequestModeration()) {
            throw new ConflictException("Moderation is disabled or limit is zero");
        }

        List<ParticipationRequest> requests = requestRepository.findAllById(requestUpdate.getRequestIds());
        for (ParticipationRequest req : requests) {
            if (!req.getEvent().getId().equals(eventId)) {
                throw new ConflictException("Request does not belong to this event");
            }
            if (req.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Request must have status PENDING");
            }
        }

        long currentConfirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        long available = event.getParticipantLimit() - currentConfirmed;

        List<ParticipationRequest> confirmed = new ArrayList<>();
        List<ParticipationRequest> rejected = new ArrayList<>();

        if ("CONFIRMED".equals(requestUpdate.getStatus())) {
            for (ParticipationRequest req : requests) {
                if (available > 0) {
                    req.setStatus(RequestStatus.CONFIRMED);
                    confirmed.add(req);
                    available--;
                } else {
                    req.setStatus(RequestStatus.REJECTED);
                    rejected.add(req);
                }
            }
            // If limit is reached, reject all remaining pending requests for this event
            if (available == 0) {
                List<ParticipationRequest> pendingOthers = requestRepository.findByEventIdAndStatus(eventId, RequestStatus.PENDING);
                for (ParticipationRequest other : pendingOthers) {
                    if (!requests.contains(other)) {
                        other.setStatus(RequestStatus.REJECTED);
                        rejected.add(other);
                    }
                }
            }
        } else if ("REJECTED".equals(requestUpdate.getStatus())) {
            for (ParticipationRequest req : requests) {
                req.setStatus(RequestStatus.REJECTED);
                rejected.add(req);
            }
        } else {
            throw new IllegalArgumentException("Unknown status: " + requestUpdate.getStatus());
        }

        requestRepository.saveAll(confirmed);
        requestRepository.saveAll(rejected);

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        result.setConfirmedRequests(confirmed.stream().map(requestMapper::toDto).collect(Collectors.toList()));
        result.setRejectedRequests(rejected.stream().map(requestMapper::toDto).collect(Collectors.toList()));
        return result;
    }

    // ==================== Admin endpoints ====================

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> getEventsByAdmin(List<Long> users, List<String> states, List<Long> categories,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<EventState> stateEnums = states == null ? null :
                states.stream().map(EventState::valueOf).collect(Collectors.toList());

        List<Event> events = eventRepository.findAllForAdmin(users, stateEnums, categories, rangeStart, rangeEnd, pageable);
        if (events.isEmpty()) return List.of();

        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        Map<Long, Long> viewsMap = getViewsForEvents(eventIds);
        Map<Long, Long> confirmedMap = getConfirmedRequestsForEvents(eventIds);

        return events.stream()
                .map(e -> eventMapper.toFullDto(e, viewsMap.getOrDefault(e.getId(), 0L)))
                .peek(dto -> dto.setConfirmedRequests(confirmedMap.getOrDefault(dto.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        // Проверка даты
        if (updateRequest.getEventDate() != null) {
            LocalDateTime now = LocalDateTime.now();
            // Если дата уже наступила (в прошлом) - 400 BAD_REQUEST
            if (updateRequest.getEventDate().isBefore(now)) {
                throw new IllegalArgumentException("Event date cannot be in the past");
            }
            // Если дата в пределах часа от текущего момента - 409 CONFLICT
            if (updateRequest.getEventDate().isBefore(now.plusHours(1))) {
                throw new ForbiddenException("Event date must be at least 1 hour from now");
            }
            event.setEventDate(updateRequest.getEventDate());
        }

        // Update fields
        if (updateRequest.getAnnotation() != null) event.setAnnotation(updateRequest.getAnnotation());
        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            event.setCategory(category);
        }
        if (updateRequest.getDescription() != null) event.setDescription(updateRequest.getDescription());
        if (updateRequest.getEventDate() != null) event.setEventDate(updateRequest.getEventDate());
        if (updateRequest.getLocation() != null) event.setLocation(locationMapper.toEntity(updateRequest.getLocation()));
        if (updateRequest.getPaid() != null) event.setPaid(updateRequest.getPaid());
        if (updateRequest.getParticipantLimit() != null) event.setParticipantLimit(updateRequest.getParticipantLimit());
        if (updateRequest.getRequestModeration() != null) event.setRequestModeration(updateRequest.getRequestModeration());
        if (updateRequest.getTitle() != null) event.setTitle(updateRequest.getTitle());

        if (updateRequest.getStateAction() != null) {
            switch (updateRequest.getStateAction()) {
                case "PUBLISH_EVENT":
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException("Cannot publish the event because it's not in the right state");
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;
                case "REJECT_EVENT":
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConflictException("Cannot reject already published event");
                    }
                    event.setState(EventState.CANCELED);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown state action: " + updateRequest.getStateAction());
            }
        }

        event = eventRepository.save(event);
        Long views = getViewsForEvent(eventId);
        return eventMapper.toFullDto(event, views);
    }

    // ==================== Public endpoints ====================

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getEventsPublic(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Boolean onlyAvailable, String sort, int from, int size,
                                               HttpServletRequest request) {
        // Save hit to stats service
        saveHit(request);

        Pageable pageable;
        if ("VIEWS".equals(sort)) {
            pageable = PageRequest.of(from / size, size);
        } else {
            pageable = PageRequest.of(from / size, size, Sort.by("eventDate").ascending());
        }

        List<Event> events = eventRepository.findAllPublic(text, categories, paid, rangeStart, rangeEnd, pageable);
        if (events.isEmpty()) return List.of();

        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        Map<Long, Long> viewsMap = getViewsForEvents(eventIds);
        Map<Long, Long> confirmedMap = getConfirmedRequestsForEvents(eventIds);

        // Filter onlyAvailable
        if (onlyAvailable) {
            events = events.stream()
                    .filter(e -> e.getParticipantLimit() == 0 ||
                            confirmedMap.getOrDefault(e.getId(), 0L) < e.getParticipantLimit())
                    .collect(Collectors.toList());
        }

        List<EventShortDto> dtos = events.stream()
                .map(e -> eventMapper.toShortDto(e, viewsMap.getOrDefault(e.getId(), 0L)))
                .peek(dto -> dto.setConfirmedRequests(confirmedMap.getOrDefault(dto.getId(), 0L)))
                .collect(Collectors.toList());

        // Sort by views if needed (in-memory, because DB sort by views is complex)
        if ("VIEWS".equals(sort)) {
            dtos.sort(Comparator.comparingLong(EventShortDto::getViews).reversed());
        }

        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getEventPublic(Long id, HttpServletRequest request) {
        saveHit(request);

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event not published");
        }

        Long views = getViewsForEvent(id);
        Long confirmed = requestRepository.countByEventIdAndStatus(id, RequestStatus.CONFIRMED);
        EventFullDto dto = eventMapper.toFullDto(event, views);
        dto.setConfirmedRequests(confirmed);
        return dto;
    }

    // ==================== Helper methods ====================

    private void saveHit(HttpServletRequest request) {
        EndpointHit hit = new EndpointHit();
        hit.setApp("ewm-main-service");
        hit.setUri(request.getRequestURI());
        hit.setIp(request.getRemoteAddr());
        hit.setTimestamp(LocalDateTime.now().format(FORMATTER));
        statsClient.saveHit(hit);
    }

    private Long getViewsForEvent(Long eventId) {
        try {
            List<ViewStats> stats = statsClient.getStats(
                    LocalDateTime.now().minusYears(100),
                    LocalDateTime.now().plusYears(100),
                    List.of("/events/" + eventId),
                    true
            );
            return stats.isEmpty() ? 0L : stats.get(0).getHits();
        } catch (Exception e) {
            log.error("Failed to get views for event {}: {}", eventId, e.getMessage());
            return 0L;
        }
    }

    private Map<Long, Long> getViewsForEvents(List<Long> eventIds) {
        if (eventIds.isEmpty()) return Map.of();
        try {
            List<String> uris = eventIds.stream().map(id -> "/events/" + id).collect(Collectors.toList());
            List<ViewStats> stats = statsClient.getStats(
                    LocalDateTime.now().minusYears(100),
                    LocalDateTime.now().plusYears(100),
                    uris,
                    true
            );
            return stats.stream().collect(Collectors.toMap(
                    s -> Long.parseLong(s.getUri().substring(s.getUri().lastIndexOf('/') + 1)),
                    ViewStats::getHits
            ));
        } catch (Exception e) {
            log.error("Failed to get views for events: {}", e.getMessage());
            return Map.of();
        }
    }

    private Map<Long, Long> getConfirmedRequestsForEvents(List<Long> eventIds) {
        if (eventIds.isEmpty()) return Map.of();
        Map<Long, Long> result = new HashMap<>();
        for (Long id : eventIds) {
            Long count = requestRepository.countByEventIdAndStatus(id, RequestStatus.CONFIRMED);
            result.put(id, count);
        }
        return result;
    }
}