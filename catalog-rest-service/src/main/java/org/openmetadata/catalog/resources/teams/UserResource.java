/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements. See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openmetadata.catalog.resources.teams;

import com.google.inject.Inject;
import io.dropwizard.jersey.PATCH;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.openmetadata.catalog.api.teams.CreateUser;
import org.openmetadata.catalog.entity.teams.User;
import org.openmetadata.catalog.jdbi3.CollectionDAO;
import org.openmetadata.catalog.jdbi3.UserRepository;
import org.openmetadata.catalog.jdbi3.UserRepository.UserEntityInterface;
import org.openmetadata.catalog.resources.Collection;
import org.openmetadata.catalog.security.CatalogAuthorizer;
import org.openmetadata.catalog.security.SecurityUtil;
import org.openmetadata.catalog.type.EntityHistory;
import org.openmetadata.catalog.type.EntityReference;
import org.openmetadata.catalog.util.EntityUtil;
import org.openmetadata.catalog.util.EntityUtil.Fields;
import org.openmetadata.catalog.util.RestUtil;
import org.openmetadata.catalog.util.ResultList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonPatch;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Path("/v1/users")
@Api(value = "User collection", tags = "User collection")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Collection(name = "users")
public class UserResource {
  public static final Logger LOG = LoggerFactory.getLogger(UserResource.class);
  public static final String USER_COLLECTION_PATH = "v1/users/";
  private final UserRepository dao;
  private final CatalogAuthorizer authorizer;

  public static void addHref(UriInfo uriInfo, EntityReference user) {
    user.setHref(RestUtil.getHref(uriInfo, USER_COLLECTION_PATH, user.getId()));
  }

  public static User addHref(UriInfo uriInfo, User user) {
    user.setHref(RestUtil.getHref(uriInfo, USER_COLLECTION_PATH, user.getId()));
    Optional.ofNullable(user.getTeams()).orElse(Collections.emptyList()).forEach(t -> TeamResource.addHref(uriInfo, t));
    EntityUtil.addHref(uriInfo, user.getOwns());
    EntityUtil.addHref(uriInfo, user.getFollows());
    return user;
  }

  @Inject
  public UserResource(CollectionDAO dao, CatalogAuthorizer authorizer) {
    Objects.requireNonNull(dao, "UserRepository must not be null");
    this.dao = new UserRepository(dao);
    this.authorizer = authorizer;
  }

  public static class UserList extends ResultList<User> {
    @SuppressWarnings("unused")  // Used for deserialization
    public UserList() {}

    public UserList(List<User> users, String beforeCursor, String afterCursor, int total)
            throws GeneralSecurityException, UnsupportedEncodingException {
      super(users, beforeCursor, afterCursor, total);
    }
  }

  static final String FIELDS = "profile,teams,follows,owns";
  public static final List<String> FIELD_LIST = Arrays.asList(FIELDS.replaceAll(" ", "")
          .split(","));
  @GET
  @Valid
  @Operation(summary = "List users", tags = "users",
          description = "Get a list of users. Use `fields` " +
                  "parameter to get only necessary fields. Use cursor-based pagination to limit the number " +
                  "entries in the list using `limit` and `before` or `after` query params.",
          responses = {
                  @ApiResponse(responseCode = "200", description = "The user ",
                          content = @Content(mediaType = "application/json",
                          schema = @Schema(implementation = UserList.class)))
  })
  public ResultList<User> list(@Context UriInfo uriInfo,
                               @Context SecurityContext securityContext,
                               @Parameter(description = "Fields requested in the returned resource",
                                          schema = @Schema(type = "string", example = FIELDS))
                               @QueryParam("fields") String fieldsParam,
                               @Parameter(description = "Limit the number users returned. (1 to 1000000, default = 10) ",
                                          schema = @Schema(type = "string", example = "snowflakeWestCoast.financeDB"))
                               @DefaultValue("10")
                               @Min(1)
                               @Max(1000000)
                               @QueryParam("limit") int limitParam,
                               @Parameter(description = "Returns list of users before this cursor",
                                          schema = @Schema(type = "string"))
                               @QueryParam("before") String before,
                               @Parameter(description = "Returns list of users after this cursor",
                                          schema = @Schema(type = "string"))
                               @QueryParam("after") String after)
          throws IOException, GeneralSecurityException, ParseException {
    RestUtil.validateCursors(before, after);
    Fields fields = new Fields(FIELD_LIST, fieldsParam);

    ResultList<User> users;
    if (before != null) { // Reverse paging
      users = dao.listBefore(fields, null, limitParam, before);
    } else { // Forward paging or first page
      users = dao.listAfter(fields, null, limitParam, after);
    }
    Optional.ofNullable(users.getData()).orElse(Collections.emptyList()).forEach(u -> addHref(uriInfo, u));
    return users;
  }

