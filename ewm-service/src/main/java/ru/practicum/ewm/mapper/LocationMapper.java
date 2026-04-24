package ru.practicum.ewm.mapper;

import ru.practicum.ewm.dto.LocationDto;
import ru.practicum.ewm.model.Location;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LocationMapper {
    LocationDto toDto(Location location);

    Location toEntity(LocationDto locationDto);
}