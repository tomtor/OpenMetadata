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

package org.openmetadata.catalog.resources.databases;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.http.client.HttpResponseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.openmetadata.catalog.Entity;
import org.openmetadata.catalog.api.data.CreateDatabase;
import org.openmetadata.catalog.api.services.CreateDatabaseService;
import org.openmetadata.catalog.api.services.CreateDatabaseService.DatabaseServiceType;
import org.openmetadata.catalog.entity.data.Database;
import org.openmetadata.catalog.entity.services.DatabaseService;
import org.openmetadata.catalog.exception.CatalogExceptionMessage;
import org.openmetadata.catalog.jdbi3.DatabaseRepository.DatabaseEntityInterface;
import org.openmetadata.catalog.jdbi3.DatabaseServiceRepository.DatabaseServiceEntityInterface;
import org.openmetadata.catalog.resources.EntityResourceTest;
import org.openmetadata.catalog.resources.databases.DatabaseResource.DatabaseList;
import org.openmetadata.catalog.resources.services.DatabaseServiceResourceTest;
import org.openmetadata.catalog.type.ChangeDescription;
import org.openmetadata.catalog.type.EntityReference;
import org.openmetadata.catalog.util.EntityInterface;
import org.openmetadata.catalog.util.JsonUtils;
import org.openmetadata.catalog.util.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.WebTarget;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.openmetadata.catalog.exception.CatalogExceptionMessage.entityNotFound;
import static org.openmetadata.catalog.util.TestUtils.UpdateType.MINOR_UPDATE;
import static org.openmetadata.catalog.util.TestUtils.adminAuthHeaders;
import static org.openmetadata.catalog.util.TestUtils.assertEntityPagination;
import static org.openmetadata.catalog.util.TestUtils.assertResponse;
import static org.openmetadata.catalog.util.TestUtils.authHeaders;

public class DatabaseResourceTest extends EntityResourceTest<Database> {
  private static final Logger LOG = LoggerFactory.getLogger(DatabaseResourceTest.class);
  public static EntityReference SNOWFLAKE_REFERENCE;
  public static EntityReference REDSHIFT_REFERENCE;
  public static EntityReference MYSQL_REFERENCE;
  public static EntityReference BIGQUERY_REFERENCE;

  public DatabaseResourceTest() {
    super(Database.class, "databases", DatabaseResource.FIELDS);
  }

  @BeforeAll
  public static void setup(TestInfo test) throws HttpResponseException, URISyntaxException {
    EntityResourceTest.setup(test);

    CreateDatabaseService createService = new CreateDatabaseService().withName("snowflakeDB")
            .withServiceType(DatabaseServiceType.Snowflake).withJdbc(TestUtils.JDBC_INFO);
    DatabaseService service = DatabaseServiceResourceTest.createService(createService, adminAuthHeaders());
    SNOWFLAKE_REFERENCE = new DatabaseServiceEntityInterface(service).getEntityReference();

    createService.withName("redshiftDB").withServiceType(DatabaseServiceType.Redshift);
    service = DatabaseServiceResourceTest.createService(createService, adminAuthHeaders());
    REDSHIFT_REFERENCE = new DatabaseServiceEntityInterface(service).getEntityReference();

    createService.withName("bigQueryDB").withServiceType(DatabaseServiceType.BigQuery);
    service = DatabaseServiceResourceTest.createService(createService, adminAuthHeaders());
    BIGQUERY_REFERENCE = new DatabaseServiceEntityInterface(service).getEntityReference();

    createService.withName("mysqlDB").withServiceType(DatabaseServiceType.MySQL);
    service = DatabaseServiceResourceTest.createService(createService, adminAuthHeaders());
    MYSQL_REFERENCE = new DatabaseServiceEntityInterface(service).getEntityReference();
  }

  @Test
  public void post_databaseWithLongName_400_badRequest(TestInfo test) {
    // Create database with mandatory name field empty
    CreateDatabase create = create(test).withName(TestUtils.LONG_ENTITY_NAME);
    assertResponse(() -> createDatabase(create, adminAuthHeaders()), BAD_REQUEST,
            "[name size must be between 1 and 64]");
  }

  @Test
  public void post_databaseWithoutName_400_badRequest(TestInfo test) {
    // Create database with mandatory name field empty
    CreateDatabase create = create(test).withName("");
    HttpResponseException exception = assertThrows(HttpResponseException.class, () ->
            createDatabase(create, adminAuthHeaders()));
    assertResponse(exception, BAD_REQUEST, "[name size must be between 1 and 64]");
  }

