/*
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements. See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
  * The ASF licenses this file to You under the Apache License, Version 2.0
  * (the "License"); you may not use this file except in compliance with
  * the License. You may obtain a copy of the License at

  * http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
*/

import { FilterObject } from 'Models';
import { getCurrentUserId } from '../utils/CommonUtils';
import { getFilterString } from '../utils/FilterUtils';

export const myDataSearchIndex =
  'dashboard_search_index,topic_search_index,table_search_index,pipeline_search_index';

export const myDataEntityCounts = {
  tableCount: 0,
  topicCount: 0,
  dashboardCount: 0,
  pipelineCount: 0,
};

export const myDataFilterType = [
  { key: 'Owned', value: 'owner' },
  { key: 'Following', value: 'followers' },
];

export const getFilters = (
  myDataFilters: Array<string>,
  filters: FilterObject
) => {
  const facetFilterString = getFilterString(filters);
  let myDataFilterString = '';
  myDataFilters.map((filter, index) => {
    myDataFilterString += `${filter}:${getCurrentUserId()}${
      index !== myDataFilters.length - 1 ? ' OR ' : ''
    }`;
  });

  return `${
    facetFilterString && myDataFilterString
      ? `${facetFilterString} AND (${myDataFilterString})`
      : myDataFilterString
      ? `(${myDataFilterString})`
      : `${facetFilterString}`
  }`;
};
