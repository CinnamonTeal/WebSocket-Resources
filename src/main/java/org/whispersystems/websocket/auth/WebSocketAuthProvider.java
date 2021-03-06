/**
 * Copyright (C) 2014 Open WhisperSystems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.whispersystems.websocket.auth;

import com.google.common.base.Optional;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.model.Parameter;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.server.impl.inject.AbstractHttpContextInjectable;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.whispersystems.websocket.servlet.WebSocketServletRequest;
import org.whispersystems.websocket.session.WebSocketSessionContext;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.security.Principal;

import io.dropwizard.auth.Auth;

public class WebSocketAuthProvider implements InjectableProvider<Auth, Parameter> {

  private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthProvider.class);

  private static class WebSocketAuthInjectable<T> extends AbstractHttpContextInjectable<T> {

    private final boolean  required;
    private final Class<T> clazz;

    private WebSocketAuthInjectable(boolean required, Class<T> clazz) {
      this.required = required;
      this.clazz    = clazz;
    }

    @Override
    public T getValue(HttpContext c) {
      Principal principal = c.getRequest().getUserPrincipal();

      if (principal != null && principal instanceof WebSocketServletRequest.ContextPrincipal) {
        WebSocketSessionContext context       = ((WebSocketServletRequest.ContextPrincipal)principal).getContext();
        Optional<T>             authenticated = context.getAuthenticated(clazz);

        if (authenticated.isPresent()) {
          return authenticated.get();
        }
      }

      if (required) {
        throw new WebApplicationException(Response.Status.UNAUTHORIZED);
      }

      return null;
    }
  }

  @Override
  public ComponentScope getScope() {
    return ComponentScope.PerRequest;
  }

  @Override
  public Injectable getInjectable(ComponentContext ic, Auth a, Parameter c) {
    return new WebSocketAuthInjectable(a.required(), c.getParameterClass());
  }
}
