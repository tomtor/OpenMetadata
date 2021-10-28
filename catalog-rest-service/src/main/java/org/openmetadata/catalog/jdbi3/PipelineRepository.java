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

package org.openmetadata.catalog.jdbi3;

import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.openmetadata.catalog.Entity;
import org.openmetadata.catalog.entity.data.Pipeline;
import org.openmetadata.catalog.entity.services.PipelineService;
import org.openmetadata.catalog.exception.EntityNotFoundException;
import org.openmetadata.catalog.resources.pipelines.PipelineResource;
import org.openmetadata.catalog.type.ChangeDescription;
import org.openmetadata.catalog.type.EntityReference;
import org.openmetadata.catalog.type.TagLabel;
import org.openmetadata.catalog.util.EntityInterface;
import org.openmetadata.catalog.util.EntityUtil;
import org.openmetadata.catalog.util.EntityUtil.Fields;
import org.openmetadata.catalog.util.JsonUtils;

import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.openmetadata.catalog.exception.CatalogExceptionMessage.entityNotFound;

public class PipelineRepository extends EntityRepository<Pipeline> {
  private static final Fields PIPELINE_UPDATE_FIELDS = new Fields(PipelineResource.FIELD_LIST,
          "owner,service,tags,tasks");
  private static final Fields PIPELINE_PATCH_FIELDS = new Fields(PipelineResource.FIELD_LIST,
          "owner,service,tags,tasks");
  private final CollectionDAO dao;

  public PipelineRepository(CollectionDAO dao) {
    super(Pipeline.class, dao.pipelineDAO(), dao, PIPELINE_PATCH_FIELDS, PIPELINE_UPDATE_FIELDS);
    this.dao = dao;
  }

  public static String getFQN(Pipeline pipeline) {
    return (pipeline.getService().getName() + "." + pipeline.getName());
  }

  @Transaction
  public Status addFollower(UUID pipelineId, UUID userId) throws IOException {
    dao.pipelineDAO().findEntityById(pipelineId);
    return EntityUtil.addFollower(dao.relationshipDAO(), dao.userDAO(), pipelineId, Entity.PIPELINE, userId,
            Entity.USER) ?
            Status.CREATED : Status.OK;
  }

  @Transaction
  public void deleteFollower(UUID pipelineId, UUID userId) {
    EntityUtil.validateUser(dao.userDAO(), userId);
    EntityUtil.removeFollower(dao.relationshipDAO(), pipelineId, userId);
  }

  @Transaction
  public void delete(UUID id) {
    if (dao.relationshipDAO().findToCount(id.toString(), Relationship.CONTAINS.ordinal(), Entity.PIPELINE) > 0) {
      throw new IllegalArgumentException("Pipeline is not empty");
    }
    if (dao.pipelineDAO().delete(id) <= 0) {
      throw EntityNotFoundException.byMessage(entityNotFound(Entity.PIPELINE, id));
    }
    dao.relationshipDAO().deleteAll(id.toString());
  }

  @Transaction
  public EntityReference getOwnerReference(Pipeline pipeline) throws IOException {
    return EntityUtil.populateOwner(dao.userDAO(), dao.teamDAO(), pipeline.getOwner());
  }

  @Override
  public Pipeline setFields(Pipeline pipeline, Fields fields) throws IOException {
    pipeline.setDisplayName(pipeline.getDisplayName());
    pipeline.setService(getService(pipeline));
    pipeline.setPipelineUrl(pipeline.getPipelineUrl());
    pipeline.setStartDate(pipeline.getStartDate());
    pipeline.setConcurrency(pipeline.getConcurrency());
    pipeline.setOwner(fields.contains("owner") ? getOwner(pipeline) : null);
    pipeline.setFollowers(fields.contains("followers") ? getFollowers(pipeline) : null);
    pipeline.setTasks(fields.contains("tasks") ? getTasks(pipeline) : null);
    pipeline.setTags(fields.contains("tags") ? getTags(pipeline.getFullyQualifiedName()) : null);
    return pipeline;
  }

  @Override
  public void restorePatchAttributes(Pipeline original, Pipeline updated) throws IOException, ParseException {
    // Patch can't make changes to following fields. Ignore the changes
    updated.withFullyQualifiedName(original.getFullyQualifiedName()).withName(original.getName())
            .withService(original.getService()).withId(original.getId());
  }

  @Override
  public EntityInterface<Pipeline> getEntityInterface(Pipeline entity) {
    return new PipelineEntityInterface(entity);
  }

