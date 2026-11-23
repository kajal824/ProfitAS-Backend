package profitas.profitas.backend.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Boots the whole app on H2 and drives it through the real security filter chain. */
@SpringBootTest
class ApiFlowIntegrationTest {

    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    // ---- helpers ----

    private ResultActions send(MockHttpServletRequestBuilder req, String token, String json) throws Exception {
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (json != null) req.contentType(MediaType.APPLICATION_JSON).content(json);
        return mvc.perform(req);
    }

    private String register(String email, String extraJson) throws Exception {
        String body = send(post("/api/v1/auth/register"), null,
                "{\"email\":\"" + email + "\",\"password\":\"Password123\",\"fullName\":\"Test User\"" + extraJson + "}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private String adminToken() throws Exception {
        String body = send(post("/api/v1/auth/login"), null,
                "{\"email\":\"admin@test.local\",\"password\":\"AdminPass123!\"}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private int userId(String adminToken, String email) throws Exception {
        String body = send(get("/api/v1/admin/users"), adminToken, null)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(body, "$[?(@.email=='" + email + "')].id");
        return ids.get(0);
    }

    private int createAsset(String adminToken, int ownerId, String valuation) throws Exception {
        String body = send(post("/api/v1/admin/assets"), adminToken,
                "{\"ownerId\":" + ownerId + ",\"assetName\":\"Tech Park 4B\",\"currentValuation\":" + valuation
                        + ",\"monthlyRentalIncome\":5000}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    // ---- tests ----

    @Test
    void protectedEndpointsRequireAuthentication() throws Exception {
        send(get("/api/v1/assets"), null, null).andExpect(status().isUnauthorized());
        send(get("/api/v1/assets"), "garbage.token.value", null).andExpect(status().isUnauthorized());
    }

    @Test
    void registrationValidatesInput() throws Exception {
        send(post("/api/v1/auth/register"), null,
                "{\"email\":\"not-an-email\",\"password\":\"short\",\"fullName\":\"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.email").exists())
                .andExpect(jsonPath("$.details.password").exists());
    }

    @Test
    void duplicateEmailIsConflictAndLoginFailureIs401() throws Exception {
        register("dup@example.com", "");
        send(post("/api/v1/auth/register"), null,
                "{\"email\":\"DUP@example.com\",\"password\":\"Password123\",\"fullName\":\"X\"}")
                .andExpect(status().isConflict());
        send(post("/api/v1/auth/login"), null, "{\"email\":\"dup@example.com\",\"password\":\"wrong-password\"}")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void selfRegisteredUserCannotBecomeAdmin() throws Exception {
        String token = register("sneaky@example.com", ",\"role\":\"ADMIN\"");
        send(get("/api/v1/admin/users"), token, null).andExpect(status().isForbidden());
    }

    @Test
    void fullLendingAndExitFlow() throws Exception {
        String admin = adminToken();
        String alice = register("alice@example.com", "");
        String bob = register("bob@example.com", "");
        int assetId = createAsset(admin, userId(admin, "alice@example.com"), "1000000");

        // Ownership: bob can't see or borrow against alice's asset.
        send(get("/api/v1/assets/" + assetId), bob, null).andExpect(status().isForbidden());
        send(post("/api/v1/loans/apply"), bob, "{\"assetId\":" + assetId + ",\"requestedAmount\":1000}")
                .andExpect(status().isForbidden());
        send(get("/api/v1/assets"), alice, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(assetId));

        // Input validation + business rules.
        send(post("/api/v1/loans/apply"), alice, "{\"assetId\":" + assetId + ",\"requestedAmount\":-5}")
                .andExpect(status().isBadRequest());
        send(post("/api/v1/loans/apply"), alice, "{\"assetId\":999999,\"requestedAmount\":100}")
                .andExpect(status().isNotFound());
        send(post("/api/v1/loans/apply"), alice, "{\"assetId\":" + assetId + ",\"requestedAmount\":700000.01}")
                .andExpect(status().isUnprocessableEntity());

        // Borrow exactly 70% LTV; any more is refused because the limit covers all loans on the asset.
        String loanBody = send(post("/api/v1/loans/apply"), alice,
                "{\"assetId\":" + assetId + ",\"requestedAmount\":700000}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.interestRate").value(11.0))
                .andReturn().getResponse().getContentAsString();
        int loanId = JsonPath.read(loanBody, "$.id");
        send(post("/api/v1/loans/apply"), alice, "{\"assetId\":" + assetId + ",\"requestedAmount\":1}")
                .andExpect(status().isUnprocessableEntity());

        // Can't exit while a loan is open.
        send(post("/api/v1/exit/request"), alice, "{\"assetId\":" + assetId + "}")
                .andExpect(status().isUnprocessableEntity());

        // Bob can't repay alice's loan; alice can't overpay.
        send(post("/api/v1/loans/" + loanId + "/repay"), bob, "{\"amount\":10}").andExpect(status().isForbidden());
        send(post("/api/v1/loans/" + loanId + "/repay"), alice, "{\"amount\":700000.01}")
                .andExpect(status().isUnprocessableEntity());

        // Partial then full repayment.
        send(post("/api/v1/loans/" + loanId + "/repay"), alice, "{\"amount\":100000}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.remainingBalance").value(600000.0));
        send(post("/api/v1/loans/" + loanId + "/repay"), alice, "{\"amount\":600000}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REPAID"));

        // Exit: payout is 97% of the SERVER-side valuation (a client-sent valuation is ignored).
        send(post("/api/v1/exit/request"), alice, "{\"assetId\":" + assetId + ",\"originalValuation\":1}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SETTLED"))
                .andExpect(jsonPath("$.originalValuation").value(1000000.0))
                .andExpect(jsonPath("$.payoutAmount").value(970000.0));

        // Asset is now EXITED; a second exit and new loans are refused.
        send(get("/api/v1/assets/" + assetId), alice, null).andExpect(jsonPath("$.status").value("EXITED"));
        send(post("/api/v1/exit/request"), alice, "{\"assetId\":" + assetId + "}")
                .andExpect(status().isUnprocessableEntity());
        send(post("/api/v1/loans/apply"), alice, "{\"assetId\":" + assetId + ",\"requestedAmount\":1}")
                .andExpect(status().isUnprocessableEntity());
        send(get("/api/v1/exit"), alice, null).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void adminCanUpdateValuationAndDefaultALoan() throws Exception {
        String admin = adminToken();
        String carol = register("carol@example.com", "");
        int assetId = createAsset(admin, userId(admin, "carol@example.com"), "200000");

        send(put("/api/v1/admin/assets/" + assetId + "/valuation"), admin, "{\"currentValuation\":300000}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.currentValuation").value(300000.0));

        String loanBody = send(post("/api/v1/loans/apply"), carol,
                "{\"assetId\":" + assetId + ",\"requestedAmount\":210000}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int loanId = JsonPath.read(loanBody, "$.id");

        send(post("/api/v1/admin/loans/" + loanId + "/default"), carol, null).andExpect(status().isForbidden());
        send(post("/api/v1/admin/loans/" + loanId + "/default"), admin, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DEFAULTED"));
        send(post("/api/v1/admin/loans/" + loanId + "/default"), admin, null)
                .andExpect(status().isUnprocessableEntity());
        assertEquals(1, ((List<?>) JsonPath.read(
                send(get("/api/v1/admin/loans?status=DEFAULTED"), admin, null)
                        .andReturn().getResponse().getContentAsString(), "$[?(@.id==" + loanId + ")]")).size());
    }
}
