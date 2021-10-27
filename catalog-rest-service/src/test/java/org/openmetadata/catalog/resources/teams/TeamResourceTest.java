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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.http.client.HttpResponseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.openmetadata.catalog.CatalogApplicationTest;
import org.openmetadata.catalog.Entity;
import org.openmetadata.catalog.api.teams.CreateTeam;
import org.openmetadata.catalog.entity.teams.Team;
import org.openmetadata.catalog.entity.teams.User;
import org.openmetadata.catalog.exception.CatalogExceptionMessage;
import org.openmetadata.catalog.jdbi3.TeamRepository.TeamEntityInterface;
import org.openmetadata.catalog.jdbi3.UserRepository.UserEntityInterface;
import org.openmetadata.catalog.resources.EntityResourceTest;
import org.openmetadata.catalog.resources.databases.TableResourceTest;
import org.openmetadata.catalog.resources.teams.TeamResource.TeamList;
import org.openmetadata.catalog.type.ChangeDescription;
import org.openmetadata.catalog.type.EntityReference;
import org.openmetadata.catalog.type.ImageList;
import org.openmetadata.catalog.type.Profile;
import org.openmetadata.catalog.util.EntityInterface;
import org.openmetadata.catalog.util.JsonUtils;
import org.openmetadata.catalog.util.TestUtils;
import org.openmetadata.common.utils.JsonSchemaUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonPatch;
import javax.ws.rs.client.WebTarget;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openmetadata.catalog.exception.CatalogExceptionMessage.entityNotFound;
import static org.openmetadata.catalog.resources.teams.UserResourceTest.createUser;
import static org.openmetadata.catalog.util.TestUtils.UpdateType.MINOR_UPDATE;
import static org.openmetadata.catalog.util.TestUtils.adminAuthHeaders;
import static org.openmetadata.catalog.util.TestUtils.assertEntityPagination;
import static org.openmetadata.catalog.util.TestUtils.assertResponse;
import static org.openmetadata.catalog.util.TestUtils.authHeaders;
import static org.openmetadata.catalog.util.TestUtils.validateEntityReference;

public class TeamResourceTest extends EntityResourceTest<Team> {
  private static final Logger LOG = LoggerFactory.getLogger(TeamResourceTest.class);
  final Profile PROFILE = new Profile().withImages(new ImageList().withImage(URI.create("http://image.com")));

  public TeamResourceTest() {
    super(Team.class, "teams", TeamResource.FIELDS);
  }

  @Test
  public void post_teamWithLongName_400_badRequest(TestInfo test) {
    // Create team with mandatory name field empty
    HttpResponseException exception = assertThrows(HttpResponseException.class, () ->
                    createTeam(create(test).withName(TestUtils.LONG_ENTITY_NAME), adminAuthHeaders()));
    assertResponse(exception, BAD_REQUEST, "[name size must be between 1 and 64]");
  }

  @Test
  public void post_teamWithoutName_400_badRequest(TestInfo test) {
    // Create team with mandatory name field empty
    HttpResponseException exception = assertThrows(HttpResponseException.class, () ->
            createTeam(create(test).withName(""), adminAuthHeaders()));
    assertResponse(exception, BAD_REQUEST, "[name size must be between 1 and 64]");
  }

  @Test
  public void post_teamAlreadyExists_409_conflict(TestInfo test) throws HttpResponseException {
    CreateTeam create = create(test);
    createTeam(create, adminAuthHeaders());
    HttpResponseException exception = assertThrows(HttpResponseException.class, () ->
            createTeam(create, adminAuthHeaders()));
    assertResponse(exception, CONFLICT, CatalogExceptionMessage.ENTITY_ALREADY_EXISTS);
  }

  @Test
  public void post_validTeams_as_admin_200_OK(TestInfo test) throws HttpResponseException {
    // Create team with different optional fields
    CreateTeam create = create(test, 1);
    createAndCheckEntity(create, adminAuthHeaders());

    create = create(test, 2).withDisplayName("displayName");
    createAndCheckEntity(create, adminAuthHeaders());

    create = create(test, 3).withDescription("description");
    createAndCheckEntity(create, adminAuthHeaders());

    create = create(test, 4).withProfile(PROFILE);
    createAndCheckEntity(create, adminAuthHeaders());

    create = create(test, 5).withDisplayName("displayName").withDescription("description").withProfile(PROFILE);
    createAndCheckEntity(create, adminAuthHeaders());
  }

