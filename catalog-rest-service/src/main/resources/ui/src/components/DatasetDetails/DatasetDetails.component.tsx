import { isEqual, isNil } from 'lodash';
import { ColumnJoins, EntityTags } from 'Models';
import React, { useEffect, useState } from 'react';
import { getTeamDetailsPath } from '../../constants/constants';
import {
  JoinedWith,
  Table,
  TableJoins,
  TypeUsedToReturnUsageDetailsOfAnEntity,
} from '../../generated/entity/data/table';
import { User } from '../../generated/entity/teams/user';
import { LabelType, State } from '../../generated/type/tagLabel';
import { useAuth } from '../../hooks/authHooks';
import {
  getCurrentUserId,
  getPartialNameFromFQN,
  getTableFQNFromColumnFQN,
  getUserTeams,
} from '../../utils/CommonUtils';
import { getTagsWithoutTier, getUsagePercentile } from '../../utils/TableUtils';
import Description from '../common/description/Description';
import EntityPageInfo from '../common/entityPageInfo/EntityPageInfo';
import TabsPane from '../common/TabsPane/TabsPane';
import PageContainer from '../containers/PageContainer';
import Entitylineage from '../EntityLineage/EntityLineage.component';
import FrequentlyJoinedTables from '../FrequentlyJoinedTables/FrequentlyJoinedTables.component';
import ManageTab from '../ManageTab/ManageTab.component';
import SchemaTab from '../SchemaTab/SchemaTab.component';
import TableProfiler from '../TableProfiler/TableProfiler.component';
import TableProfilerGraph from '../TableProfiler/TableProfilerGraph.component';
import { DatasetDetailsProps } from './DatasetDetails.interface';

