package uk.gov.hmcts.reform.professionalapi;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.professionalapi.client.ProfessionalApiClient.createOrganisationRequest;
import static uk.gov.hmcts.reform.professionalapi.controller.request.NewUserCreationRequest.aNewUserCreationRequest;
import static uk.gov.hmcts.reform.professionalapi.helper.OrganisationFixtures.createJurisdictions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.professionalapi.controller.request.NewUserCreationRequest;
import uk.gov.hmcts.reform.professionalapi.controller.request.OrganisationCreationRequest;

@RunWith(SpringIntegrationSerenityRunner.class)
@ActiveProfiles("functional")
public class AddNewUserTest extends AuthorizationFunctionalTest {

    String orgIdentifierResponse = null;

    @Before
    public void createAndUpdateOrganisation() {
        Map<String, Object> response = professionalApiClient.createOrganisation();
        String orgIdentifierResponse = (String) response.get("organisationIdentifier");
        professionalApiClient.updateOrganisation(orgIdentifierResponse, hmctsAdmin);

        List<String> userRoles = new ArrayList<>();
        userRoles.add("pui-user-manager");
        String userEmail = randomAlphabetic(5).toLowerCase() + "@hotmail.com";
        String lastName = "someLastName";
        String firstName = "someName";

        bearerToken = professionalApiClient.getMultipleAuthHeadersExternal(hmctsAdmin, firstName, lastName, userEmail);

        NewUserCreationRequest userCreationRequest = aNewUserCreationRequest().firstName(firstName).lastName(lastName).email(userEmail).roles(userRoles).jurisdictions(createJurisdictions()).build();
        professionalApiClient.addNewUserToAnOrganisation(orgIdentifierResponse, hmctsAdmin, userCreationRequest, HttpStatus.CREATED, bearerToken);
    }

    @Test
    public void add_new_user_to_organisation() {
        NewUserCreationRequest newUserCreationRequest = professionalApiClient.createNewUserRequest();
        Map<String, Object> newUserResponse = professionalApiClient.addNewUserToAnOrganisation(orgIdentifierResponse, hmctsAdmin, newUserCreationRequest, HttpStatus.CREATED, bearerToken);
        assertThat(newUserResponse).isNotNull();
    }

    @Test
    public void add_new_user_to_organisation_with_no_jurisdiction_should_return_400() {

        NewUserCreationRequest newUserCreationRequest = professionalApiClient.createNewUserRequest();
        newUserCreationRequest.setJurisdictions(new ArrayList<>());
        Map<String, Object> newUserResponse = professionalApiClient.addNewUserToAnOrganisation(orgIdentifierResponse, hmctsAdmin, newUserCreationRequest, HttpStatus.BAD_REQUEST, bearerToken);
        assertThat(newUserResponse).isNotNull();
    }

    @Test
    public void add_new_user_to_organisation_with_unknown_roles_should_return_404() {

        List<String> roles = new ArrayList<>();
        roles.add("unknown");
        NewUserCreationRequest newUserCreationRequest = professionalApiClient.createNewUserRequest();
        newUserCreationRequest.setRoles(roles);

        Map<String, Object> newUserResponse = professionalApiClient.addNewUserToAnOrganisation(orgIdentifierResponse, hmctsAdmin, newUserCreationRequest, HttpStatus.NOT_FOUND, bearerToken);
        assertThat(newUserResponse).isNotNull();
    }

    @Test
    public void should_throw_409_when_add_duplicate_new_user_to_organisation() {

        // create pending org
        OrganisationCreationRequest pendingOrganisationCreationRequest = createOrganisationRequest().build();
        professionalApiClient.createOrganisation(pendingOrganisationCreationRequest);

        // create organisation to add normal user
        String organisationIdentifier = createAndUpdateOrganisationToActive(hmctsAdmin);

        // now invite same user/email used in above pending org should give CONFLICT
        NewUserCreationRequest newUserCreationRequest = professionalApiClient.createNewUserRequest();
        newUserCreationRequest.setEmail(pendingOrganisationCreationRequest.getSuperUser().getEmail());
        Map<String, Object> newUserResponse = professionalApiClient.addNewUserToAnOrganisation(organisationIdentifier, hmctsAdmin, newUserCreationRequest, HttpStatus.CONFLICT, bearerToken);
        assertThat((String) newUserResponse.get("errorDescription")).contains("409 User already exists");
    }
}