  @Test
  public void post_databaseAlreadyExists_409_conflict(TestInfo test) throws HttpResponseException {
    CreateDatabase create = create(test);
    createDatabase(create, adminAuthHeaders());
    HttpResponseException exception = assertThrows(HttpResponseException.class, () ->
            createDatabase(create, adminAuthHeaders()));
    assertResponse(exception, CONFLICT, CatalogExceptionMessage.ENTITY_ALREADY_EXISTS);
  }

  @Test
  public void post_validDatabases_as_admin_200_OK(TestInfo test) throws HttpResponseException {
    // Create team with different optional fields
    CreateDatabase create = create(test);
    createAndCheckEntity(create, adminAuthHeaders());

    create.withName(getDatabaseName(test, 1)).withDescription("description");
    createAndCheckEntity(create, adminAuthHeaders());
  }

  @Test
  public void post_databaseFQN_as_admin_200_OK(TestInfo test) throws HttpResponseException {
    // Create team with different optional fields
    CreateDatabase create = create(test);
    create.setService(new EntityReference().withId(SNOWFLAKE_REFERENCE.getId()).withType("databaseService"));
    Database db = createAndCheckEntity(create, adminAuthHeaders());
    String expectedFQN = SNOWFLAKE_REFERENCE.getName()+"."+create.getName();
    assertEquals(expectedFQN, db.getFullyQualifiedName());

  }

  @Test
  public void post_databaseWithUserOwner_200_ok(TestInfo test) throws HttpResponseException {
    createAndCheckEntity(create(test).withOwner(USER_OWNER1), adminAuthHeaders());
  }

  @Test
  public void post_databaseWithTeamOwner_200_ok(TestInfo test) throws HttpResponseException {
    createAndCheckEntity(create(test).withOwner(TEAM_OWNER1), adminAuthHeaders());
  }

  @Test
  public void post_database_as_non_admin_401(TestInfo test) {
    CreateDatabase create = create(test);
    HttpResponseException exception = assertThrows(HttpResponseException.class, () ->
            createDatabase(create, authHeaders("test@open-metadata.org")));
    assertResponse(exception, FORBIDDEN, "Principal: CatalogPrincipal{name='test'} is not admin");
  }

  @Test
  public void post_databaseWithoutRequiredService_4xx(TestInfo test) {
    CreateDatabase create = create(test).withService(null);
    HttpResponseException exception = assertThrows(HttpResponseException.class, () ->
            createDatabase(create, adminAuthHeaders()));
    TestUtils.assertResponseContains(exception, BAD_REQUEST, "service must not be null");
  }

  @Test
  public void post_databaseWithInvalidOwnerType_4xx(TestInfo test) {
    EntityReference owner = new EntityReference().withId(TEAM1.getId()); /* No owner type is set */

    CreateDatabase create = create(test).withOwner(owner);
    HttpResponseException exception = assertThrows(HttpResponseException.class, () ->
            createDatabase(create, adminAuthHeaders()));
    TestUtils.assertResponseContains(exception, BAD_REQUEST, "type must not be null");
  }

  @Test
  public void post_databaseWithNonExistentOwner_4xx(TestInfo test) {
    EntityReference owner = new EntityReference().withId(TestUtils.NON_EXISTENT_ENTITY).withType("user");
    CreateDatabase create = create(test).withOwner(owner);
    HttpResponseException exception = assertThrows(HttpResponseException.class, () ->
            createDatabase(create, adminAuthHeaders()));
    assertResponse(exception, NOT_FOUND, entityNotFound("User", TestUtils.NON_EXISTENT_ENTITY));
  }

  @Test
  public void post_databaseWithDifferentService_200_ok(TestInfo test) throws HttpResponseException {
    EntityReference[] differentServices = {MYSQL_REFERENCE, REDSHIFT_REFERENCE, BIGQUERY_REFERENCE,
            SNOWFLAKE_REFERENCE};

    // Create database for each service and test APIs
    for (EntityReference service : differentServices) {
      createAndCheckEntity(create(test).withService(service), adminAuthHeaders());

      // List databases by filtering on service name and ensure right databases are returned in the response
      DatabaseList list = listDatabases("service", service.getName(), adminAuthHeaders());
      for (Database db : list.getData()) {
        assertEquals(service.getName(), db.getService().getName());
      }
    }
  }

