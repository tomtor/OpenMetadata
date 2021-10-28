/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * This schema defines the Database Service entity, such as MySQL, BigQuery, Redshift,
 * Postgres, or Snowflake. Alternative terms such as Database Cluster, Database Server
 * instance are also used for database service.
 */
export interface DatabaseService {
  /**
   * Change that lead to this version of the entity.
   */
  changeDescription?: ChangeDescription;
  /**
   * Description of a database service instance.
   */
  description?: string;
  /**
   * Display Name that identifies this database service.
   */
  displayName?: string;
  /**
   * Link to the resource corresponding to this database service.
   */
  href: string;
  /**
   * Unique identifier of this database service instance.
   */
  id: string;
  /**
   * Schedule for running metadata ingestion jobs.
   */
  ingestionSchedule?: Schedule;
  /**
   * JDBC connection information.
   */
  jdbc: JDBCInfo;
  /**
   * Name that identifies this database service.
   */
  name: string;
  /**
   * Type of database service such as MySQL, BigQuery, Snowflake, Redshift, Postgres...
   */
  serviceType: DatabaseServiceType;
  /**
   * Last update time corresponding to the new version of the entity.
   */
  updatedAt?: Date;
  /**
   * User who made the update.
   */
  updatedBy?: string;
  /**
   * Metadata version of the entity.
   */
  version?: number;
}

/**
 * Change that lead to this version of the entity.
 *
 * Description of the change.
 */
export interface ChangeDescription {
  /**
   * Fields added during the version changes.
   */
  fieldsAdded?: string[];
  /**
   * Fields deleted during the version changes.
   */
  fieldsDeleted?: string[];
  /**
   * Fields modified during the version changes.
   */
  fieldsUpdated?: string[];
  previousVersion?: number;
}

/**
 * Schedule for running metadata ingestion jobs.
 *
 * This schema defines the type used for the schedule. The schedule has a start time and
 * repeat frequency.
 */
export interface Schedule {
  /**
   * Repeat frequency in ISO 8601 duration format. Example - 'P23DT23H'.
   */
  repeatFrequency?: string;
  /**
   * Start date and time of the schedule.
   */
  startDate?: Date;
}

/**
 * JDBC connection information.
 *
 * Type for capturing JDBC connector information.
 */
export interface JDBCInfo {
  connectionUrl: string;
  driverClass: string;
}

/**
 * Type of database service such as MySQL, BigQuery, Snowflake, Redshift, Postgres...
 */
export enum DatabaseServiceType {
  Athena = 'Athena',
  BigQuery = 'BigQuery',
  Hive = 'Hive',
  Mssql = 'MSSQL',
  MySQL = 'MySQL',
  Oracle = 'Oracle',
  Postgres = 'Postgres',
  Presto = 'Presto',
  Redshift = 'Redshift',
  Snowflake = 'Snowflake',
  Trino = 'Trino',
  Vertica = 'Vertica',
}