  private List<TagLabel> getTags(String fqn) {
    return dao.tagDAO().getTags(fqn);
  }


  @Override
  public void validate(Pipeline pipeline) throws IOException {
    pipeline.setService(getService(pipeline.getService()));
    pipeline.setFullyQualifiedName(getFQN(pipeline));
    EntityUtil.populateOwner(dao.userDAO(), dao.teamDAO(), pipeline.getOwner()); // Validate owner
    getService(pipeline.getService());
    pipeline.setTags(EntityUtil.addDerivedTags(dao.tagDAO(), pipeline.getTags()));
  }

  @Override
  public void store(Pipeline pipeline, boolean update) throws IOException {
    // Relationships and fields such as href are derived and not stored as part of json
    EntityReference owner = pipeline.getOwner();
    List<TagLabel> tags = pipeline.getTags();
    EntityReference service = pipeline.getService();
    List<EntityReference> tasks = pipeline.getTasks();

    // Don't store owner, database, href and tags as JSON. Build it on the fly based on relationships
    pipeline.withOwner(null).withService(null).withTasks(null).withHref(null).withTags(null);

    if (update) {
      dao.pipelineDAO().update(pipeline.getId(), JsonUtils.pojoToJson(pipeline));
    } else {
      dao.pipelineDAO().insert(pipeline);
    }

    // Restore the relationships
    pipeline.withOwner(owner).withService(service).withTasks(tasks).withTags(tags);
  }

  @Override
  public void storeRelationships(Pipeline pipeline) throws IOException {
    EntityReference service = pipeline.getService();
    dao.relationshipDAO().insert(service.getId().toString(), pipeline.getId().toString(), service.getType(),
            Entity.PIPELINE, Relationship.CONTAINS.ordinal());

    // Add relationship from pipeline to task
    String pipelineId = pipeline.getId().toString();
    if (pipeline.getTasks() != null) {
      for (EntityReference task : pipeline.getTasks()) {
        dao.relationshipDAO().insert(pipelineId, task.getId().toString(), Entity.PIPELINE, Entity.TASK,
                Relationship.CONTAINS.ordinal());
      }
    }
    // Add owner relationship
    EntityUtil.setOwner(dao.relationshipDAO(), pipeline.getId(), Entity.PIPELINE, pipeline.getOwner());

    // Add tag to pipeline relationship
    applyTags(pipeline);
  }

  @Override
  public EntityUpdater getUpdater(Pipeline original, Pipeline updated, boolean patchOperation) throws IOException {
    return new PipelineUpdater(original, updated, patchOperation);
  }

  private EntityReference getService(Pipeline pipeline) throws IOException {
    EntityReference ref = EntityUtil.getService(dao.relationshipDAO(), pipeline.getId(),
            Entity.PIPELINE_SERVICE);
    return getService(ref);
  }

  private EntityReference getService(EntityReference service) throws IOException {
    if (service.getType().equalsIgnoreCase(Entity.PIPELINE_SERVICE)) {
      PipelineService serviceInstance = dao.pipelineServiceDAO().findEntityById(service.getId());
      service.setDescription(serviceInstance.getDescription());
      service.setName(serviceInstance.getName());
    } else {
      throw new IllegalArgumentException(String.format("Invalid service type %s for the pipeline", service.getType()));
    }
    return service;
  }

  private EntityReference getOwner(Pipeline pipeline) throws IOException {
    return pipeline == null ? null : EntityUtil.populateOwner(pipeline.getId(), dao.relationshipDAO(),
            dao.userDAO(), dao.teamDAO());
  }

  public void setOwner(Pipeline pipeline, EntityReference owner) {
    EntityUtil.setOwner(dao.relationshipDAO(), pipeline.getId(), Entity.PIPELINE, owner);
    pipeline.setOwner(owner);
  }

  private void applyTags(Pipeline pipeline) throws IOException {
    // Add pipeline level tags by adding tag to pipeline relationship
    EntityUtil.applyTags(dao.tagDAO(), pipeline.getTags(), pipeline.getFullyQualifiedName());
    pipeline.setTags(getTags(pipeline.getFullyQualifiedName())); // Update tag to handle additional derived tags
  }

  private List<EntityReference> getFollowers(Pipeline pipeline) throws IOException {
    return pipeline == null ? null : EntityUtil.getFollowers(pipeline.getId(), dao.relationshipDAO(), dao.userDAO());
  }