  @Test
  public void get_databaseListWithInvalidLimitOffset_4xx() {
    // Limit must be >= 1 and <= 1000,000
    HttpResponseException exception = assertThrows(HttpResponseException.class, ()
            -> listDatabases(null, null, -1, null, null, adminAuthHeaders()));
    assertResponse(exception, BAD_REQUEST, "[query param limit must be greater than or equal to 1]");

    exception = assertThrows(HttpResponseException.class, ()
            -> listDatabases(null, null, 0, null, null, adminAuthHeaders()));
    assertResponse(exception, BAD_REQUEST, "[query param limit must be greater than or equal to 1]");

    exception = assertThrows(HttpResponseException.class, ()
            -> listDatabases(null, null, 1000001, null, null, adminAuthHeaders()));
    assertResponse(exception, BAD_REQUEST, "[query param limit must be less than or equal to 1000000]");
  }

  @Test
  public void get_databaseListWithInvalidPaginationCursors_4xx() {
    // Passing both before and after cursors is invalid
    HttpResponseException exception = assertThrows(HttpResponseException.class, ()
            -> listDatabases(null, null, 1, "", "", adminAuthHeaders()));
    assertResponse(exception, BAD_REQUEST, "Only one of before or after query parameter allowed");
  }

  @Order(1)
  @Test
  public void get_databaseListWithValidLimitOffset_4xx(TestInfo test) throws HttpResponseException {
    // Create a large number of databases
    int maxDatabases = 40;
    for (int i = 0; i < maxDatabases; i++) {
      createDatabase(create(test, i), adminAuthHeaders());
    }

    // List all databases
    DatabaseList allDatabases = listDatabases(null, null, 1000000, null,
            null, adminAuthHeaders());
    int totalRecords = allDatabases.getData().size();
    printDatabases(allDatabases);

    // List limit number databases at a time at various offsets and ensure right results are returned
    for (int limit = 1; limit < maxDatabases; limit++) {
      String after = null;
      String before;
      int pageCount = 0;
      int indexInAllDatabases = 0;
      DatabaseList forwardPage;
      DatabaseList backwardPage;
      do { // For each limit (or page size) - forward scroll till the end
        LOG.info("Limit {} forward scrollCount {} afterCursor {}", limit, pageCount, after);
        forwardPage = listDatabases(null, null, limit, null, after, adminAuthHeaders());
        printDatabases(forwardPage);
        after = forwardPage.getPaging().getAfter();
        before = forwardPage.getPaging().getBefore();
        assertEntityPagination(allDatabases.getData(), forwardPage, limit, indexInAllDatabases);

        if (pageCount == 0) {  // CASE 0 - First page is being returned. There is no before cursor
          assertNull(before);
        } else {
          // Make sure scrolling back based on before cursor returns the correct result
          backwardPage = listDatabases(null, null, limit, before, null, adminAuthHeaders());
          assertEntityPagination(allDatabases.getData(), backwardPage, limit, (indexInAllDatabases - limit));
        }

        indexInAllDatabases += forwardPage.getData().size();
        pageCount++;
      } while (after != null);

      // We have now reached the last page - test backward scroll till the beginning
      pageCount = 0;
      indexInAllDatabases = totalRecords - limit - forwardPage.getData().size();
      do {
        LOG.info("Limit {} backward scrollCount {} beforeCursor {}", limit, pageCount, before);
        forwardPage = listDatabases(null, null, limit, before, null, adminAuthHeaders());
        printDatabases(forwardPage);
        before = forwardPage.getPaging().getBefore();
        assertEntityPagination(allDatabases.getData(), forwardPage, limit, indexInAllDatabases);
        pageCount++;
        indexInAllDatabases -= forwardPage.getData().size();
      } while (before != null);
    }
  }

  private void printDatabases(DatabaseList list) {
    list.getData().forEach(database -> LOG.info("DB {}", database.getFullyQualifiedName()));
    LOG.info("before {} after {} ", list.getPaging().getBefore(), list.getPaging().getAfter());
  }

  @Test
  public void get_nonExistentDatabase_404_notFound() {
    HttpResponseException exception = assertThrows(HttpResponseException.class, () ->
            getDatabase(TestUtils.NON_EXISTENT_ENTITY, adminAuthHeaders()));
    assertResponse(exception, NOT_FOUND,
            entityNotFound(Entity.DATABASE, TestUtils.NON_EXISTENT_ENTITY));
  }

  @Test
  public void get_databaseWithDifferentFields_200_OK(TestInfo test) throws HttpResponseException {
    CreateDatabase create = create(test).withDescription("description").withOwner(USER_OWNER1)
            .withService(SNOWFLAKE_REFERENCE);
    Database database = createAndCheckEntity(create, adminAuthHeaders());
    validateGetWithDifferentFields(database, false);
  }

