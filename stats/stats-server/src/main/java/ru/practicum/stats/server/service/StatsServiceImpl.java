package ru.practicum.stats.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.EndpointHit;
import ru.practicum.stats.dto.ViewStats;
import ru.practicum.stats.server.mapper.HitMapper;
import ru.practicum.stats.server.repository.StatsHitRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private final StatsHitRepository repository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public void saveHit(EndpointHit hitDto) {
        log.info("Saving hit: {}", hitDto);
        LocalDateTime timestamp = LocalDateTime.parse(hitDto.getTimestamp(), FORMATTER);
        repository.save(HitMapper.toEntity(hitDto, timestamp));
    }

    @Override
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        log.info("Get stats: start={}, end={}, uris={}, unique={}", start, end, uris, unique);
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
        if (uris != null && uris.isEmpty()) {
            uris = null;
        }
        if (unique) {
            return repository.getStatsWithUnique(start, end, uris);
        } else {
            return repository.getStatsWithoutUnique(start, end, uris);
        }
    }
}