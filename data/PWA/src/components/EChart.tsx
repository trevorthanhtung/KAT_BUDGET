import * as echarts from 'echarts'
import { useEffect, useRef, type CSSProperties } from 'react'

type EChartProps = {
  options: echarts.EChartsCoreOption
  style?: CSSProperties
}

export function EChart({ options, style }: EChartProps) {
  const chartRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!chartRef.current) return
    const chart = echarts.init(chartRef.current)
    chart.setOption(options)

    const handleResize = () => chart.resize()
    window.addEventListener('resize', handleResize)

    return () => {
      window.removeEventListener('resize', handleResize)
      chart.dispose()
    }
  }, [options])

  return <div ref={chartRef} style={{ width: '100%', height: '300px', ...style }} />
}