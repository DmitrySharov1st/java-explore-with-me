package ru.practicum.ewm.mapper;

import org.mapstruct.*;
import ru.practicum.ewm.dto.EventFullDto;
import ru.practicum.ewm.dto.EventShortDto;
import ru.practicum.ewm.dto.NewEventDto;
import ru.practicum.ewm.dto.LocationDto;
import ru.practicum.ewm.model.*;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class, UserMapper.class, LocationMapper.class})
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "category")
    @Mapping(target = "initiator", source = "initiator")
    @Mapping(target = "createdOn", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "state", constant = "PENDING")
    @Mapping(target = "confirmedRequests", constant = "0L")
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "location", source = "newEventDto.location", qualifiedByName = "mapLocation")
    @Mapping(target = "paid", source = "newEventDto.paid", defaultValue = "false")
    @Mapping(target = "participantLimit", source = "newEventDto.participantLimit", defaultValue = "0")
    @Mapping(target = "requestModeration", source = "newEventDto.requestModeration", defaultValue = "true")
    Event toEntity(NewEventDto newEventDto, Category category, User initiator);

    @Mapping(target = "category", source = "event.category")
    @Mapping(target = "initiator", source = "event.initiator")
    @Mapping(target = "views", source = "views")
    EventFullDto toFullDto(Event event, Long views);

    @Mapping(target = "category", source = "event.category")
    @Mapping(target = "initiator", source = "event.initiator")
    @Mapping(target = "views", source = "views")
    EventShortDto toShortDto(Event event, Long views);

    @Named("mapLocation")
    static Location mapLocation(LocationDto locationDto) {
        if (locationDto == null) return null;
        return Location.builder()
                .lat(locationDto.getLat())
                .lon(locationDto.getLon())
                .build();
    }
}