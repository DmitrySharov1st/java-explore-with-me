package ru.practicum.stats.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EndpointHit {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;
    private String app;
    private String uri;
    private String ip;
    private String timestamp; // формат "yyyy-MM-dd HH:mm:ss"
}