  @Test
  public void post_validTeams_as_non_admin_401(TestInfo test) {
    // Create team with different optional fields
    Map<String, String> authHeaders = authHeaders("test@open-metadata.org");
    CreateTeam create = create(test, 1);
    HttpResponseException exception = assertThrows(HttpResponseException.class, () -> createAndCheckEntity(create,
            authHeaders));
    assertResponse(exception, FORBIDDEN, "Principal: CatalogPrincipal{name='test'} is not admin");
  }

  @Test
  public void post_teamWithUsers_200_OK(TestInfo test) throws HttpResponseException {
    // Add team to user relationships while creating a team
    User user1 = createUser(UserResourceTest.create(test, 1),
            authHeaders("test@open-metadata.org"));
    User user2 = createUser(UserResourceTest.create(test, 2),
            authHeaders("test@open-metadata.org"));
    List<UUID> users = Arrays.asList(user1.getId(), user2.getId());
    CreateTeam create = create(test).withDisplayName("displayName").withDescription("description")
            .withProfile(PROFILE).withUsers(users);
    Team team = createAndCheckEntity(create, adminAuthHeaders());

    // Make sure the user entity has relationship to the team
    user1 = UserResourceTest.getUser(user1.getId(), "teams", authHeaders("test@open-metadata.org"));
    assertEquals(team.getId(), user1.getTeams().get(0).getId());
    user2 = UserResourceTest.getUser(user2.getId(), "teams", authHeaders("test@open-metadata.org"));
    assertEquals(team.getId(), user2.getTeams().get(0).getId());
  }

  @Test
  public void get_nonExistentTeam_404_notFound() {
    HttpResponseException exception = assertThrows(HttpResponseException.class, () ->
            getTeam(TestUtils.NON_EXISTENT_ENTITY, adminAuthHeaders()));
    assertResponse(exception, NOT_FOUND, entityNotFound("Team", TestUtils.NON_EXISTENT_ENTITY));
  }

  @Test
  public void get_teamWithDifferentFields_200_OK(TestInfo test) throws HttpResponseException {
    User user1 = createUser(UserResourceTest.create(test, 1),
            authHeaders("test@open-metadata.org"));
    List<UUID> users = Collections.singletonList(user1.getId());

    CreateTeam create = create(test).withDisplayName("displayName").withDescription("description")
            .withProfile(PROFILE).withUsers(users);
    Team team = createTeam(create, adminAuthHeaders());
    validateGetWithDifferentFields(team, false, adminAuthHeaders());
  }

  @Test
  public void get_teamByNameWithDifferentFields_200_OK(TestInfo test) throws HttpResponseException {
    User user1 = createUser(UserResourceTest.create(test), adminAuthHeaders());
    List<UUID> users = Collections.singletonList(user1.getId());

    CreateTeam create = create(test).withDisplayName("displayName").withDescription("description")
            .withProfile(PROFILE).withUsers(users);
    Team team = createTeam(create, adminAuthHeaders());
    validateGetWithDifferentFields(team, true, adminAuthHeaders());
  }

  @Test
  public void get_teamWithInvalidFields_400_BadRequest(TestInfo test) throws HttpResponseException {
    CreateTeam create = create(test);
    Team team = createTeam(create, adminAuthHeaders());

    // Empty query field .../teams?fields=
    HttpResponseException exception = assertThrows(HttpResponseException.class, () ->
            getTeam(team.getId(), "", adminAuthHeaders()));
    assertResponse(exception, BAD_REQUEST, CatalogExceptionMessage.invalidField(""));

    // .../teams?fields=invalidField
    exception = assertThrows(HttpResponseException.class, () ->
            getTeam(team.getId(), "invalidField", adminAuthHeaders()));
    assertResponse(exception, BAD_REQUEST, CatalogExceptionMessage.invalidField("invalidField"));
  }

  @Test
  public void get_teamListWithInvalidLimit_4xx() {
    // Limit must be >= 1 and <= 1000,000
    HttpResponseException exception = assertThrows(HttpResponseException.class, ()
            -> listTeams(null, -1, null, null, adminAuthHeaders()));
    assertResponse(exception, BAD_REQUEST, "[query param limit must be greater than or equal to 1]");

    exception = assertThrows(HttpResponseException.class, ()
            -> listTeams(null, 0, null, null, adminAuthHeaders()));
    assertResponse(exception, BAD_REQUEST, "[query param limit must be greater than or equal to 1]");

    exception = assertThrows(HttpResponseException.class, ()
            -> listTeams(null, 1000001, null, null, adminAuthHeaders()));
    assertResponse(exception, BAD_REQUEST, "[query param limit must be less than or equal to 1000000]");
  }

