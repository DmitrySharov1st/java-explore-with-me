package ru.practicum.stats.server.mapper;

import ru.practicum.stats.dto.EndpointHit;
import ru.practicum.stats.server.model.Hit;

import java.time.LocalDateTime;

public class HitMapper {
    public static Hit toEntity(EndpointHit dto, LocalDateTime timestamp) {
        Hit hit = new Hit();
        hit.setId(dto.getId());
        hit.setApp(dto.getApp());
        hit.setUri(dto.getUri());
        hit.setIp(dto.getIp());
        hit.setTimestamp(timestamp);
        return hit;
    }
}