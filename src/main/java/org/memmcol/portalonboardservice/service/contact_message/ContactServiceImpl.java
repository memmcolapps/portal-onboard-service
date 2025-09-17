package org.memmcol.portalonboardservice.service.contact_message;

import org.memmcol.portalonboardservice.config.ResponseProperties;
import org.memmcol.portalonboardservice.mapper.ContactMessageMapper;
import org.memmcol.portalonboardservice.mapper.PortalUserMapper;
import org.memmcol.portalonboardservice.model.audit.AuditLog;
import org.memmcol.portalonboardservice.model.audit.ExceptionErrorLogs;
import org.memmcol.portalonboardservice.model.user.ContactMessage;
import org.memmcol.portalonboardservice.model.user.ContactMessageSearchCriteria;
import org.memmcol.portalonboardservice.model.user.Operator;
import org.memmcol.portalonboardservice.model.user.ReadMessages;
import org.memmcol.portalonboardservice.repository.AuditRepository;
import org.memmcol.portalonboardservice.repository.ExceptionAuditRepository;
import org.memmcol.portalonboardservice.service.organization.OnboardOrganizationServiceImpl;
import org.memmcol.portalonboardservice.util.ResponseMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.memmcol.portalonboardservice.components.handleValidUser.handleUserValidation;

@Service
public class ContactServiceImpl implements ContactService{

    private final ContactMessageMapper contactMessageMapper;
    private final ExceptionAuditRepository exceptionAuditRepository;
    private static final Logger log = LoggerFactory.getLogger(ContactServiceImpl.class);

    @Autowired
    private ResponseProperties status;

    @Autowired
    private AuditRepository auditRepository;

    private PortalUserMapper portalUserMapper;

    public ContactServiceImpl(ContactMessageMapper contactMessageMapper,
                              ExceptionAuditRepository exceptionAuditRepository) {
        this.contactMessageMapper = contactMessageMapper;
        this.exceptionAuditRepository = exceptionAuditRepository;
    }

    @Transactional
    @Override
    public Map<String, Object> addMessage(ContactMessage message) {

        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
        try {
            contactMessageMapper.insertContactMessage(message);

            return ResponseMap.response(
                    status.getSuccessCode(), "Message sent successfully", "");

        }catch (Exception exception) {
            log.error("Error sending message: {}", exception.getMessage(), exception);

            // Log exception to audit system
            errorLog.setDescription("Error sending message");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);
            throw exception;

        }
    }

    @Transactional
    @Override
    public Map<String, Object> addReadMessage(UUID msgId) {
        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
        try {

            Operator operatorAction = handleUserValidation();
            UUID currentOperator = operatorAction.getId();

            ReadMessages cMsg = contactMessageMapper.getReadMessagesByMessageId(msgId);
            if (cMsg == null) {
                ReadMessages readMessages = new ReadMessages();

                readMessages.setUserId(currentOperator);
                readMessages.setMessageId(msgId);
                readMessages.setStatus(true);
                contactMessageMapper.insertReadMessage(readMessages);
            }

            return ResponseMap.response(
                    status.getSuccessCode(), "Message marked read successfully", "");

        }catch (Exception exception) {
            log.error("Error mark read message: {}", exception.getMessage(), exception);

            // Log exception to audit system
            errorLog.setDescription("Error mark read message");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);
            throw exception;

        }
    }

    @Override
    public Map<String, Object> searchMessages(ContactMessageSearchCriteria criteria, int page, int size) {
        try {

            Operator operatorAction = handleUserValidation();
            UUID currentOperator = operatorAction.getId();

            List<ContactMessage> filteredMessages = contactMessageMapper.searchContactMessages(criteria,currentOperator);
            int totalCount = filteredMessages.size();

            List<ContactMessage> paginatedMessages;

            if (size == 0) {
                paginatedMessages = filteredMessages;
            } else {
                int fromIndex = Math.min(page * size, totalCount);
                int toIndex = Math.min(fromIndex + size, totalCount);

                if (fromIndex > toIndex) {
                    fromIndex = toIndex;
                }

                paginatedMessages = filteredMessages.subList(fromIndex, toIndex);
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("messages", paginatedMessages);
            responseData.put("totalCount", totalCount);
            responseData.put("page", page);
            responseData.put("size", size);
            responseData.put("totalPages", size == 0 ? 1 : (int) Math.ceil((double) totalCount / size));

            return ResponseMap.response(status.getSuccessCode(), status.getDesc(), responseData);

        } catch (Exception exception) {
            log.error("Error searching messages: {}", exception.getMessage(), exception);

            ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
            errorLog.setDescription("Error searching messages");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);

            throw exception;
        }
    }

}