  @Test
  public void get_teamListWithInvalidPaginationCursors_4xx() {
    // Passing both before and after cursors is invalid
    HttpResponseException exception = assertThrows(HttpResponseException.class, ()
            -> listTeams(null, 1, "", "", adminAuthHeaders()));
    assertResponse(exception, BAD_REQUEST, "Only one of before or after query parameter allowed");
  }

  /**
   * For cursor based pagination and implementation details:
   * @see org.openmetadata.catalog.util.ResultList
   *
   * The tests and various CASES referenced are base on that.
   */
  @Test
  public void get_teamListWithPagination_200(TestInfo test) throws HttpResponseException {
    // Create a large number of teams
    int maxTeams = 40;
    for (int i = 0; i < maxTeams; i++) {
      createTeam(create(test, i), adminAuthHeaders());
    }

    // List all teams and use it for checking pagination
    TeamList allTeams = listTeams(null, 1000000, null, null, adminAuthHeaders());
    int totalRecords = allTeams.getData().size();
    printTeams(allTeams);

    // List tables with limit set from 1 to maxTables size
    // Each time comapare the returned list with allTables list to make sure right results are returned
    for (int limit = 1; limit < maxTeams; limit++) {
      String after = null;
      String before;
      int pageCount = 0;
      int indexInAllTables = 0;
      TeamList forwardPage;
      TeamList backwardPage;
      do { // For each limit (or page size) - forward scroll till the end
        LOG.info("Limit {} forward scrollCount {} afterCursor {}", limit, pageCount, after);
        forwardPage = listTeams(null, limit, null, after, adminAuthHeaders());
        after = forwardPage.getPaging().getAfter();
        before = forwardPage.getPaging().getBefore();
        assertEntityPagination(allTeams.getData(), forwardPage, limit, indexInAllTables);

        if (pageCount == 0) {  // CASE 0 - First page is being returned. There is no before cursor
          assertNull(before);
        } else {
          // Make sure scrolling back based on before cursor returns the correct result
          backwardPage = listTeams(null, limit, before, null, adminAuthHeaders());
          assertEntityPagination(allTeams.getData(), backwardPage, limit, (indexInAllTables - limit));
        }

        printTeams(forwardPage);
        indexInAllTables += forwardPage.getData().size();
        pageCount++;
      } while (after != null);

      // We have now reached the last page - test backward scroll till the beginning
      pageCount = 0;
      indexInAllTables = totalRecords - limit - forwardPage.getData().size();
      do {
        LOG.info("Limit {} backward scrollCount {} beforeCursor {}", limit, pageCount, before);
        forwardPage = listTeams(null, limit, before, null, adminAuthHeaders());
        printTeams(forwardPage);
        before = forwardPage.getPaging().getBefore();
        assertEntityPagination(allTeams.getData(), forwardPage, limit, indexInAllTables);
        pageCount++;
        indexInAllTables -= forwardPage.getData().size();
      } while (before != null);
    }
  }

  private void printTeams(TeamList list) {
    list.getData().forEach(team -> LOG.info("Team {}", team.getName()));
    LOG.info("before {} after {} ", list.getPaging().getBefore(), list.getPaging().getAfter());
  }
  /**
   * @see TableResourceTest#put_addDeleteFollower_200
   * for tests related getting team with entities owned by the team
   */

  @Test
  public void delete_validTeam_200_OK(TestInfo test) throws HttpResponseException {
    User user1 = createUser(UserResourceTest.create(test, 1), adminAuthHeaders());
    List<UUID> users = Collections.singletonList(user1.getId());
    CreateTeam create = create(test).withUsers(users);
    Team team = createAndCheckEntity(create, adminAuthHeaders());
    deleteTeam(team.getId(), adminAuthHeaders());

    // Make sure team is no longer there
    HttpResponseException exception = assertThrows(HttpResponseException.class, () ->
            getTeam(team.getId(), adminAuthHeaders()));
    assertResponse(exception, NOT_FOUND, entityNotFound("Team", team.getId()));

    // Make sure user does not have relationship to this team
    User user = UserResourceTest.getUser(user1.getId(), "teams", adminAuthHeaders());
    assertTrue(user.getTeams().isEmpty());
  }

