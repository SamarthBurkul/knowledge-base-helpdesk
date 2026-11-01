package com.knowledgebase.helpdesk.controller;

import com.knowledgebase.helpdesk.model.Article;
import com.knowledgebase.helpdesk.service.ArticleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end HTTP checks against the real application (embedded Tomcat on a
 * random port, real Thymeleaf rendering). These prove that role rules are
 * enforced by the server, not just by hiding buttons, and that every page
 * renders for every role without template errors.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccessControlIntegrationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private ArticleService articleService;

    private static final String HTML = "text/html,application/xhtml+xml;q=0.9,*/*;q=0.8";

    /** One browser-like client per signed-in user: keeps cookies, never follows redirects. */
    private final class Client {
        final CookieManager cookies = new CookieManager();
        final HttpClient http = HttpClient.newBuilder()
                .cookieHandler(cookies)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        HttpResponse<String> get(String path) {
            return send(HttpRequest.newBuilder(url(path)).header("Accept", HTML).GET().build());
        }

        HttpResponse<String> post(String path, Map<String, String> form) {
            String body = form.entrySet().stream()
                    .map(e -> enc(e.getKey()) + "=" + enc(e.getValue()))
                    .collect(Collectors.joining("&"));
            return send(HttpRequest.newBuilder(url(path))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", HTML)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build());
        }

        String sessionId() {
            return cookies.getCookieStore().getCookies().stream()
                    .filter(c -> c.getName().equals("JSESSIONID"))
                    .map(HttpCookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        private HttpResponse<String> send(HttpRequest request) {
            try {
                return http.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }

    private URI url(String path) {
        return URI.create("http://localhost:" + environment.getProperty("local.server.port") + path);
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String location(HttpResponse<?> response) {
        return response.headers().firstValue("Location").orElse("");
    }

    private Client signIn(String username, String password) {
        Client client = new Client();
        client.get("/login");
        HttpResponse<String> response = client.post("/login",
                Map.of("username", username, "password", password));
        assertEquals(302, response.statusCode(), "login should redirect for " + username);
        assertTrue(location(response).endsWith("/dashboard"));
        return client;
    }

    private static Map<String, String> articleForm(String title, String status) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("title", title);
        form.put("description", "Integration test problem.");
        form.put("solution", "Integration test solution.");
        form.put("status", status);
        return form;
    }

    private Article findByTitle(String title) {
        return articleService.findAll().stream()
                .filter(a -> a.getTitle().equals(title))
                .findFirst()
                .orElse(null);
    }

    @Test
    void anonymousVisitorsAreSentToLogin() {
        Client anonymous = new Client();
        for (String path : new String[]{"/dashboard", "/articles", "/articles/1",
                "/articles/new", "/articles/edit/1", "/admin/users"}) {
            HttpResponse<String> response = anonymous.get(path);
            assertEquals(302, response.statusCode(), path);
            assertTrue(location(response).endsWith("/login"), path);
        }

        HttpResponse<String> post = anonymous.post("/articles/delete/1", Map.of());
        assertEquals(302, post.statusCode());
        assertNotNull(articleService.findById(1L), "anonymous delete must not remove anything");
    }

    @Test
    void invalidCredentialsAreRejected() {
        Client client = new Client();
        HttpResponse<String> response = client.post("/login",
                Map.of("username", "admin", "password", "wrong-password"));

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Invalid username or password"));
    }

    @Test
    void loginIssuesANewSessionIdAndLogoutEndsTheSession() {
        Client client = new Client();
        client.get("/login");
        String before = client.sessionId();

        client.post("/login", Map.of("username", "user", "password", "user123"));
        assertNotNull(client.sessionId());
        assertNotEquals(before, client.sessionId(), "session ID must change on login");

        assertEquals(200, client.get("/dashboard").statusCode());
        assertEquals(302, client.get("/logout").statusCode());
        assertTrue(location(client.get("/dashboard")).endsWith("/login"));

        // Signing in again (as a different user) on the same browser works.
        client.get("/login");
        HttpResponse<String> again = client.post("/login",
                Map.of("username", "support", "password", "support123"));
        assertTrue(location(again).endsWith("/dashboard"));
        assertEquals(200, client.get("/dashboard").statusCode());
        assertTrue(client.get("/articles/new").statusCode() == 200, "now signed in as SUPPORT");
    }

    @Test
    void everyPageRendersForEveryRole() {
        for (String[] account : new String[][]{
                {"admin", "admin123"}, {"support", "support123"}, {"user", "user123"}}) {
            Client client = signIn(account[0], account[1]);
            for (String path : new String[]{"/dashboard", "/articles", "/articles?view=table",
                    "/articles?view=list&sort=title", "/articles?keyword=wifi&status=PUBLISHED",
                    "/articles?keyword=zzz-no-match", "/articles/1"}) {
                HttpResponse<String> response = client.get(path);
                assertEquals(200, response.statusCode(), account[0] + " " + path);
                assertTrue(response.body().contains("</html>"), account[0] + " " + path);
            }
        }
    }

    @Test
    void regularUserCannotCreateEditOrDeleteEvenWithDirectRequests() {
        Client user = signIn("user", "user123");
        String title = "Forbidden create " + System.nanoTime();

        assertTrue(location(user.get("/articles/new")).endsWith("/articles"));
        assertTrue(location(user.get("/articles/edit/1")).endsWith("/articles"));

        user.post("/articles/create", articleForm(title, "PUBLISHED"));
        assertNull(findByTitle(title), "USER must not be able to create articles");

        String originalTitle = articleService.findById(2L).getTitle();
        user.post("/articles/update/2", articleForm("Hijacked", "PUBLISHED"));
        assertEquals(originalTitle, articleService.findById(2L).getTitle());

        user.post("/articles/delete/2", Map.of());
        assertNotNull(articleService.findById(2L), "USER must not be able to delete articles");

        HttpResponse<String> admin = user.get("/admin/users");
        assertEquals(302, admin.statusCode());
        assertTrue(location(admin).endsWith("/dashboard"));
    }

    @Test
    void supportCanCreateAndEditButNotDeleteOrManageUsers() {
        Client support = signIn("support", "support123");
        String title = "Support article " + System.nanoTime();

        assertEquals(200, support.get("/articles/new").statusCode());
        HttpResponse<String> created = support.post("/articles/create", articleForm(title, "PUBLISHED"));
        Article article = findByTitle(title);
        assertNotNull(article);
        assertEquals("support", article.getAuthor());
        assertTrue(location(created).endsWith("/articles/" + article.getId()));

        support.post("/articles/update/" + article.getId(), articleForm(title + " (edited)", "RESOLVED"));
        assertEquals(title + " (edited)", article.getTitle());
        assertEquals(Article.Status.RESOLVED, article.getStatus());

        support.post("/articles/delete/" + article.getId(), Map.of());
        assertNotNull(articleService.findById(article.getId()), "SUPPORT must not be able to delete");

        assertTrue(location(support.get("/admin/users")).endsWith("/dashboard"));
    }

    @Test
    void adminCanDeleteAndManageUsers() {
        Client admin = signIn("admin", "admin123");
        String title = "Admin delete target " + System.nanoTime();
        admin.post("/articles/create", articleForm(title, "DRAFT"));
        Article article = findByTitle(title);
        assertNotNull(article);

        HttpResponse<String> users = admin.get("/admin/users");
        assertEquals(200, users.statusCode());
        assertTrue(users.body().contains("Users &amp; roles"));

        HttpResponse<String> deleted = admin.post("/articles/delete/" + article.getId(), Map.of());
        assertTrue(location(deleted).endsWith("/articles"));
        assertNull(articleService.findById(article.getId()));
    }

    @Test
    void draftsAreHiddenFromRegularUsersOnTheServer() {
        Client support = signIn("support", "support123");
        String title = "Hidden draft " + System.nanoTime();
        support.post("/articles/create", articleForm(title, "DRAFT"));
        Article draft = findByTitle(title);
        assertNotNull(draft);

        assertTrue(support.get("/articles?status=DRAFT").body().contains(title));
        assertEquals(200, support.get("/articles/" + draft.getId()).statusCode());

        Client user = signIn("user", "user123");
        assertFalse(user.get("/articles").body().contains(title));
        assertFalse(user.get("/articles?status=DRAFT").body().contains(title));
        assertFalse(user.get("/dashboard").body().contains(title));
        HttpResponse<String> direct = user.get("/articles/" + draft.getId());
        assertEquals(302, direct.statusCode(), "USER must not open a draft by URL");
        assertTrue(location(direct).endsWith("/articles"));
    }

    @Test
    void invalidArticleInputIsRejectedWithFieldMessages() {
        Client support = signIn("support", "support123");
        long before = articleService.count();

        HttpResponse<String> blank = support.post("/articles/create", articleForm(" ", "PUBLISHED"));
        assertEquals(200, blank.statusCode());
        assertTrue(blank.body().contains("Enter a title for the article."));

        HttpResponse<String> badStatus = support.post("/articles/create",
                articleForm("Valid title " + System.nanoTime(), "ARCHIVED"));
        assertEquals(200, badStatus.statusCode());
        assertTrue(badStatus.body().contains("Choose Draft, Published or Resolved."));

        HttpResponse<String> tooLong = support.post("/articles/create",
                articleForm("x".repeat(ArticleController.TITLE_MAX + 1), "PUBLISHED"));
        assertTrue(tooLong.body().contains("Keep the title under"));

        assertEquals(before, articleService.count(), "invalid input must not create articles");
    }

    @Test
    void unknownPagesUseTheFriendlyErrorPage() {
        Client admin = signIn("admin", "admin123");
        HttpResponse<String> missing = admin.get("/no-such-page");
        assertEquals(404, missing.statusCode());
        assertTrue(missing.body().contains("Page not found"),
                () -> "unexpected 404 body: " + missing.body().substring(0, Math.min(400, missing.body().length())));
        assertFalse(missing.body().contains("Exception"));

        HttpResponse<String> missingArticle = admin.get("/articles/999999");
        assertEquals(302, missingArticle.statusCode());
    }

    @Test
    void healthEndpointReportsUp() {
        HttpResponse<String> health = new Client().get("/actuator/health");
        assertEquals(200, health.statusCode());
        assertTrue(health.body().contains("\"status\":\"UP\""));
    }
}
