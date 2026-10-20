package com.workbench.workshop.domain;

import com.workbench.workshop.domain.client.Cnpj;
import com.workbench.workshop.domain.client.Cpf;
import com.workbench.workshop.domain.client.Document;
import com.workbench.workshop.domain.client.DocumentType;
import com.workbench.workshop.domain.vehicle.LicensePlate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentAndPlateTest {

    @Test
    void validCpfNormalizesAndFormats() {
        Cpf cpf = new Cpf("529.982.247-25");
        assertEquals("52998224725", cpf.getValue());
        assertEquals("529.982.247-25", cpf.formatted());
    }

    @Test
    void cpfRejectsWrongCheckDigit() {
        assertThrows(IllegalArgumentException.class, () -> new Cpf("52998224726"));
    }

    @Test
    void cpfRejectsAllIdenticalDigits() {
        assertThrows(IllegalArgumentException.class, () -> new Cpf("11111111111"));
    }

    @Test
    void cpfRejectsWrongLength() {
        assertThrows(IllegalArgumentException.class, () -> new Cpf("123"));
    }

    @Test
    void validCnpjNormalizesAndFormats() {
        Cnpj cnpj = new Cnpj("11.444.777/0001-61");
        assertEquals("11444777000161", cnpj.getValue());
        assertEquals("11.444.777/0001-61", cnpj.formatted());
    }

    @Test
    void cnpjRejectsWrongCheckDigit() {
        assertThrows(IllegalArgumentException.class, () -> new Cnpj("11444777000162"));
    }

    @Test
    void documentDetectsCpfVsCnpjByLength() {
        assertEquals(DocumentType.CPF, Document.of("529.982.247-25").getType());
        assertEquals(DocumentType.CNPJ, Document.of("11.444.777/0001-61").getType());
    }

    @Test
    void documentRejectsInvalidLength() {
        assertThrows(IllegalArgumentException.class, () -> Document.of("12345"));
    }

    @Test
    void documentFormattedByType() {
        assertEquals("529.982.247-25", Document.of("52998224725").formatted());
        assertEquals("11.444.777/0001-61", Document.of("11444777000161").formatted());
    }

    @Test
    void plateAcceptsOldAndMercosul() {
        assertEquals("ABC1234", new LicensePlate("abc-1234").getValue());
        assertEquals("ABC1D23", new LicensePlate("ABC1D23").getValue());
        assertEquals("ABC-1234", new LicensePlate("ABC1234").formatted());
    }

    @Test
    void plateRejectsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> new LicensePlate("1234567"));
        assertThrows(IllegalArgumentException.class, () -> new LicensePlate(""));
    }
}