const DatasetDetails: React.FC<DatasetDetailsProps> = ({
  entityName,
  datasetFQN,
  activeTab,
  setActiveTabHandler,
  owner,
  description,
  tableProfile,
  columns,
  tier,
  sampleData,
  entityLineage,
  followTableHandler,
  unfollowTableHandler,
  followers,
  slashedTableName,
  tableTags,
  tableDetails,
  descriptionUpdateHandler,
  columnsUpdateHandler,
  settingsUpdateHandler,
  users,
  usageSummary,
  joins,
}: DatasetDetailsProps) => {
  const { isAuthDisabled } = useAuth();
  const [isEdit, setIsEdit] = useState(false);
  const [followersCount, setFollowersCount] = useState(0);
  const [isFollowing, setIsFollowing] = useState(false);
  const [usage, setUsage] = useState('');
  const [weeklyUsageCount, setWeeklyUsageCount] = useState('');
  const [tableJoinData, setTableJoinData] = useState<TableJoins>({
    startDate: new Date(),
    dayCount: 0,
    columnJoins: [],
  });

  const setUsageDetails = (
    usageSummary: TypeUsedToReturnUsageDetailsOfAnEntity
  ) => {
    if (!isNil(usageSummary?.weeklyStats?.percentileRank)) {
      const percentile = getUsagePercentile(
        usageSummary?.weeklyStats?.percentileRank || 0
      );
      setUsage(percentile);
    } else {
      setUsage('--');
    }
    setWeeklyUsageCount(
      usageSummary?.weeklyStats?.count.toLocaleString() || '--'
    );
  };
  const hasEditAccess = () => {
    if (owner?.type === 'user') {
      return owner.id === getCurrentUserId();
    } else {
      return getUserTeams().some((team) => team.id === owner?.id);
    }
  };
  const setFollowersData = (followers: Array<User>) => {
    setIsFollowing(
      followers.some(({ id }: { id: string }) => id === getCurrentUserId())
    );
    setFollowersCount(followers?.length);
  };
  const tabs = [
    {
      name: 'Schema',
      icon: {
        alt: 'schema',
        name: 'icon-schema',
        title: 'Schema',
      },
      isProtected: false,
      position: 1,
    },
    {
      name: 'Profiler',
      icon: {
        alt: 'profiler',
        name: 'icon-profiler',
        title: 'Profiler',
      },
      isProtected: false,
      position: 2,
    },
    {
      name: 'Lineage',
      icon: {
        alt: 'lineage',
        name: 'icon-lineage',
        title: 'Lineage',
      },
      isProtected: false,
      position: 3,
    },
    {
      name: 'Manage',
      icon: {
        alt: 'manage',
        name: 'icon-manage',
        title: 'Manage',
      },
      isProtected: true,
      protectedState: !owner || hasEditAccess(),
      position: 4,
    },
  ];

  const getFrequentlyJoinedWithTables = (): Array<
    JoinedWith & { name: string }
  > => {
    let freqJoin: Array<JoinedWith & { name: string }> = [];
    for (const joinData of tableJoinData.columnJoins as ColumnJoins[]) {
      freqJoin = [
        ...freqJoin,
        ...(joinData?.joinedWith?.map((joinedCol) => {
          const tableFQN = getTableFQNFromColumnFQN(
            joinedCol?.fullyQualifiedName as string
          );

          return {
            name: getPartialNameFromFQN(tableFQN, ['database', 'table']),
            fullyQualifiedName: tableFQN,
            joinCount: joinedCol.joinCount,
          };
        }) as Array<JoinedWith & { name: string }>),
      ].sort((a, b) =>
        (a?.joinCount as number) > (b?.joinCount as number)
          ? 1
          : (b?.joinCount as number) > (a?.joinCount as number)
          ? -1
          : 0
      );
    }

    return freqJoin;
  };

  const extraInfo: Array<{
    key?: string;
    value: string | number | React.ReactNode;
    isLink?: boolean;
    placeholderText?: string;
    openInNewTab?: boolean;
  }> = [
    {
      key: 'Owner',
      value:
        owner?.type === 'team'
          ? getTeamDetailsPath(owner?.name || '')
          : owner?.name || '',
      placeholderText: owner?.displayName || '',
      isLink: owner?.type === 'team',
      openInNewTab: false,
    },
    { key: 'Tier', value: tier ? tier.split('.')[1] : '' },
    { key: 'Usage', value: usage },
    { key: 'Queries', value: `${weeklyUsageCount} past week` },
    {
      key: 'Columns',
      value:
        tableProfile && tableProfile[0]?.columnCount
          ? tableProfile[0].columnCount
          : '--',
    },
    {
      key: 'Rows',
      value: tableProfile ? (
        <TableProfilerGraph
          className="tw--mt-5"
          data={
            tableProfile
              ?.map((d) => ({
                date: d.profileDate,
                value: d.rowCount ?? 0,
              }))
              .reverse() as Array<{
              date: Date;
              value: number;
            }>
          }
          height={38}
          toolTipPos={{ x: 20, y: -30 }}
        />
      ) : (
        '--'
      ),
    },
  ];

  const onDescriptionEdit = (): void => {
    setIsEdit(true);
  };
  const onCancel = () => {
    setIsEdit(false);
  };

  const onDescriptionUpdate = (updatedHTML: string) => {
    if (description !== updatedHTML) {
      const updatedTableDetails = {
        ...tableDetails,
        description: updatedHTML,
      };
      descriptionUpdateHandler(updatedTableDetails);
      setIsEdit(false);
    } else {
      setIsEdit(false);
    }
  };

  const onColumnsUpdate = (updateColumns: Table['columns']) => {
    if (!isEqual(columns, updateColumns)) {
      const updatedTableDetails = {
        ...tableDetails,
        columns: updateColumns,
      };
      columnsUpdateHandler(updatedTableDetails);
    }
  };

  const onSettingsUpdate = (newOwner?: Table['owner'], newTier?: string) => {
    if (newOwner || newTier) {
      const tierTag: Table['tags'] = newTier
        ? [
            ...getTagsWithoutTier(tableDetails.tags as Array<EntityTags>),
            {
              tagFQN: newTier,
              labelType: LabelType.Manual,
              state: State.Confirmed,
            },
          ]
        : tableDetails.tags;
      const updatedTableDetails = {
        ...tableDetails,
        owner: newOwner
          ? {
              ...tableDetails.owner,
              ...newOwner,
            }
          : tableDetails.owner,
        tags: tierTag,
      };

      return settingsUpdateHandler(updatedTableDetails);
    } else {
      return Promise.reject();
    }
  };

  const followTable = () => {
    if (isFollowing) {
      setFollowersCount((preValu) => preValu - 1);
      setIsFollowing(false);
      unfollowTableHandler();
    } else {
      setFollowersCount((preValu) => preValu + 1);
      setIsFollowing(true);
      followTableHandler();
    }
  };

  useEffect(() => {
    if (isAuthDisabled && users.length && followers.length) {
      setFollowersData(followers);
    }
  }, [users, followers]);

  useEffect(() => {
    setFollowersData(followers);
  }, [followers]);
  useEffect(() => {
    setUsageDetails(usageSummary);
  }, [usageSummary]);

  useEffect(() => {
    setTableJoinData(joins);
  }, [joins]);

  return (
    <PageContainer>
      <div className="tw-px-4 tw-w-full tw-h-full tw-flex tw-flex-col">
        <EntityPageInfo
          entityName={entityName}
          extraInfo={extraInfo}
          followers={followersCount}
          followersList={followers}
          followHandler={followTable}
          isFollowing={isFollowing}
          tags={tableTags}
          tier={tier || ''}
          titleLinks={slashedTableName}
        />

        <div className="tw-mt-1 tw-flex tw-flex-col tw-flex-grow">
          <TabsPane
            activeTab={activeTab}
            className="tw-flex-initial"
            setActiveTab={setActiveTabHandler}
            tabs={tabs}
          />

          <div className="tw-bg-white tw-flex-grow">
            {activeTab === 1 && (
              <div className="tw-grid tw-grid-cols-4 tw-gap-4 tw-w-full tw-mt-4 ">
                <div className="tw-col-span-3">
                  <Description
                    description={description}
                    hasEditAccess={hasEditAccess()}
                    isEdit={isEdit}
                    owner={owner}
                    onCancel={onCancel}
                    onDescriptionEdit={onDescriptionEdit}
                    onDescriptionUpdate={onDescriptionUpdate}
                  />
                </div>
                <div className="tw-col-span-1 tw-border tw-border-main tw-rounded-md">
                  <FrequentlyJoinedTables
                    header="Frequently Joined Tables"
                    tableList={getFrequentlyJoinedWithTables()}
                  />
                </div>
                <div className="tw-col-span-full">
                  <SchemaTab
                    columnName={getPartialNameFromFQN(
                      datasetFQN,
                      ['column'],
                      '.'
                    )}
                    columns={columns}
                    hasEditAccess={hasEditAccess()}
                    joins={tableJoinData.columnJoins as ColumnJoins[]}
                    owner={owner}
                    sampleData={sampleData}
                    onUpdate={onColumnsUpdate}
                  />
                </div>
              </div>
            )}
            {activeTab === 2 && (
              <div className="tw-mt-4">
                <TableProfiler
                  columns={columns.map((col) => col.name)}
                  tableProfiles={tableProfile}
                />
              </div>
            )}
            {activeTab === 3 && (
              <div className="tw-h-full">
                <Entitylineage entityLineage={entityLineage} />
              </div>
            )}
            {activeTab === 4 && (
              <div className="tw-mt-4">
                <ManageTab
                  currentTier={tier}
                  currentUser={owner?.id}
                  hasEditAccess={hasEditAccess()}
                  onSave={onSettingsUpdate}
                />
              </div>
            )}
          </div>
        </div>
      </div>
    </PageContainer>
  );
};

export default DatasetDetails;
