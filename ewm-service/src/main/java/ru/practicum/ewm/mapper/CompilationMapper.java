package ru.practicum.ewm.mapper;

import org.mapstruct.*;
import ru.practicum.ewm.dto.CompilationDto;
import ru.practicum.ewm.dto.NewCompilationDto;
import ru.practicum.ewm.dto.UpdateCompilationRequest;
import ru.practicum.ewm.model.Compilation;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", uses = {EventMapper.class})
public abstract class CompilationMapper {

    @Autowired
    protected EventRepository eventRepository;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", expression = "java(mapEvents(newCompilationDto.getEvents()))")
    public abstract Compilation toEntity(NewCompilationDto newCompilationDto);

    @Mapping(target = "events", ignore = true)   // игнорируем, т.к. заполняем в сервисе с views
    public abstract CompilationDto toDto(Compilation compilation);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", expression = "java(mapEvents(request.getEvents()))")
    public abstract void updateEntity(@MappingTarget Compilation compilation, UpdateCompilationRequest request);

    protected Set<Event> mapEvents(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(eventRepository.findAllById(eventIds));
    }
}