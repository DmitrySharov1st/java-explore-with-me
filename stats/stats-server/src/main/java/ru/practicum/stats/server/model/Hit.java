package ru.practicum.stats.server.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PRIVATE;

@Entity
@Table(name = "hits")
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = PRIVATE)
public class Hit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String app;
    String uri;
    String ip;
    LocalDateTime timestamp;
}