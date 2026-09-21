import React from 'react';
import {Area,AreaChart,CartesianGrid,ResponsiveContainer,Tooltip,XAxis,YAxis} from 'recharts';

export default function LatencyChart({data}) {
  return <ResponsiveContainer width="100%" height={210}>
    <AreaChart data={data}>
      <defs><linearGradient id="latency" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#5b8cff" stopOpacity={.3}/><stop offset="100%" stopColor="#5b8cff" stopOpacity={0}/></linearGradient></defs>
      <CartesianGrid stroke="#202b3b" vertical={false}/>
      <XAxis dataKey="time" stroke="#69778a"/>
      <YAxis stroke="#69778a"/>
      <Tooltip contentStyle={{background:'#111b29',border:'1px solid #26364a',borderRadius:8}}/>
      <Area dataKey="latency" stroke="#6e9aff" fill="url(#latency)" strokeWidth={2}/>
    </AreaChart>
  </ResponsiveContainer>;
}
