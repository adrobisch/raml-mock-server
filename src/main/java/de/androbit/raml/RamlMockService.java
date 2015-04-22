package de.androbit.raml;

import de.androbit.nibbler.RestServiceBuilder;
import de.androbit.nibbler.dsl.PathDefinition;
import de.androbit.nibbler.http.Header;
import de.androbit.nibbler.http.MediaType;
import de.androbit.nibbler.http.RestRequestHandler;
import de.androbit.nibbler.http.RestResponse;
import org.raml.emitter.RamlEmitter;
import org.raml.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class RamlMockService extends RestServiceBuilder implements CorsSupport {

  final Raml raml;
  final MediaType ramlMediaType = MediaType.valueOf("application/raml");

  Map<String, PathDefinition> pathDefinitionMap = new HashMap<>();

  public RamlMockService(Raml raml) {
    this.raml = raml;
  }

  @Override
  public void define() {
    setupMockRoutes();
  }

  private void setupMockRoutes() {
    raml.getResources().forEach(setupResourceRoute());

    RestRequestHandler ramlHandler = (req, rsp) -> rsp.body(new RamlEmitter().dump(raml)).header(Header.ContentType, ramlMediaType.contentType());

    path("/").get(type(ramlMediaType, ramlHandler));
  }

  private BiConsumer<String, Resource> setupResourceRoute() {
    return (resourceName, resource) -> resource.getActions().forEach((actionType, action) -> {
      setupPathForResourceAction(resource, action);
    });
  }

  private void setupPathForResourceAction(Resource resource, Action action) {
    pathDefinitionWithHandler(resource, action);
    setupSubResources(resource);
  }

  private void setupSubResources(Resource resource) {
    resource.getResources().forEach(setupResourceRoute());
  }

  private PathDefinition allowCorsOptionsRequest(PathDefinition pathDefinition) {
    return pathDefinition.options(corsOptionsHeaders());
  }

  private PathDefinition pathDefinitionWithHandler(Resource resource, Action action) {
    if (pathDefinitionMap.get(resource.getUri()) == null) {
      PathDefinition pathDefinition = withMethodHandlerForAction(
        action,
        allowCorsOptionsRequest(path(resource.getUri()))
      );

      pathDefinitionMap.put(resource.getUri(), pathDefinition);
      return pathDefinition;
    }

    return withMethodHandlerForAction(action, pathDefinitionMap.get(resource.getUri()));
  }

  private PathDefinition withMethodHandlerForAction(Action action, PathDefinition pathDefinition) {
    switch (action.getType()) {
      case GET:
        return pathDefinition.get(mockActionHandler(action));
      case POST:
        return pathDefinition.post(mockActionHandler(action));
      case PUT:
        return pathDefinition.put(mockActionHandler(action));
      case DELETE:
        return pathDefinition.delete(mockActionHandler(action));
      case OPTIONS:
        return pathDefinition.options(mockActionHandler(action));
      case PATCH:
        return pathDefinition.patch(mockActionHandler(action));
      default:
        throw new UnsupportedOperationException("unknown http method: " + action) ;
    }
  }

  RestRequestHandler mockActionHandler(Action action) {
    return (mockRequest, mockResponse) -> {

      action.getResponses()
        .entrySet()
        .stream()
        .findFirst().map(setResponseBodyAndStatus(mockResponse));

      return mockResponse.with(corsResponseHeaders());
    };
  }

  private Function<Map.Entry<String, Response>, RestResponse> setResponseBodyAndStatus(RestResponse mockResponse) {
    return actionResponse -> {
      mockResponse.status(Integer.valueOf(actionResponse.getKey()));

      Set<Map.Entry<String, MimeType>> bodies = actionResponse.getValue().getBody().entrySet();

      if (!bodies.isEmpty()) {
        MimeType firstBodyMimeType = bodies.stream().findFirst().get().getValue();
        mockResponse.body(firstBodyMimeType.getExample());
        mockResponse.header(Header.ContentType, firstBodyMimeType.getType());
      }

      return mockResponse;
    };
  }
}
