package com.example.communicationapp;

import static org.junit.Assert.assertEquals;

import com.example.communicationapp.data.SmsRepository;

import org.junit.Test;

public class SmsAddressNormalizerTest {
    @Test
    public void normalizeAddress_removesFormatting() {
        assertEquals("+15551234567", SmsRepository.normalizeAddress("+1 (555) 123-4567"));
    }

    @Test
    public void normalizeAddress_preservesNonPhoneAddress() {
        assertEquals("SERVICE", SmsRepository.normalizeAddress("SERVICE"));
    }

    @Test
    public void escapeSearchQuery_escapesSqlWildcards() {
        assertEquals("50\\%\\_off", SmsRepository.escapeSearchQuery("50%_off"));
    }
}
