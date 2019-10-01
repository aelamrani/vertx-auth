/*
 * Copyright 2019 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.ext.auth.webauthn;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.AuthStore;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.webauthn.impl.WebAuthNImpl;

import static io.vertx.codegen.annotations.GenIgnore.PERMITTED_TYPE;

/**
 * Factory interface for creating WebAuthN based {@link io.vertx.ext.auth.AuthProvider} instances.
 *
 * @author Paulo Lopes
 */
@VertxGen
public interface WebAuthN extends AuthProvider {

  /**
   * Create a WebAuthN auth provider
   *
   * @param vertx the Vertx instance.
   * @param store the user store used to load credentials.
   * @return the auth provider.
   */
  static WebAuthN create(Vertx vertx, AuthStore store) {
    return create(vertx, new WebAuthNOptions(), store);
  }

  /**
   * Create a WebAuthN auth provider
   *
   * @param vertx the Vertx instance.
   * @param options the custom options to the provider.
   * @param store the user store used to load credentials.
   * @return the auth provider.
   */
  static WebAuthN create(Vertx vertx, WebAuthNOptions options, AuthStore store) {
    return new WebAuthNImpl(vertx, options, store);
  }

  /**
   * Generates makeCredentials request
   *
   * @param user    - the user object with username, displayName
   * @param type        - optional Credentials Challenge Type
   * @param handler server encoded make credentials request
   * @return fluent self
   */
  @Fluent
  WebAuthN generateServerCredentialsChallenge(JsonObject user, CredentialsChallengeType type, Handler<AsyncResult<JsonObject>> handler);

  /**
   * Same as {@link #generateServerCredentialsChallenge(JsonObject, CredentialsChallengeType, Handler)} but returning a Future.
   */
  default Future<JsonObject> generateServerCredentialsChallenge(JsonObject user, CredentialsChallengeType type) {
    Promise<JsonObject> promise = Promise.promise();
    generateServerCredentialsChallenge(user, type, promise);
    return promise.future();
  }

  /**
   * Generates getAssertion request
   *
   * @param username the unique user identified
   * @param handler server encoded get assertion request
   * @return fluent self.
   */
  @Fluent
  WebAuthN generateServerGetAssertion(String username, Handler<AsyncResult<JsonObject>> handler);

  /**
   * Same as {@link #generateServerGetAssertion(String, Handler)} but returning a Future.
   */
  default Future<JsonObject> generateServerGetAssertion(String username) {
    Promise<JsonObject> promise = Promise.promise();
    generateServerGetAssertion(username, promise);
    return promise.future();
  }

  @GenIgnore(PERMITTED_TYPE)
  void authenticate(WebAuthNInfo authInfo, Handler<AsyncResult<User>> handler);

  @GenIgnore(PERMITTED_TYPE)
  default Future<User> authenticate(WebAuthNInfo authInfo) {
    Promise<User> promise = Promise.promise();
    authenticate(authInfo, promise);
    return promise.future();
  }

  @Override
  default void authenticate(JsonObject authInfo, Handler<AsyncResult<User>> handler) {
    authenticate(new WebAuthNInfo(authInfo), handler);
  }
}
