package uk.gov.hmcts.reform.professionalapi.controller.request;

import org.junit.Test;
import org.springframework.dao.EmptyResultDataAccessException;

public class ProfessionalUserReqValidatorTest {

    ProfessionalUserReqValidator profUserReqValidator = new ProfessionalUserReqValidator();

    @Test(expected = EmptyResultDataAccessException.class)
    public void testValidateRequestAllNull() {
        profUserReqValidator
                .validateRequest(null,null,null);
    }

    @Test
    public void testValidateRequestNoneNull() {
        profUserReqValidator
                .validateRequest("ordId","true","some@email.com");
    }
}