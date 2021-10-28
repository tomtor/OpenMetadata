import React from 'react';
import { Area, AreaChart, Tooltip } from 'recharts';

type Props = {
  data: Array<{ date: Date | undefined; value: number | undefined }>;
  margin?: { top: number; left: number; right: number; bottom: number };
  toolTipPos?: { x: number; y: number };
  height?: number;
  className?: string;
};
const TableProfilerGraph = ({
  data,
  margin,
  toolTipPos,
  height,
  className = '',
}: Props) => {
  const CustomTooltip = ({
    active,
    payload,
  }: {
    active: boolean;
    // eslint-disable-next-line
    payload: any;
  }) => {
    if (active && payload && payload.length) {
      return (
        <div className="tw-py-1.5 tw-px-1 tw-bg-black tw-opacity-50 tw-rounded tw-text-white tw-text-xs tw-font-normal">
          <div>Value {payload[0].value}</div>
          <div>Date {payload[0].payload.date}</div>
        </div>
      );
    }

    return null;
  };

  return (
    <div className={className}>
      <AreaChart
        data={data}
        height={height ?? 40}
        margin={
          margin ?? {
            top: 10,
            right: 30,
            left: 0,
            bottom: 0,
          }
        }
        width={150}>
        <Tooltip
          content={CustomTooltip}
          cursor={{ stroke: '#FF4C3B', strokeWidth: 2 }}
          offset={20}
          position={toolTipPos ?? { x: 20, y: -40 }}
        />
        <Area
          dataKey="value"
          fill="#7147E8"
          fillOpacity="0.4"
          stroke="#7147E8"
          type="monotone"
        />
      </AreaChart>
    </div>
  );
};

export default TableProfilerGraph;
