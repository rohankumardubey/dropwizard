package io.dropwizard.auth.oauth;

import io.dropwizard.auth.AbstractAuthResourceConfig;
import io.dropwizard.auth.AuthBaseTest;
import io.dropwizard.auth.AuthResource;
import io.dropwizard.auth.util.AuthUtil;
import io.dropwizard.jersey.DropwizardResourceConfig;
import jakarta.ws.rs.container.ContainerRequestFilter;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthProviderTest extends AuthBaseTest<OAuthProviderTest.OAuthTestResourceConfig> {
    public static class OAuthTestResourceConfig extends AbstractAuthResourceConfig {
        public OAuthTestResourceConfig() {
            register(AuthResource.class);
        }

        @Override protected ContainerRequestFilter getAuthFilter() {
            return new OAuthCredentialAuthFilter.Builder<>()
                .setAuthenticator(AuthUtil.getMultiplyUsersOAuthAuthenticator(Arrays.asList(ADMIN_USER, ORDINARY_USER)))
                .setAuthorizer(AuthUtil.getTestAuthorizer(ADMIN_USER, ADMIN_ROLE))
                .setPrefix(BEARER_PREFIX)
                .buildAuthFilter();
        }
    }

    @Test
    void checksQueryStringAccessTokenIfAuthorizationHeaderMissing() {
        assertThat(target("/test/profile")
            .queryParam(OAuthCredentialAuthFilter.OAUTH_ACCESS_TOKEN_PARAM, getOrdinaryGuyValidToken())
            .request()
            .get(String.class))
            .isEqualTo("'%s' has user privileges", ORDINARY_USER);
    }

    @Override
    protected DropwizardResourceConfig getDropwizardResourceConfig() {
        return new OAuthTestResourceConfig();
    }

    @Override
    protected Class<OAuthTestResourceConfig> getDropwizardResourceConfigClass() {
        return OAuthTestResourceConfig.class;
    }

    @Override
    protected String getPrefix() {
        return BEARER_PREFIX;
    }

    @Override
    protected String getOrdinaryGuyValidToken() {
        return "ordinary-guy";
    }

    @Override
    protected String getGoodGuyValidToken() {
        return "good-guy";
    }

    @Override
    protected String getBadGuyToken() {
        return "bad-guy";
    }
}
