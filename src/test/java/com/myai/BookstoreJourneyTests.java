package com.myai;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** End-to-end run of the customer journey from the use case slides. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class BookstoreJourneyTests {

    private static final String ADDRESS = """
            {"firstName":"Asha","lastName":"Rao","line1":"12 MG Road","city":"Bengaluru","state":"Karnataka",
             "pin":"560001","country":"India","phone":"+919876543210"}""";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private JsonNode body(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    private String register(String email) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Passw0rd!long\",\"name\":\"Asha\"}"))
                .andExpect(status().isCreated()).andReturn();
        return "Bearer " + body(r).get("token").asString();
    }

    @Test
    void guestCanBrowseButNotUseCart() throws Exception {
        mvc.perform(get("/api/home")).andExpect(status().isOk())
                .andExpect(jsonPath("$.bestsellers.length()").value(3))
                .andExpect(jsonPath("$.newLaunches.length()").value(3));
        mvc.perform(get("/api/categories")).andExpect(status().isOk());
        mvc.perform(get("/api/publishers")).andExpect(status().isOk());
        mvc.perform(get("/api/books?q=minimalism")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void authValidationAndBadLogin() throws Exception {
        register("dup@example.com");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"DUP@example.com\",\"password\":\"Passw0rd!long\",\"name\":\"x\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"bad\",\"password\":\"short\",\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"dup@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"dup@example.com\",\"password\":\"Passw0rd!long\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void fullPurchaseJourneyWithGiftPointsAndCancellation() throws Exception {
        String auth = register("journey@example.com");

        // Browse: category -> brand -> product detail with related reads
        JsonNode cats = body(mvc.perform(get("/api/categories")).andReturn());
        long selfHelp = 0;
        for (JsonNode c : cats) {
            if (c.get("name").asString().equals("Self-help")) {
                selfHelp = c.get("id").asLong();
            }
        }
        JsonNode page = body(mvc.perform(get("/api/books?categoryId=" + selfHelp + "&publisher=ABC Publishers"))
                .andExpect(status().isOk()).andReturn());
        long joyId = page.get("content").get(0).get("id").asLong();
        mvc.perform(get("/api/books/" + joyId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.book.title").value("Joy of Minimalism"))
                .andExpect(jsonPath("$.book.tentativeDeliveryDate").isNotEmpty())
                .andExpect(jsonPath("$.relatedBooks.length()").value(3));
        long pathId = body(mvc.perform(get("/api/books?q=path to success")).andReturn())
                .get("content").get(0).get("id").asLong();

        // Add to basket: 149 + 359 = 508, tax 12% = 60.96
        mvc.perform(post("/api/cart/items").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookId\":" + joyId + ",\"quantity\":1}")).andExpect(status().isOk());
        MvcResult cart = mvc.perform(post("/api/cart/items").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"bookId\":" + pathId + ",\"quantity\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.subtotal").value(508.00))
                .andExpect(jsonPath("$.tax").value(60.96)).andExpect(jsonPath("$.total").value(568.96))
                .andReturn();
        long joyItem = body(cart).get("items").get(0).get("id").asLong();
        mvc.perform(patch("/api/cart/items/" + joyItem).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest());

        // Checkout requires a saved address that belongs to the user
        mvc.perform(post("/api/orders").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"addressId\":999,\"paymentMethod\":\"UPI\",\"giftPointsToRedeem\":0}"))
                .andExpect(status().isBadRequest());
        long addressId = body(mvc.perform(post("/api/addresses").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(ADDRESS)).andExpect(status().isCreated())
                .andReturn()).get("id").asLong();

        // No points yet, so redeeming is rejected
        mvc.perform(post("/api/orders").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"addressId\":" + addressId + ",\"paymentMethod\":\"CREDIT_CARD\",\"giftPointsToRedeem\":10}"))
                .andExpect(status().isBadRequest());

        // Pay: 568.96 -> earns 5 points
        MvcResult order = mvc.perform(post("/api/orders").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"addressId\":" + addressId + ",\"paymentMethod\":\"CREDIT_CARD\",\"giftPointsToRedeem\":0}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.paymentStatus").value("PAID")).andExpect(jsonPath("$.total").value(568.96))
                .andExpect(jsonPath("$.giftPointsEarned").value(5)).andReturn();
        long orderId = body(order).get("id").asLong();
        mvc.perform(get("/api/cart").header("Authorization", auth)).andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(get("/api/me").header("Authorization", auth)).andExpect(jsonPath("$.giftPoints").value(5));

        // Order history + buy-again + recommendations
        mvc.perform(get("/api/orders").header("Authorization", auth)).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/orders/buy-again").header("Authorization", auth))
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/home").header("Authorization", auth)).andExpect(status().isOk())
                // Self-help has 4 books; the 2 already bought are excluded
                .andExpect(jsonPath("$.recommended.length()").value(2))
                .andExpect(jsonPath("$.recommended[?(@.title=='Joy of Minimalism')]").isEmpty())
                .andExpect(jsonPath("$.recommended[?(@.title=='The Path to Success')]").isEmpty());

        // Second order redeems the 5 points
        mvc.perform(post("/api/cart/items").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookId\":" + joyId + ",\"quantity\":2}")).andExpect(status().isOk());
        mvc.perform(post("/api/orders").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"addressId\":" + addressId + ",\"paymentMethod\":\"WALLET\",\"giftPointsToRedeem\":5}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.discount").value(5.00))
                .andExpect(jsonPath("$.giftPointsRedeemed").value(5));

        // Cancel within 48 h refunds; a second cancel conflicts
        mvc.perform(post("/api/orders/" + orderId + "/cancel").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.paymentStatus").value("REFUNDED"));
        mvc.perform(post("/api/orders/" + orderId + "/cancel").header("Authorization", auth))
                .andExpect(status().isConflict());
    }

    @Test
    void usersCannotSeeEachOthersData() throws Exception {
        String alice = register("alice@example.com");
        String bob = register("bob@example.com");
        long addressId = body(mvc.perform(post("/api/addresses").header("Authorization", alice)
                .contentType(MediaType.APPLICATION_JSON).content(ADDRESS)).andReturn()).get("id").asLong();
        long bookId = body(mvc.perform(get("/api/books?size=1")).andReturn()).get("content").get(0).get("id").asLong();
        long item = body(mvc.perform(post("/api/cart/items").header("Authorization", alice)
                .contentType(MediaType.APPLICATION_JSON).content("{\"bookId\":" + bookId + ",\"quantity\":1}"))
                .andReturn()).get("items").get(0).get("id").asLong();
        long orderId = body(mvc.perform(post("/api/orders").header("Authorization", alice)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"addressId\":" + addressId + ",\"paymentMethod\":\"UPI\",\"giftPointsToRedeem\":0}"))
                .andReturn()).get("id").asLong();

        mvc.perform(get("/api/orders/" + orderId).header("Authorization", bob)).andExpect(status().isNotFound());
        mvc.perform(post("/api/orders/" + orderId + "/cancel").header("Authorization", bob))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/addresses/" + addressId).header("Authorization", bob))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/cart/items/" + item).header("Authorization", bob)).andExpect(status().isNotFound());
        mvc.perform(get("/api/orders").header("Authorization", bob)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void stockIsEnforced() throws Exception {
        String auth = register("stock@example.com");
        long bookId = body(mvc.perform(get("/api/books?size=1")).andReturn()).get("content").get(0).get("id").asLong();
        mvc.perform(post("/api/cart/items").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookId\":" + bookId + ",\"quantity\":21}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/cart/items").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookId\":987654,\"quantity\":1}")).andExpect(status().isNotFound());
        mvc.perform(get("/api/books?minPrice=500&maxPrice=100")).andExpect(status().isBadRequest());
    }

    @Test
    void forgedOrExpiredTokenIsRejected() throws Exception {
        mvc.perform(get("/api/cart").header("Authorization", "Bearer not.a.jwt")).andExpect(status().isUnauthorized());
        // csrf() is unused on purpose: this API is stateless/bearer-only, so POST works without a CSRF token.
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nobody@example.com\",\"password\":\"whatever-long\"}"))
                .andExpect(status().isUnauthorized());
    }
}