  @GET
  @Path("/{id}/versions")
  @Operation(summary = "List user versions", tags = "users",
          description = "Get a list of all the versions of a user identified by `id`",
          responses = {@ApiResponse(responseCode = "200", description = "List of user versions",
                  content = @Content(mediaType = "application/json",
                          schema = @Schema(implementation = EntityHistory.class)))
          })
  public EntityHistory listVersions(@Context UriInfo uriInfo,
                                    @Context SecurityContext securityContext,
                                    @Parameter(description = "user Id", schema = @Schema(type = "string"))
                                    @PathParam("id") String id)
          throws IOException, ParseException, GeneralSecurityException {
    return dao.listVersions(id);
  }

  @GET
  @Valid
  @Path("/{id}")
  @Operation(summary = "Get a user", tags = "users",
          description = "Get a user by `id`",
          responses = {
                  @ApiResponse(responseCode = "200", description = "The user",
                          content = @Content(mediaType = "application/json",
                          schema = @Schema(implementation = User.class))),
                  @ApiResponse(responseCode = "404", description = "User for instance {id} is not found")
  })
  public User get(@Context UriInfo uriInfo, @Context SecurityContext securityContext, @PathParam("id") String id,
                  @Parameter(description = "Fields requested in the returned resource",
                          schema = @Schema(type = "string", example = FIELDS))
                  @QueryParam("fields") String fieldsParam) throws IOException, ParseException {
    Fields fields = new Fields(FIELD_LIST, fieldsParam);
    User user = dao.get(id, fields);
    return addHref(uriInfo, user);
  }

  @GET
  @Valid
  @Path("/name/{name}")
  @Operation(summary = "Get a user by name", tags = "users",
          description = "Get a user by `name`.",
          responses = {
                  @ApiResponse(responseCode = "200", description = "The user",
                          content = @Content(mediaType = "application/json",
                          schema = @Schema(implementation = User.class))),
                  @ApiResponse(responseCode = "404", description = "User for instance {id} is not found")
          })
  public User getByName(@Context UriInfo uriInfo, @Context SecurityContext securityContext,
                      @PathParam("name") String name,
                  @Parameter(description = "Fields requested in the returned resource",
                          schema = @Schema(type = "string", example = FIELDS))
                  @QueryParam("fields") String fieldsParam) throws IOException, ParseException {
    Fields fields = new Fields(FIELD_LIST, fieldsParam);
    User user = dao.getByName(name, fields);
    return addHref(uriInfo, user);
  }

  @GET
  @Valid
  @Path("/loggedInUser")
  @Operation(summary = "Get current logged in user", tags = "users",
          description = "Get the user who is authenticated and is currently logged in.",
          responses = {
                  @ApiResponse(responseCode = "200", description = "The user",
                          content = @Content(mediaType = "application/json",
                          schema = @Schema(implementation = User.class))),
                  @ApiResponse(responseCode = "404", description = "User not found")
          })
  public User getCurrentLoggedInUser(@Context UriInfo uriInfo, @Context SecurityContext securityContext,
                                     @Parameter(description = "Fields requested in the returned resource",
                                             schema = @Schema(type = "string", example = FIELDS))
                                     @QueryParam("fields") String fieldsParam) throws IOException, ParseException {
    Fields fields = new Fields(FIELD_LIST, fieldsParam);
    String currentUserName = securityContext.getUserPrincipal().getName();
    User user = dao.getByName(currentUserName, fields);
    return addHref(uriInfo, user);
  }

