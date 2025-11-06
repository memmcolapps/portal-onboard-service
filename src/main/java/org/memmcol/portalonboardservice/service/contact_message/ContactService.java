package org.memmcol.portalonboardservice.service.contact_message;


import org.memmcol.portalonboardservice.model.user.ContactMessage;
import org.memmcol.portalonboardservice.model.user.ContactMessageSearchCriteria;
import org.memmcol.portalonboardservice.model.user.ReadMessages;

import java.util.Map;
import java.util.UUID;

public interface ContactService {

    Map<String, Object> addMessage(ContactMessage messages);
    Map<String, Object> addReadMessage(UUID msgId);
    Map<String, Object> searchMessages(String search,ContactMessageSearchCriteria searchTerm, int page, int size);
}
