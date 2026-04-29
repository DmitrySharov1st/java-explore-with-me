package ru.practicum.ewm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentDto {
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    Long id;
    String text;
    Long authorId;
    Long eventId;

    @JsonFormat(pattern = DATE_PATTERN)
    LocalDateTime created;

    Boolean edited;
    String status; // APPROVED, REJECTED, DELETED
}