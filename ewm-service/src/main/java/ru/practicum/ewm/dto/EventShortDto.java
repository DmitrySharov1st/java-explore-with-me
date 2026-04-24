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
public class EventShortDto {
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    Long id;
    String annotation;
    CategoryDto category;
    Long confirmedRequests;
    @JsonFormat(pattern = DATE_PATTERN)
    LocalDateTime eventDate;
    UserShortDto initiator;
    Boolean paid;
    String title;
    Long views;
}