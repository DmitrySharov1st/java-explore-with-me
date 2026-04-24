package ru.practicum.ewm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventFullDto {
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    Long id;
    String annotation;
    CategoryDto category;
    Long confirmedRequests;
    @JsonFormat(pattern = DATE_PATTERN)
    LocalDateTime createdOn;
    String description;
    @JsonFormat(pattern = DATE_PATTERN)
    LocalDateTime eventDate;
    UserShortDto initiator;
    LocationDto location;
    Boolean paid;
    Integer participantLimit;
    @JsonFormat(pattern = DATE_PATTERN)
    LocalDateTime publishedOn;
    Boolean requestModeration;
    String state;
    String title;
    Long views;
}