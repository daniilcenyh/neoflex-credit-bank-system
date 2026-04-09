package com.neoflex.dealservice.domain.entity;

import com.neoflex.deal.dto.EmploymentStatus;
import com.neoflex.deal.dto.Position;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "deal", name = "employments")
public class Employment {
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "employment_id", columnDefinition = "uuid")
    private UUID employmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", length = 50)
    private EmploymentStatus employmentStatus;

    @Column(name = "employer_inn", length = 12)
    private String employerINN;

    @Column(name = "salary", precision = 19, scale = 2)
    private BigDecimal salary;

    @Enumerated(EnumType.STRING)
    @Column(name = "position", length = 50)
    private Position position;

    @Column(name = "work_experience_total")
    private Integer workExperienceTotal;

    @Column(name = "work_experience_current")
    private Integer workExperienceCurrent;

    @OneToOne(mappedBy = "employment")
    private Client client;

    @NotNull
    @Column(name = "created", nullable = false)
    private Instant created;

    @NotNull
    @Column(name = "updated", nullable = false)
    private Instant updated;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        created = now;
        updated = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updated = Instant.now();
    }
}
