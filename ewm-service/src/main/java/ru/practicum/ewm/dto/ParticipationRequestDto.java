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
public class ParticipationRequestDto {
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    Long id;
    @JsonFormat(pattern = DATE_PATTERN)
    LocalDateTime created;
    Long event;
    Long requester;
    String status;
}