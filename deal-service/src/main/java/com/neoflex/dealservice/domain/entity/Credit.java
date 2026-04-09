package com.neoflex.dealservice.domain.entity;

import com.neoflex.deal.dto.CreditStatus;
import com.neoflex.deal.dto.PaymentScheduleElementDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "deal", name = "credits")
public class Credit {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "credit_id", columnDefinition = "uuid")
    private UUID creditId;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "term")
    private Integer term;

    @Column(name = "monthly_payment", precision = 19, scale = 2)
    private BigDecimal monthlyPayment;

    @Column(name = "rate", precision = 5, scale = 2)
    private BigDecimal rate;

    @Column(name = "psk", precision = 5, scale = 2)
    private BigDecimal psk;

    @Column(name = "is_insurance_enabled")
    private Boolean isInsuranceEnabled;

    @Column(name = "is_salary_client")
    private Boolean isSalaryClient;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payment_schedule", columnDefinition = "jsonb")
    private List<PaymentScheduleElementDto> paymentSchedule;

    @Column(name = "credit_status", nullable = false)
    private CreditStatus creditStatus;

    @OneToOne(mappedBy = "credit")
    private Statement statement;

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
        if (creditStatus == null) {
            creditStatus = CreditStatus.CALCULATED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updated = Instant.now();
    }

}
