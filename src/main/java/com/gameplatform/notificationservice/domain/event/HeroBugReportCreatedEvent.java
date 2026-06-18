package com.gameplatform.notificationservice.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeroBugReportCreatedEvent {

    private UUID eventId;
    private UUID bugReportId;
    private Long heroId;
    private String heroSlug;
    private String heroName;
    private UUID authorId;
    private String authorName;
    private String description;
    private OffsetDateTime createdAt;
}
