package ru.practicum.ewm.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@ToString
public class ErrorResponse {

    final String status;
    final String reason;
    final String message;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    final LocalDateTime timestamp = LocalDateTime.now();
}