  @Test
  public void get_databaseByNameWithDifferentFields_200_OK(TestInfo test) throws HttpResponseException {
    CreateDatabase create = create(test).withDescription("description").withOwner(USER_OWNER1)
            .withService(SNOWFLAKE_REFERENCE);
    Database database = createAndCheckEntity(create, adminAuthHeaders());
    validateGetWithDifferentFields(database, true);
  }

  @Test
  public void patch_databaseAttributes_200_ok(TestInfo test) throws HttpResponseException, JsonProcessingException {
    // Create database without description, owner
    Database database = createDatabase(create(test), adminAuthHeaders());
    assertNull(database.getDescription());
    assertNull(database.getOwner());
    assertNotNull(database.getService());

    database = getDatabase(database.getId(), "service,owner,usageSummary", adminAuthHeaders());
    database.getService().setHref(null); // href is readonly and not patchable

    //
    // Add description, owner when previously they were null
    //
    String origJson = JsonUtils.pojoToJson(database);
    database.withDescription("description").withOwner(TEAM_OWNER1);
    ChangeDescription change = getChangeDescription(database.getVersion())
            .withFieldsAdded(Arrays.asList("description", "owner"));
    database = patchEntityAndCheck(database, origJson, adminAuthHeaders(), MINOR_UPDATE, change);
    database.setOwner(TEAM_OWNER1); // Get rid of href and name returned in the response for owner
    database.setService(SNOWFLAKE_REFERENCE); // Get rid of href and name returned in the response for service

    //
    // Replace description, tier, owner
    //
    origJson = JsonUtils.pojoToJson(database);
    database.withDescription("description1").withOwner(USER_OWNER1);
    change = getChangeDescription(database.getVersion()).withFieldsUpdated(Arrays.asList("description", "owner"));
    database = patchEntityAndCheck(database, origJson, adminAuthHeaders(), MINOR_UPDATE, change);
    database.setOwner(USER_OWNER1); // Get rid of href and name returned in the response for owner
    database.setService(SNOWFLAKE_REFERENCE); // Get rid of href and name returned in the response for service

    // Remove description, tier, owner
    origJson = JsonUtils.pojoToJson(database);
    database.withDescription(null).withOwner(null);
    change = getChangeDescription(database.getVersion()).withFieldsDeleted(Arrays.asList("description", "owner"));
    patchEntityAndCheck(database, origJson, adminAuthHeaders(), MINOR_UPDATE, change);
  }

  // TODO listing tables test:1
  // TODO Change service?

  @Test
  public void delete_emptyDatabase_200_ok(TestInfo test) throws HttpResponseException {
    Database database = createDatabase(create(test), adminAuthHeaders());
    deleteDatabase(database.getId(), adminAuthHeaders());
  }

  @Test
  public void delete_nonEmptyDatabase_4xx() {
    // TODO
  }

  @Test
  public void delete_nonExistentDatabase_404() {
    HttpResponseException exception = assertThrows(HttpResponseException.class, () ->
            deleteDatabase(TestUtils.NON_EXISTENT_ENTITY, adminAuthHeaders()));
    assertResponse(exception, NOT_FOUND, entityNotFound(Entity.DATABASE, TestUtils.NON_EXISTENT_ENTITY));
  }

  public static Database createAndCheckDatabase(CreateDatabase create,
                                                Map<String, String> authHeaders) throws HttpResponseException {
    return new DatabaseResourceTest().createAndCheckEntity(create, authHeaders);
  }

  public static Database createDatabase(CreateDatabase create,
                                        Map<String, String> authHeaders) throws HttpResponseException {
    return TestUtils.post(getResource("databases"), create, Database.class, authHeaders);
  }

  /** Validate returned fields GET .../databases/{id}?fields="..." or GET .../databases/name/{fqn}?fields="..." */
  private void validateGetWithDifferentFields(Database database, boolean byName) throws HttpResponseException {
    // .../databases?fields=owner
    String fields = "owner";
    database = byName ? getDatabaseByName(database.getFullyQualifiedName(), fields, adminAuthHeaders()) :
            getDatabase(database.getId(), fields, adminAuthHeaders());
    assertNotNull(database.getOwner());
    assertNotNull(database.getService()); // We always return the service
    assertNull(database.getTables());

    // .../databases?fields=owner,service
    fields = "owner,service";
    database = byName ? getDatabaseByName(database.getFullyQualifiedName(), fields, adminAuthHeaders()) :
            getDatabase(database.getId(), fields, adminAuthHeaders());
    assertNotNull(database.getOwner());
    assertNotNull(database.getService());
    assertNull(database.getTables());

    // .../databases?fields=owner,service,tables
    fields = "owner,service,tables,usageSummary";
    database = byName ? getDatabaseByName(database.getFullyQualifiedName(), fields, adminAuthHeaders()) :
            getDatabase(database.getId(), fields, adminAuthHeaders());
    assertNotNull(database.getOwner());
    assertNotNull(database.getService());
    assertNotNull(database.getTables());
    TestUtils.validateEntityReference(database.getTables());
    assertNotNull(database.getUsageSummary());

  }

