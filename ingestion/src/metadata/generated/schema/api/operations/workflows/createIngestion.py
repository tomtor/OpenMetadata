# generated by datamodel-codegen:
#   filename:  schema/api/operations/workflows/createIngestion.json
#   timestamp: 2021-11-16T07:44:38+00:00

from __future__ import annotations

from typing import List, Optional

from pydantic import BaseModel, Field, constr

from ....operations.workflows import ingestion
from ....type import basic, entityReference, tagLabel


class CreateIngestionEntityRequest(BaseModel):
    name: constr(min_length=1, max_length=256) = Field(
        ..., description='Name that identifies this ingestion instance uniquely.'
    )
    displayName: Optional[str] = Field(
        None, description='Display Name that identifies this Ingestion.'
    )
    description: Optional[str] = Field(None, description='Description of the workflow.')
    ingestionType: Optional[ingestion.IngestionType] = None
    owner: Optional[entityReference.EntityReference] = Field(
        None, description='Owner of this Ingestion.'
    )
    tags: Optional[List[tagLabel.TagLabel]] = Field(
        None, description='Tags associated with the Ingestion.'
    )
    forceDeploy: Optional[bool] = Field(
        'false',
        description='Deploy the workflow by overwriting existing workflow with the same name.',
    )
    pauseWorkflow: Optional[bool] = Field(
        'false',
        description='pause the workflow from running once the deploy is finished successfully.',
    )
    concurrency: Optional[int] = Field(1, description='Concurrency of the Pipeline.')
    startDate: basic.Date = Field(..., description='Start date of the workflow.')
    endDate: Optional[basic.Date] = Field(None, description='End Date of the workflow.')
    workflowTimezone: Optional[str] = Field(
        'UTC', description='Timezone in which workflow going to be scheduled.'
    )
    retries: Optional[int] = Field(1, description='Retry workflow in case of failure')
    retryDelay: Optional[int] = Field(
        300, description='Delay between retries in seconds.'
    )
    workflowCatchup: Optional[bool] = Field(
        'false', description='Workflow catchup for past executions.'
    )
    scheduleInterval: Optional[str] = Field(
        None, description='Scheduler Interval for the Workflow in cron format.'
    )
    workflowTimeout: Optional[int] = Field(
        60, description='Timeout for the workflow in seconds.'
    )
    service: entityReference.EntityReference = Field(
        ...,
        description='Link to the database service where this database is hosted in.',
    )
    connectorConfig: ingestion.ConnectorConfig
