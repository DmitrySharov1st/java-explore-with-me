package ru.practicum.ewm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewEventDto {
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @NotBlank @Size(min = 20, max = 2000)
    String annotation;
    @NotNull
    Long category;
    @NotBlank @Size(min = 20, max = 7000)
    String description;
    @NotNull @Future
    @JsonFormat(pattern = DATE_PATTERN)
    LocalDateTime eventDate;
    @NotNull
    LocationDto location;
    Boolean paid;
    @PositiveOrZero
    Integer participantLimit;
    Boolean requestModeration;
    @NotBlank @Size(min = 3, max = 120)
    String title;
}