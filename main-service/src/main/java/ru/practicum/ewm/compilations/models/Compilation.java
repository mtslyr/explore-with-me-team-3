package ru.practicum.ewm.compilations.models;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
@Table(name = "compilations")
public class Compilation {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Setter
    @Column(name = "pinned")
    private boolean pinned;
    @Setter
    @Column(name = "title", length = 50)
    private String title;
    @OneToMany(mappedBy = "compilation", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<CompilationEvent> events;
}