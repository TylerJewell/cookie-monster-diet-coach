package com.example.api;

import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.http.HttpResponses;

/** Serves the React single-page app from {@code src/main/resources/static-resources}. */
@HttpEndpoint
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
public class StaticResourcesEndpoint {

  @Get("/")
  public HttpResponse index() {
    return HttpResponses.staticResource("index.html");
  }
}
