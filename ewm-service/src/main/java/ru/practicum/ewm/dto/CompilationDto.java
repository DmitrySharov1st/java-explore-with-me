package ru.practicum.ewm.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompilationDto {
    Long id;
    List<EventShortDto> events;
    Boolean pinned;
    String title;
}