  @Test
  public void delete_validTeam_as_non_admin_401(TestInfo test) throws HttpResponseException {
    User user1 = createUser(UserResourceTest.create(test, 1),
            authHeaders("test@open-metadata.org"));
    List<UUID> users = Collections.singletonList(user1.getId());
    CreateTeam create = create(test).withUsers(users);
    Team team = createAndCheckEntity(create, adminAuthHeaders());
    HttpResponseException exception = assertThrows(HttpResponseException.class, () ->
            deleteTeam(team.getId(), authHeaders("test@open-metadata.org")));
    assertResponse(exception, FORBIDDEN, "Principal: CatalogPrincipal{name='test'} is not admin");
  }


  @Test
  public void delete_nonExistentTeam_404_notFound() {
    HttpResponseException exception = assertThrows(HttpResponseException.class, () ->
            deleteTeam(TestUtils.NON_EXISTENT_ENTITY, adminAuthHeaders()));
    assertResponse(exception, NOT_FOUND, entityNotFound("Team", TestUtils.NON_EXISTENT_ENTITY));
  }

  @Test
  public void patch_teamDeletedDisallowed_400(TestInfo test) throws HttpResponseException, JsonProcessingException {
    // Ensure team deleted attribute can't be changed using patch
    Team team = createTeam(create(test), adminAuthHeaders());
    String teamJson = JsonUtils.pojoToJson(team);
    team.setDeleted(true);
    HttpResponseException exception = assertThrows(HttpResponseException.class, () ->
            patchTeam(teamJson, team, adminAuthHeaders()));
    assertResponse(exception, BAD_REQUEST, CatalogExceptionMessage.readOnlyAttribute("Team", "deleted"));
  }

  @Test
  public void patch_teamAttributes_as_admin_200_ok(TestInfo test)
          throws HttpResponseException, JsonProcessingException {
    // Create table without any attributes
    Team team = createTeam(create(test), adminAuthHeaders());
    assertNull(team.getDisplayName());
    assertNull(team.getDescription());
    assertNull(team.getProfile());
    assertNull(team.getDeleted());
    assertNull(team.getUsers());

    User user1 = createUser(UserResourceTest.create(test, 1), authHeaders("test@open-metadata.org"));
    User user2 = createUser(UserResourceTest.create(test, 2), authHeaders("test@open-metadata.org"));
    User user3 = createUser(UserResourceTest.create(test, 3), authHeaders("test@open-metadata.org"));

    List<EntityReference> users = Arrays.asList(new UserEntityInterface(user1).getEntityReference(),
            new UserEntityInterface(user2).getEntityReference());
    Profile profile = new Profile().withImages(new ImageList().withImage(URI.create("http://image.com")));

    //
    // Add previously absent attributes
    //
    String originalJson = JsonUtils.pojoToJson(team);
    team.withDisplayName("displayName").withDescription("description").withProfile(profile).withUsers(users);
    ChangeDescription change = getChangeDescription(team.getVersion())
            .withFieldsAdded(Arrays.asList("displayName", "description", "profile", "users"));
    team = patchEntityAndCheck(team, originalJson, adminAuthHeaders(), MINOR_UPDATE, change);
    team.getUsers().get(0).setHref(null);
    team.getUsers().get(1).setHref(null);

    //
    // Replace the attributes
    //
    users = Arrays.asList(new UserEntityInterface(user1).getEntityReference(),
            new UserEntityInterface(user3).getEntityReference()); // user2 dropped and user3 is added
    profile = new Profile().withImages(new ImageList().withImage(URI.create("http://image1.com")));

    originalJson = JsonUtils.pojoToJson(team);
    team.withDisplayName("displayName1").withDescription("description1").withProfile(profile).withUsers(users);
    change = getChangeDescription(team.getVersion())
            .withFieldsUpdated(Arrays.asList("displayName", "description", "profile", "users"));
    team = patchEntityAndCheck(team, originalJson, adminAuthHeaders(), MINOR_UPDATE, change);

    // Remove the attributes
    originalJson = JsonUtils.pojoToJson(team);
    team.withDisplayName(null).withDescription(null).withProfile(null).withUsers(null);
    change = getChangeDescription(team.getVersion())
            .withFieldsDeleted(Arrays.asList("displayName", "description", "profile", "users"));
    patchEntityAndCheck(team, originalJson, adminAuthHeaders(), MINOR_UPDATE, change);
  }