  @GET
  @Path("/{id}/versions/{version}")
  @Operation(summary = "Get a version of the user", tags = "users",
          description = "Get a version of the user by given `id`",
          responses = {
                  @ApiResponse(responseCode = "200", description = "user",
                          content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = User.class))),
                  @ApiResponse(responseCode = "404", description = "User for instance {id} and version {version} is " +
                          "not found")
          })
  public User getVersion(@Context UriInfo uriInfo,
                          @Context SecurityContext securityContext,
                          @Parameter(description = "User Id", schema = @Schema(type = "string"))
                          @PathParam("id") String id,
                          @Parameter(description = "User version number in the form `major`.`minor`",
                                  schema = @Schema(type = "string", example = "0.1 or 1.1"))
                          @PathParam("version") String version) throws IOException, ParseException {
    return dao.getVersion(id, version);
  }

  @POST
  @Operation(summary = "Create a user", tags = "users",
          description = "Create a new user.",
          responses = {
                  @ApiResponse(responseCode = "200", description = "The user ",
                          content = @Content(mediaType = "application/json",
                          schema = @Schema(implementation = CreateUser.class))),
                  @ApiResponse(responseCode = "400", description = "Bad request")
          })
  public Response createUser(@Context UriInfo uriInfo, @Context SecurityContext securityContext,
                             @Valid CreateUser create) throws IOException, ParseException {
    if (create.getIsAdmin() != null && create.getIsAdmin()) {
      SecurityUtil.checkAdminOrBotRole(authorizer, securityContext);
    }
    User user = getUser(securityContext, create);
    addHref(uriInfo, dao.create(user));
    return Response.created(user.getHref()).entity(user).build();
  }

  @PUT
  @Operation(summary = "Create or Update a user", tags = "users",
          description = "Create or Update a user.",
          responses = {
                  @ApiResponse(responseCode = "200", description = "The user ",
                          content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = CreateUser.class))),
                  @ApiResponse(responseCode = "400", description = "Bad request")
          })
  public Response createOrUpdateUser(@Context UriInfo uriInfo,
                                     @Context SecurityContext securityContext,
                                     @Valid CreateUser create) throws IOException, ParseException {
    if (create.getIsAdmin() != null && create.getIsAdmin()) {
      SecurityUtil.checkAdminOrBotRole(authorizer, securityContext);
    }
    User user = getUser(securityContext, create);
    SecurityUtil.checkAdminRoleOrPermissions(authorizer, securityContext,
            new UserEntityInterface(user).getEntityReference());
    RestUtil.PutResponse<User> response = dao.createOrUpdate(user);
    user = addHref(uriInfo, response.getEntity());
    return Response.status(response.getStatus()).entity(user).build();
  }

  @PATCH
  @Valid
  @Path("/{id}")
  @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
  @Operation(summary = "Update a user", tags = "users",
          description = "Update an existing user using JsonPatch.",
          externalDocs = @ExternalDocumentation(description = "JsonPatch RFC",
                  url = "https://tools.ietf.org/html/rfc6902"))
  public User patch(@Context UriInfo uriInfo, @Context SecurityContext securityContext, @PathParam("id") String id,
                    @RequestBody(description = "JsonPatch with array of operations",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON,
                                    examples = {@ExampleObject("[" +
                                            "{op:remove, path:/a}," +
                                            "{op:add, path: /b, value: val}" +
                                            "]")}))
                            JsonPatch patch) throws IOException, ParseException {
    User user = dao.get(id, new Fields(FIELD_LIST, null));
    SecurityUtil.checkAdminRoleOrPermissions(authorizer, securityContext,
            new UserEntityInterface(user).getEntityReference());
    return addHref(uriInfo, dao.patch(UUID.fromString(id), securityContext.getUserPrincipal().getName(), patch));
  }

  @DELETE
  @Path("/{id}")
  @Operation(summary = "Deactivate a user", tags = "users",
          description = "Users can't be deleted but are deactivated. The name and display name is prefixed with " +
                  "the string `deactivated`.",
          responses = {
                  @ApiResponse(responseCode = "200", description = "OK"),
                  @ApiResponse(responseCode = "404", description = "User for instance {id} is not found")
          })
  public Response delete(@Context UriInfo uriInfo, @Context SecurityContext securityContext,
                         @PathParam("id") String id) throws IOException {
    SecurityUtil.checkAdminOrBotRole(authorizer, securityContext);
    dao.delete(UUID.fromString(id));
    return Response.ok().build();
  }

  private User getUser(SecurityContext securityContext, CreateUser create) throws IOException {
    return new User().withId(UUID.randomUUID()).withName(create.getName()).withEmail(create.getEmail())
            .withDescription(create.getDescription()).withDisplayName(create.getDisplayName())
            .withIsBot(create.getIsBot()).withIsAdmin(create.getIsAdmin())
            .withProfile(create.getProfile()).withTimezone(create.getTimezone())
            .withUpdatedBy(securityContext.getUserPrincipal().getName())
            .withUpdatedAt(new Date()).withTeams(dao.validateTeams(create.getTeams()));
  }
}
