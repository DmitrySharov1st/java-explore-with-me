package ru.practicum.stats.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = PRIVATE)
public class EndpointHit {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    Long id;
    String app;
    String uri;
    String ip;
    String timestamp; // формат "yyyy-MM-dd HH:mm:ss"
}