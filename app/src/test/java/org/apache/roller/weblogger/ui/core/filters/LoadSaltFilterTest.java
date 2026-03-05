package org.apache.roller.weblogger.ui.core.filters;

import org.apache.roller.weblogger.pojos.User;
import org.apache.roller.weblogger.ui.core.RollerSession;
import org.apache.roller.weblogger.ui.rendering.util.cache.SaltCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.*;

public class LoadSaltFilterTest {

    private LoadSaltFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @Mock
    private RollerSession rollerSession;

    @Mock
    private SaltCache saltCache;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        filter = spy(new LoadSaltFilter());
    }

    @Test
    public void testDoFilterGeneratesSalt() throws Exception {
        doReturn(rollerSession).when(filter).getRollerSession(request);
        doReturn(saltCache).when(filter).getSaltCache();

        when(rollerSession.getAuthenticatedUser()).thenReturn(new TestUser("userId"));

        filter.doFilter(request, response, chain);

        verify(request).setAttribute(eq("salt"), anyString());
        verify(saltCache).put(anyString(), eq("userId"));
        verify(chain).doFilter(request, response);
    }

    @Test
    public void testDoFilterWithNullRollerSession() throws Exception {
        doReturn(null).when(filter).getRollerSession(request);
        doReturn(saltCache).when(filter).getSaltCache();

        filter.doFilter(request, response, chain);

        verify(request, never()).setAttribute(eq("salt"), anyString());
        verify(saltCache, never()).put(anyString(), anyString());
        verify(chain).doFilter(request, response);
    }

    private static class TestUser extends User {
        private final String id;

        TestUser(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }
}
