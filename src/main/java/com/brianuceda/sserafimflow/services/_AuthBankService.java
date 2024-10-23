package com.brianuceda.sserafimflow.services;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.brianuceda.sserafimflow.dtos.BankDTO;
import com.brianuceda.sserafimflow.dtos.ResponseDTO;
import com.brianuceda.sserafimflow.entities.BankEntity;
import com.brianuceda.sserafimflow.enums.AuthRoleEnum;
import com.brianuceda.sserafimflow.enums.CurrencyEnum;
import com.brianuceda.sserafimflow.implementations._AuthBankImpl;
import com.brianuceda.sserafimflow.respositories.BankRepository;
import com.brianuceda.sserafimflow.utils.JwtUtils;

@Service
public class _AuthBankService implements _AuthBankImpl {
  private final AuthenticationManager authenticationManager;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtils jwtUtils;
  private final BankRepository bankRepository;

  public _AuthBankService(AuthenticationManager authenticationManager, PasswordEncoder passwordEncoder,
      JwtUtils jwtUtils,
      BankRepository bankRepository) {
    this.authenticationManager = authenticationManager;
    this.passwordEncoder = passwordEncoder;
    this.jwtUtils = jwtUtils;
    this.bankRepository = bankRepository;
  }

  @Override
  public ResponseDTO register(BankDTO bankDTO) {
    if (bankRepository.findByUsername(bankDTO.getUsername()).isPresent()) {
      throw new BadCredentialsException("El banco ya existe");
    }

    BankEntity bank = BankEntity.builder()
        .realName(bankDTO.getRealName())
        .ruc(bankDTO.getRuc())
        .username(bankDTO.getUsername())
        .password(passwordEncoder.encode(bankDTO.getPassword()))
        .image(bankDTO.getImage() != null ? bankDTO.getImage() : "https://i.ibb.co/BrwL76K/bank.png")
        .currency(CurrencyEnum.PEN)
        .balance(BigDecimal.valueOf(0.0))
        .role(AuthRoleEnum.BANK)
        .creationDate(Timestamp.from(Instant.now()))
        .nominalRate(bankDTO.getNominalRate())
        .effectiveRate(bankDTO.getEffectiveRate())
        .extraCommission(bankDTO.getExtraCommission())
        .build();

    bankRepository.save(bank);

    ResponseDTO response = new ResponseDTO();
    response.setToken(jwtUtils.genToken(bank));

    return response;
  }

  @Override
  public ResponseDTO login(BankDTO bankDTO) {
    authenticationManager
        .authenticate(new UsernamePasswordAuthenticationToken(bankDTO.getUsername(), bankDTO.getPassword()));
    UserDetails userDetails = bankRepository.findByUsername(bankDTO.getUsername()).get();
    ResponseDTO response = new ResponseDTO();
    response.setToken(jwtUtils.genToken(userDetails));
    return response;
  }

  @Override
  public ResponseDTO logout(String token) {
    if (!jwtUtils.isTokenBlacklisted(token)) {
      jwtUtils.addTokenToBlacklist(token);
      return new ResponseDTO("Desconectado exitosamente");
    } else {
      throw new BadCredentialsException("No se pudo desconectar");
    }
  }
}