  public static void getDatabase(UUID id, Map<String, String> authHeaders) throws HttpResponseException {
    getDatabase(id, null, authHeaders);
  }

  public static Database getDatabase(UUID id, String fields, Map<String, String> authHeaders)
          throws HttpResponseException {
    WebTarget target = getResource("databases/" + id);
    target = fields != null ? target.queryParam("fields", fields): target;
    return TestUtils.get(target, Database.class, authHeaders);
  }

  public static Database getDatabaseByName(String fqn, String fields, Map<String, String> authHeaders)
          throws HttpResponseException {
    WebTarget target = getResource("databases/name/" + fqn);
    target = fields != null ? target.queryParam("fields", fields): target;
    return TestUtils.get(target, Database.class, authHeaders);
  }

  public static DatabaseList listDatabases(String fields, String serviceParam, Map<String, String> authHeaders)
          throws HttpResponseException {
    return listDatabases(fields, serviceParam, null, null, null, authHeaders);
  }

  public static DatabaseList listDatabases(String fields, String serviceParam, Integer limitParam,
                                           String before, String after, Map<String, String> authHeaders)
          throws HttpResponseException {
    WebTarget target = getResource("databases");
    target = fields != null ? target.queryParam("fields", fields): target;
    target = serviceParam != null ? target.queryParam("service", serviceParam): target;
    target = limitParam != null ? target.queryParam("limit", limitParam): target;
    target = before != null ? target.queryParam("before", before) : target;
    target = after != null ? target.queryParam("after", after) : target;
    return TestUtils.get(target, DatabaseList.class, authHeaders);
  }

  private void deleteDatabase(UUID id, Map<String, String> authHeaders) throws HttpResponseException {
    TestUtils.delete(getResource("databases/" + id), authHeaders);

    // Ensure deleted database does not exist
    HttpResponseException exception = assertThrows(HttpResponseException.class, () -> getDatabase(id, authHeaders));
    assertResponse(exception, NOT_FOUND, entityNotFound(Entity.DATABASE, id));
  }

  public static String getDatabaseName(TestInfo test) {
    return String.format("database_%s", test.getDisplayName());
  }

  public static String getDatabaseName(TestInfo test, int index) {
    return String.format("database%d_%s", index, test.getDisplayName());
  }

  public static CreateDatabase create(TestInfo test) {
    return new CreateDatabase().withName(getDatabaseName(test)).withService(SNOWFLAKE_REFERENCE);
  }

  public static CreateDatabase create(TestInfo test, int index) {
    return new CreateDatabase().withName(getDatabaseName(test, index)).withService(SNOWFLAKE_REFERENCE);
  }

  @Override
  public Object createRequest(TestInfo test, String description, String displayName, EntityReference owner) {
    return create(test).withDescription(description).withOwner(owner);
  }

  @Override
  public void validateCreatedEntity(Database createdEntity, Object request, Map<String, String> authHeaders) {
    CreateDatabase createRequest = (CreateDatabase) request;
    validateCommonEntityFields(getEntityInterface(createdEntity), createRequest.getDescription(),
            TestUtils.getPrincipal(authHeaders), createRequest.getOwner());

    // Validate service
    assertService(createRequest.getService(), createdEntity.getService());
  }

  @Override
  public void validateUpdatedEntity(Database updatedEntity, Object request, Map<String, String> authHeaders) {
    validateCreatedEntity(updatedEntity, request, authHeaders);
  }

  @Override
  public void validatePatchedEntity(Database expected, Database updated, Map<String, String> authHeaders) {
    validateCommonEntityFields(getEntityInterface(updated), expected.getDescription(),
            TestUtils.getPrincipal(authHeaders), expected.getOwner());
    // Validate service
    assertService(expected.getService(), updated.getService());
  }

  @Override
  public EntityInterface<Database> getEntityInterface(Database entity) {
    return new DatabaseEntityInterface(entity);
  }
}
