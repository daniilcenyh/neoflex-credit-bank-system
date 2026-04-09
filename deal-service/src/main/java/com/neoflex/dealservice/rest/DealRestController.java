package com.neoflex.dealservice.rest;

import com.neoflex.deal.api.DealControllerApi;
import com.neoflex.deal.dto.FinishRegistrationRequestDto;
import com.neoflex.deal.dto.LoanOfferDto;
import com.neoflex.deal.dto.LoanStatementRequestDto;
import com.neoflex.dealservice.service.DealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DealRestController implements DealControllerApi {

    private final DealService dealService;

    @Override
    public ResponseEntity<Void> calculateCredit(UUID statementId, FinishRegistrationRequestDto finishRegistrationRequestDto) {
        dealService.calculateCredit(statementId, finishRegistrationRequestDto);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .build();
    }

    @Override
    public ResponseEntity<List<LoanOfferDto>> calculateStatement(LoanStatementRequestDto loanStatementRequestDto) {
        return ResponseEntity.ok(dealService.calculateStatement(loanStatementRequestDto));
    }

    @Override
    public ResponseEntity<Void> selectOffer(LoanOfferDto loanOfferDto) {
        dealService.selectOffer(loanOfferDto);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .build();
    }
}
