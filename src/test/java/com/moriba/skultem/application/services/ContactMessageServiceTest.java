package com.moriba.skultem.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.moriba.skultem.infrastructure.persistence.entity.ContactMessageEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.ContactMessageJpaRepository;
import com.moriba.skultem.infrastructure.rest.dto.CreateContactMessageDTO;

class ContactMessageServiceTest {

    private final ContactMessageJpaRepository repo = mock(ContactMessageJpaRepository.class);
    private final LeadService leadService = mock(LeadService.class);
    private final ContactMessageService service = new ContactMessageService(repo, leadService);

    private static CreateContactMessageDTO form(String website) {
        return new CreateContactMessageDTO(" Jane Doe ", "jane@school.edu", " Bright Future Academy ", "+23276000000",
                " Collecting fees on time ", website);
    }

    @Test
    void storesAllFiveFieldsTrimmedAsANewMessage() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        service.submit(form(null));

        var saved = ArgumentCaptor.forClass(ContactMessageEntity.class);
        verify(repo).save(saved.capture());
        var m = saved.getValue();
        assertEquals("Jane Doe", m.getName());
        assertEquals("jane@school.edu", m.getEmail());
        assertEquals("Bright Future Academy", m.getSchool());
        assertEquals("+23276000000", m.getPhone());
        assertEquals("Collecting fees on time", m.getChallenge());
        assertEquals(ContactMessageEntity.Status.NEW, m.getStatus());
        // ...and is handed on to be followed up as a lead.
        verify(leadService).captureContactMessage(any(), eq("Jane Doe"), eq("Bright Future Academy"), eq("jane@school.edu"),
                eq("+23276000000"), eq("Collecting fees on time"));
    }

    @Test
    void aFilledHoneypotKeepsNothing() {
        service.submit(form("http://spam.example"));
        verify(repo, never()).save(any());
        verify(leadService, never()).captureContactMessage(any(), any(), any(), any(), any(), any());
    }
}
