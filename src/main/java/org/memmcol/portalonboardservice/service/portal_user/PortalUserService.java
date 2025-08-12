package org.memmcol.portalonboardservice.service.portal_user;

import org.memmcol.portalonboardservice.model.user.Operator;

import java.util.Map;
import java.util.UUID;

public interface PortalUserService {

    Map<String, Object> getAll();

    Map<String, Object> logout();

    Map<String, Object> createOperator(Operator operator);

    Map<String, Object> updateOperator(Operator operator);

    Map<String, Object> blockOperator(UUID id, boolean status);

    Map<String, Object> getSingle(UUID id);

    Map<String, Object> generateOtp(String username);

    Map<String, Object> verifyOtp(String username, String otp, String password, String retypePassword);
}
