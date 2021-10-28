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

import { getByTestId, getByText, render } from '@testing-library/react';
import React from 'react';
import { MemoryRouter } from 'react-router';
import MyDataHeader from './MyDataHeader.component';

describe('Test MyDataHeader Component', () => {
  it('Component should render', () => {
    const { container } = render(
      <MyDataHeader
        countServices={193}
        entityCounts={{
          tableCount: 40,
          topicCount: 13,
          dashboardCount: 10,
          pipelineCount: 3,
        }}
      />,
      {
        wrapper: MemoryRouter,
      }
    );

    const myDataHeader = getByTestId(container, 'data-header-container');

    expect(myDataHeader).toBeInTheDocument();
  });

  it('Should have main title', () => {
    const { container } = render(
      <MyDataHeader
        countServices={193}
        entityCounts={{
          tableCount: 40,
          topicCount: 13,
          dashboardCount: 10,
          pipelineCount: 3,
        }}
      />,
      {
        wrapper: MemoryRouter,
      }
    );

    const mainTitle = getByTestId(container, 'main-title');

    expect(mainTitle).toBeInTheDocument();
  });

  it('Should have 7 data summary details', () => {
    const { container } = render(
      <MyDataHeader
        countServices={193}
        entityCounts={{
          tableCount: 40,
          topicCount: 13,
          dashboardCount: 10,
          pipelineCount: 3,
        }}
      />,
      {
        wrapper: MemoryRouter,
      }
    );

    const dataSummary = getByTestId(container, 'data-summary-container');

    expect(dataSummary.childElementCount).toBe(7);
  });

  it('Should display same count as provided by props', () => {
    const { container } = render(
      <MyDataHeader
        countServices={4}
        entityCounts={{
          tableCount: 40,
          topicCount: 13,
          dashboardCount: 10,
          pipelineCount: 3,
        }}
      />,
      {
        wrapper: MemoryRouter,
      }
    );

    expect(getByText(container, /40 tables/i)).toBeInTheDocument();
    expect(getByText(container, /13 topics/i)).toBeInTheDocument();
    expect(getByText(container, /10 dashboards/i)).toBeInTheDocument();
    expect(getByText(container, /3 pipelines/i)).toBeInTheDocument();
    expect(getByText(container, /4 services/i)).toBeInTheDocument();
  });

  it('OnClick it should redirect to respective page', () => {
    const { container } = render(
      <MyDataHeader
        countServices={4}
        entityCounts={{
          tableCount: 40,
          topicCount: 13,
          dashboardCount: 10,
          pipelineCount: 3,
        }}
      />,
      {
        wrapper: MemoryRouter,
      }
    );
    const tables = getByTestId(container, 'tables');
    const topics = getByTestId(container, 'topics');
    const dashboards = getByTestId(container, 'dashboards');
    const pipelines = getByTestId(container, 'pipelines');
    const service = getByTestId(container, 'service');
    const user = getByTestId(container, 'user');
    const terms = getByTestId(container, 'terms');

    expect(tables).toHaveAttribute('href', '/explore/tables');
    expect(topics).toHaveAttribute('href', '/explore/topics');
    expect(dashboards).toHaveAttribute('href', '/explore/dashboards');
    expect(pipelines).toHaveAttribute('href', '/explore/pipelines');
    expect(service).toHaveAttribute('href', '/services');
    expect(user).toHaveAttribute('href', '/teams');
    expect(terms).toHaveAttribute('href', '/teams');
  });
});
