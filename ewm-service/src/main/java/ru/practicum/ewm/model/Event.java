package ru.practicum.ewm.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false, length = 2000)
    String annotation;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    Category category;
    @Column(name = "confirmed_requests")
    Long confirmedRequests; // не хранится в БД, вычисляется
    @Column(name = "created_on", nullable = false)
    LocalDateTime createdOn;
    @Column(nullable = false, length = 7000)
    String description;
    @Column(name = "event_date", nullable = false)
    LocalDateTime eventDate;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id", nullable = false)
    User initiator;
    @Embedded
    Location location;
    @Column(nullable = false)
    Boolean paid;
    @Column(name = "participant_limit", nullable = false)
    Integer participantLimit;
    @Column(name = "published_on")
    LocalDateTime publishedOn;
    @Column(name = "request_moderation", nullable = false)
    Boolean requestModeration;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    EventState state;
    @Column(nullable = false, length = 120)
    String title;
}