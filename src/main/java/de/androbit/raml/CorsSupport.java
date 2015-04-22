package de.androbit.raml;

import de.androbit.nibbler.http.Header;
import de.androbit.nibbler.http.RestRequestHandler;
import de.androbit.nibbler.http.RestResponse;

import java.util.function.Function;

public interface CorsSupport {
  Header AccessControlAllowOrigin = new Header("Access-Control-Allow-Origin");
  Header AccessControlAllowHeaders = new Header("Access-Control-Allow-Headers");
  Header AccessControlAllowCredentials = new Header("Access-Control-Allow-Credentials");

  default Function<RestResponse, RestResponse> corsResponseHeaders() {
    return (response) -> response.header(AccessControlAllowOrigin, "*");
  }

  default RestRequestHandler corsOptionsHeaders() {
    return (req, response) -> response
      .header(AccessControlAllowHeaders, "Content-Type, *")
      .header(AccessControlAllowCredentials, "true")
      .header(AccessControlAllowOrigin, "*");
  }
}