  @Test
  public void patch_teamAttributes_as_non_admin_403(TestInfo test) throws HttpResponseException,
          JsonProcessingException {
    // Create table without any attributes
    Team team = createTeam(create(test), adminAuthHeaders());
    // Patching as a non-admin should is disallowed
    String originalJson = JsonUtils.pojoToJson(team);
    team.setDisplayName("newDisplayName");
    HttpResponseException exception = assertThrows(HttpResponseException.class, () ->
            patchTeam(team.getId(), originalJson, team, authHeaders("test@open-metadata.org")));
    assertResponse(exception, FORBIDDEN, "Principal: CatalogPrincipal{name='test'} is not admin");
  }

  public static Team createTeam(CreateTeam create, Map<String, String> authHeaders) throws HttpResponseException {
    return TestUtils.post(CatalogApplicationTest.getResource("teams"), create, Team.class, authHeaders);
  }

  public static Team getTeam(UUID id, Map<String, String> authHeaders) throws HttpResponseException {
    return getTeam(id, null, authHeaders);
  }

  public static Team getTeam(UUID id, String fields, Map<String, String> authHeaders) throws HttpResponseException {
    WebTarget target = CatalogApplicationTest.getResource("teams/" + id);
    target = fields != null ? target.queryParam("fields", fields) : target;
    return TestUtils.get(target, Team.class, authHeaders);
  }

  public static TeamList listTeams(String fields, Integer limit, String before, String after,
                                   Map<String, String> authHeaders) throws HttpResponseException {
    WebTarget target = CatalogApplicationTest.getResource("teams");
    target = fields != null ? target.queryParam("fields", fields) : target;
    target = limit != null ? target.queryParam("limit", limit) : target;
    target = before != null ? target.queryParam("before", before) : target;
    target = after != null ? target.queryParam("after", after) : target;
    return TestUtils.get(target, TeamList.class, authHeaders);
  }


  public static Team getTeamByName(String name, String fields, Map<String, String> authHeaders)
          throws HttpResponseException {
    WebTarget target = CatalogApplicationTest.getResource("teams/name/" + name);
    target = fields != null ? target.queryParam("fields", fields) : target;
    return TestUtils.get(target, Team.class, authHeaders);
  }

  private static void validateTeam(Team team, String expectedDescription, String expectedDisplayName,
                                   Profile expectedProfile, List<EntityReference> expectedUsers,
                                   String expectedUpdatedBy) {
    assertNotNull(team.getId());
    assertNotNull(team.getHref());
    assertEquals(expectedDescription, team.getDescription());
    assertEquals(expectedUpdatedBy, team.getUpdatedBy());
    assertEquals(expectedDisplayName, team.getDisplayName());
    assertEquals(expectedProfile, team.getProfile());
    if (expectedUsers != null && !expectedUsers.isEmpty()) {
      assertEquals(expectedUsers.size(), team.getUsers().size());
      for (EntityReference user : team.getUsers()) {
        TestUtils.validateEntityReference(user);
        boolean foundUser = false;
        for (EntityReference expected : expectedUsers) {
          if (expected.getId().equals(user.getId())) {
            foundUser = true;
            break;
          }
        }
        assertTrue(foundUser);
      }
    }
    TestUtils.validateEntityReference(team.getOwns());
  }

  /** Validate returned fields GET .../teams/{id}?fields="..." or GET .../teams/name/{name}?fields="..." */
  private void validateGetWithDifferentFields(Team expectedTeam, boolean byName, Map<String, String> authHeaders)
          throws HttpResponseException {
    String updatedBy = TestUtils.getPrincipal(authHeaders);
    // .../teams?fields=profile
    String fields = "profile";
    Team getTeam = byName ? getTeamByName(expectedTeam.getName(), fields, authHeaders) : getTeam(expectedTeam.getId(), fields,
            authHeaders);
    validateTeam(getTeam, expectedTeam.getDescription(), expectedTeam.getDisplayName(), expectedTeam.getProfile(),
            null, updatedBy);
    assertNull(getTeam.getOwns());

    // .../teams?fields=users,owns
    fields = "users,owns,profile";
    getTeam = byName ? getTeamByName(expectedTeam.getName(), fields, authHeaders) : getTeam(expectedTeam.getId(), fields, authHeaders);
    assertNotNull(getTeam.getProfile());
    validateEntityReference(getTeam.getUsers());
    validateEntityReference(getTeam.getOwns());
  }

