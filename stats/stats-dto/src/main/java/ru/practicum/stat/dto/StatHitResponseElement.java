package ru.practicum.stat.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StatHitResponseElement {
    String app;
    String uri;
    Integer hits;
}
