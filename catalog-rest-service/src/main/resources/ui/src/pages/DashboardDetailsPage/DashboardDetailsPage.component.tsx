import { AxiosPromise, AxiosResponse } from 'axios';
import { compare, Operation } from 'fast-json-patch';
import { EntityTags, TableDetail } from 'Models';
import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import AppState from '../../AppState';
import { getChartById, updateChart } from '../../axiosAPIs/chartAPI';
import {
  addFollower,
  getDashboardByFqn,
  patchDashboardDetails,
  removeFollower,
} from '../../axiosAPIs/dashboardAPI';
import { getServiceById } from '../../axiosAPIs/serviceAPI';
import { TitleBreadcrumbProps } from '../../components/common/title-breadcrumb/title-breadcrumb.interface';
import DashboardDetails from '../../components/DashboardDetails/DashboardDetails.component';
import Loader from '../../components/Loader/Loader';
import { getServiceDetailsPath } from '../../constants/constants';
import { EntityType } from '../../enums/entity.enum';
import { Chart } from '../../generated/entity/data/chart';
import { Dashboard } from '../../generated/entity/data/dashboard';
import { User } from '../../generated/entity/teams/user';
import { addToRecentViewed, getCurrentUserId } from '../../utils/CommonUtils';
import { serviceTypeLogo } from '../../utils/ServiceUtils';
import {
  getOwnerFromId,
  getTagsWithoutTier,
  getTierFromTableTags,
} from '../../utils/TableUtils';
import { getTagCategories, getTaglist } from '../../utils/TagsUtils';
type ChartType = {
  displayName: string;
} & Chart;
const DashboardDetailsPage = () => {
  const USERId = getCurrentUserId();
  const [tagList, setTagList] = useState<Array<string>>([]);
  const { dashboardFQN } = useParams() as Record<string, string>;
  const [dashboardDetails, setDashboardDetails] = useState<Dashboard>(
    {} as Dashboard
  );
  const [dashboardId, setDashboardId] = useState<string>('');
  const [isLoading, setLoading] = useState<boolean>(false);
  const [description, setDescription] = useState<string>('');
  const [followers, setFollowers] = useState<Array<User>>([]);
  const [owner, setOwner] = useState<TableDetail['owner']>();
  const [tier, setTier] = useState<string>();
  const [tags, setTags] = useState<Array<EntityTags>>([]);
  const [activeTab, setActiveTab] = useState<number>(1);
  const [charts, setCharts] = useState<ChartType[]>([]);
  const [dashboardUrl, setDashboardUrl] = useState<string>('');
  const [displayName, setDisplayName] = useState<string>('');
  const [serviceType, setServiceType] = useState<string>('');
  const [slashedDashboardName, setSlashedDashboardName] = useState<
    TitleBreadcrumbProps['titleLinks']
  >([]);

  const activeTabHandler = (tabValue: number) => {
    setActiveTab(tabValue);
  };

  const saveUpdatedDashboardData = (
    updatedData: Dashboard
  ): Promise<AxiosResponse> => {
    const jsonPatch = compare(DashboardDetails, updatedData);

    return patchDashboardDetails(
      dashboardId,
      jsonPatch
    ) as unknown as Promise<AxiosResponse>;
  };

  const fetchTags = () => {
    getTagCategories().then((res) => {
      if (res.data) {
        setTagList(getTaglist(res.data));
      }
    });
  };

  const fetchCharts = async (charts: Dashboard['charts']) => {
    let chartsData: ChartType[] = [];
    let promiseArr: Array<AxiosPromise> = [];
    if (charts?.length) {
      promiseArr = charts.map((chart) =>
        getChartById(chart.id, ['service', 'tags'])
      );
      await Promise.allSettled(promiseArr).then(
        (res: PromiseSettledResult<AxiosResponse>[]) => {
          if (res.length) {
            chartsData = res
              .filter((chart) => chart.status === 'fulfilled')
              .map(
                (chart) =>
                  (chart as PromiseFulfilledResult<AxiosResponse>).value.data
              );
          }
        }
      );
    }

    return chartsData;
  };

  const fetchDashboardDetail = (dashboardFQN: string) => {
    setLoading(true);
    getDashboardByFqn(dashboardFQN, [
      'owner',
      'service',
      'followers',
      'tags',
      'usageSummary',
      'charts',
    ]).then((res: AxiosResponse) => {
      const {
        id,
        description,
        followers,
        fullyQualifiedName,
        service,
        tags,
        owner,
        displayName,
        charts,
        dashboardUrl,
      } = res.data;
      setDisplayName(displayName);
      setDashboardDetails(res.data);
      setDashboardId(id);
      setDescription(description ?? '');
      setFollowers(followers);
      setOwner(getOwnerFromId(owner?.id));
      setTier(getTierFromTableTags(tags));
      setTags(getTagsWithoutTier(tags));
      getServiceById('dashboardServices', service?.id).then(
        (serviceRes: AxiosResponse) => {
          setServiceType(serviceRes.data.serviceType);
          setSlashedDashboardName([
            {
              name: serviceRes.data.name,
              url: serviceRes.data.name
                ? getServiceDetailsPath(
                    serviceRes.data.name,
                    serviceRes.data.serviceType
                  )
                : '',
              imgSrc: serviceRes.data.serviceType
                ? serviceTypeLogo(serviceRes.data.serviceType)
                : undefined,
            },
            {
              name: displayName,
              url: '',
              activeTitle: true,
            },
          ]);

          addToRecentViewed({
            entityType: EntityType.DASHBOARD,
            fqn: fullyQualifiedName,
            serviceType: serviceRes.data.serviceType,
            timestamp: 0,
          });
        }
      );
      setDashboardUrl(dashboardUrl);
      fetchCharts(charts).then((charts) => setCharts(charts));
      setLoading(false);
    });
  };

  const descriptionUpdateHandler = (updatedDashboard: Dashboard) => {
    saveUpdatedDashboardData(updatedDashboard).then((res: AxiosResponse) => {
      const { description } = res.data;
      setDashboardDetails(res.data);
      setDescription(description);
    });
  };

  const followDashboard = () => {
    addFollower(dashboardId, USERId).then((res: AxiosResponse) => {
      const { followers } = res.data;
      setFollowers(followers);
    });
  };
  const unfollowDashboard = () => {
    removeFollower(dashboardId, USERId).then((res: AxiosResponse) => {
      const { followers } = res.data;

      setFollowers(followers);
    });
  };

  const onTagUpdate = (updatedDashboard: Dashboard) => {
    saveUpdatedDashboardData(updatedDashboard).then((res: AxiosResponse) => {
      setTier(getTierFromTableTags(res.data.tags));
      setTags(getTagsWithoutTier(res.data.tags));
    });
  };

  const settingsUpdateHandler = (
    updatedDashboard: Dashboard
  ): Promise<void> => {
    return new Promise<void>((resolve, reject) => {
      saveUpdatedDashboardData(updatedDashboard)
        .then((res) => {
          setDashboardDetails(res.data);
          setOwner(getOwnerFromId(res.data.owner?.id));
          setTier(getTierFromTableTags(res.data.tags));
          resolve();
        })
        .catch(() => reject());
    });
  };

  const onChartUpdate = (
    index: number,
    chartId: string,
    patch: Array<Operation>
  ) => {
    updateChart(chartId, patch).then((res: AxiosResponse) => {
      if (res.data) {
        setCharts((prevCharts) => {
          const charts = [...prevCharts];
          charts[index] = res.data;

          return charts;
        });
      }
    });
  };

  const handleChartTagSelection = (
    index: number,
    chartId: string,
    patch: Array<Operation>
  ) => {
    updateChart(chartId, patch).then((res: AxiosResponse) => {
      if (res.data) {
        setCharts((prevCharts) => {
          const charts = [...prevCharts];
          charts[index] = res.data;

          return charts;
        });
      }
    });
  };

  useEffect(() => {
    fetchDashboardDetail(dashboardFQN);
  }, [dashboardFQN]);

  useEffect(() => {
    fetchTags();
  }, []);

  return (
    <>
      {isLoading ? (
        <Loader />
      ) : (
        <DashboardDetails
          activeTab={activeTab}
          chartDescriptionUpdateHandler={onChartUpdate}
          charts={charts}
          chartTagUpdateHandler={handleChartTagSelection}
          dashboardDetails={dashboardDetails}
          dashboardTags={tags}
          dashboardUrl={dashboardUrl}
          description={description}
          descriptionUpdateHandler={descriptionUpdateHandler}
          entityName={displayName}
          followDashboardHandler={followDashboard}
          followers={followers}
          owner={owner}
          serviceType={serviceType}
          setActiveTabHandler={activeTabHandler}
          settingsUpdateHandler={settingsUpdateHandler}
          slashedDashboardName={slashedDashboardName}
          tagList={tagList}
          tagUpdateHandler={onTagUpdate}
          tier={tier as string}
          unfollowDashboardHandler={unfollowDashboard}
          users={AppState.users}
        />
      )}
    </>
  );
};

export default DashboardDetailsPage;