  private Team patchTeam(UUID teamId, String originalJson, Team updated, Map<String, String> authHeaders)
          throws JsonProcessingException, HttpResponseException {
    String updatedJson = JsonUtils.pojoToJson(updated);
    JsonPatch patch = JsonSchemaUtil.getJsonPatch(originalJson, updatedJson);
    return TestUtils.patch(CatalogApplicationTest.getResource("teams/" + teamId), patch,
            Team.class, authHeaders);
  }
  private Team patchTeam(String originalJson, Team updated, Map<String, String> authHeaders)
          throws JsonProcessingException, HttpResponseException {
    return patchTeam(updated.getId(), originalJson, updated, authHeaders);
  }

  public void deleteTeam(UUID id, Map<String, String> authHeaders) throws HttpResponseException {
    TestUtils.delete(CatalogApplicationTest.getResource("teams/" + id), authHeaders);
  }

  public static CreateTeam create(TestInfo test, int index) {
    return new CreateTeam().withName(getTeamName(test) + index);
  }

  public static CreateTeam create(TestInfo test) {
    return new CreateTeam().withName(getTeamName(test));
  }

  public static String getTeamName(TestInfo test) {
    return String.format("team_%s", test.getDisplayName());
  }

  @Override
  public Object createRequest(TestInfo test, String description, String displayName, EntityReference owner) {
    return create(test).withDescription(description).withDisplayName(displayName);
  }

  @Override
  public void validateCreatedEntity(Team team, Object request, Map<String, String> authHeaders) {
    CreateTeam createRequest = (CreateTeam) request;
    validateCommonEntityFields(getEntityInterface(team), createRequest.getDescription(),
            TestUtils.getPrincipal(authHeaders), null);

    assertEquals(createRequest.getDisplayName(), team.getDisplayName());
    assertEquals(createRequest.getProfile(), team.getProfile());

    List<EntityReference> expectedUsers = new ArrayList<>();
    for (UUID teamId : Optional.ofNullable(createRequest.getUsers()).orElse(Collections.emptyList())) {
      expectedUsers.add(new EntityReference().withId(teamId).withType(Entity.USER));
    }
    List<EntityReference> actualUsers = Optional.ofNullable(team.getUsers()).orElse(Collections.emptyList());
    if (!expectedUsers.isEmpty()) {
      assertEquals(expectedUsers.size(), actualUsers.size());
      for (EntityReference user : actualUsers) {
        TestUtils.validateEntityReference(user);
        boolean foundUser = false;
        for (EntityReference expectedEntity : expectedUsers) {
          if (expectedEntity.getId().equals(user.getId())) {
            foundUser = true;
            break;
          }
        }
        assertTrue(foundUser);
      }
    }
    TestUtils.validateEntityReference(team.getOwns());
  }

  @Override
  public void validateUpdatedEntity(Team updatedEntity, Object request, Map<String, String> authHeaders) {
    validateCreatedEntity(updatedEntity, request, authHeaders);
  }

  @Override
  public void validatePatchedEntity(Team expected, Team updated, Map<String, String> authHeaders) {
    validateCommonEntityFields(getEntityInterface(updated), expected.getDescription(),
            TestUtils.getPrincipal(authHeaders), null);

    assertEquals(expected.getDisplayName(), updated.getDisplayName());
    assertEquals(expected.getProfile(), updated.getProfile());

    List<EntityReference> expectedUsers = Optional.ofNullable(expected.getUsers()).orElse(Collections.emptyList());
    List<EntityReference> actualUsers = Optional.ofNullable(updated.getUsers()).orElse(Collections.emptyList());
    actualUsers.forEach(TestUtils::validateEntityReference);
    actualUsers.forEach(user -> user.setHref(null));

    actualUsers.sort(Comparator.comparing(EntityReference::getId));
    expectedUsers.sort(Comparator.comparing(EntityReference::getId));
    assertEquals(expectedUsers, actualUsers);
    TestUtils.validateEntityReference(updated.getOwns());
  }

  @Override
  public EntityInterface<Team> getEntityInterface(Team entity) {
    return new TeamEntityInterface(entity);
  }
}