  private List<EntityReference> getTasks(Pipeline pipeline) throws IOException {
    if (pipeline == null) {
      return null;
    }
    String pipelineId = pipeline.getId().toString();
    List<String> taskIds = dao.relationshipDAO().findTo(pipelineId, Relationship.CONTAINS.ordinal(), Entity.TASK);
    List<EntityReference> tasks = new ArrayList<>();
    for (String taskId : taskIds) {
      tasks.add(dao.taskDAO().findEntityReferenceById(UUID.fromString(taskId)));
    }
    return tasks;
  }

  private void updateTaskRelationships(Pipeline pipeline) {
    String pipelineId = pipeline.getId().toString();

    // Add relationship from pipeline to task
    if (pipeline.getTasks() != null) {
      // Remove any existing tasks associated with this pipeline
      dao.relationshipDAO().deleteFrom(pipelineId, Relationship.CONTAINS.ordinal(), Entity.TASK);
      for (EntityReference task : pipeline.getTasks()) {
        dao.relationshipDAO().insert(pipelineId, task.getId().toString(), Entity.PIPELINE, Entity.TASK,
                Relationship.CONTAINS.ordinal());
      }
    }
  }

  static class PipelineEntityInterface implements EntityInterface<Pipeline> {
    private final Pipeline entity;

    PipelineEntityInterface(Pipeline entity) {
      this.entity = entity;
    }

    @Override
    public UUID getId() {
      return entity.getId();
    }

    @Override
    public String getDescription() {
      return entity.getDescription();
    }

    @Override
    public String getDisplayName() {
      return entity.getDisplayName();
    }

    @Override
    public EntityReference getOwner() {
      return entity.getOwner();
    }

    @Override
    public String getFullyQualifiedName() {
      return entity.getFullyQualifiedName();
    }

    @Override
    public List<TagLabel> getTags() {
      return entity.getTags();
    }

    @Override
    public Double getVersion() { return entity.getVersion(); }

    @Override
    public String getUpdatedBy() { return entity.getUpdatedBy(); }

    @Override
    public Date getUpdatedAt() { return entity.getUpdatedAt(); }

    @Override
    public EntityReference getEntityReference() {
      return new EntityReference().withId(getId()).withName(getFullyQualifiedName()).withDescription(getDescription())
              .withDisplayName(getDisplayName()).withType(Entity.PIPELINE);
    }

    @Override
    public Pipeline getEntity() { return entity; }

    @Override
    public void setId(UUID id) { entity.setId(id); }

    @Override
    public void setDescription(String description) {
      entity.setDescription(description);
    }

    @Override
    public void setDisplayName(String displayName) {
      entity.setDisplayName(displayName);
    }

    @Override
    public void setUpdateDetails(String updatedBy, Date updatedAt) {
      entity.setUpdatedBy(updatedBy);
      entity.setUpdatedAt(updatedAt);
    }

    @Override
    public void setChangeDescription(Double newVersion, ChangeDescription changeDescription) {
      entity.setVersion(newVersion);
      entity.setChangeDescription(changeDescription);
    }

    @Override
    public void setTags(List<TagLabel> tags) {
      entity.setTags(tags);
    }
  }

  /**
   * Handles entity updated from PUT and POST operation.
   */
  public class PipelineUpdater extends EntityUpdater {
    public PipelineUpdater(Pipeline original, Pipeline updated, boolean patchOperation) {
      super(original, updated, patchOperation);
    }

    @Override
    public void entitySpecificUpdate() throws IOException {
      updateTasks(original.getEntity(), updated.getEntity());
    }

    private void updateTasks(Pipeline origPipeline, Pipeline updatedPipeline) {
      // Airflow lineage backend gets executed per task in a DAG. This means we will not a get full picture of the
      // pipeline in each call. Hence we may create a pipeline and add a single task when one task finishes in a
      // pipeline
      // in the next task run we may have to update. To take care of this we will merge the tasks
      if (updatedPipeline.getTasks() == null) {
        updatedPipeline.setTasks(origPipeline.getTasks());
      } else {
        updatedPipeline.getTasks().addAll(origPipeline.getTasks()); // TODO remove duplicates
      }

      // Add relationship from pipeline to task
      updateTaskRelationships(updatedPipeline);
      recordChange("tasks", EntityUtil.getIDList(updatedPipeline.getTasks()),
              EntityUtil.getIDList(origPipeline.getTasks()));
    }
  }
}
