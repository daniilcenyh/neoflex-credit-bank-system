package com.neoflex.calculator_service.service;

import net.proselyte.calculator.dto.LoanOfferDto;
import net.proselyte.calculator.dto.LoanStatementRequestDto;

import java.util.List;

public interface PrescoringService {
    List<LoanOfferDto> generateOffers(LoanStatementRequestDto request);
}
