import { useEffect, useRef, useState } from 'react';
import Chart from 'chart.js/auto';
import ZoomPlugin from 'chartjs-plugin-zoom';
import '../../App.css';

Chart.register(ZoomPlugin);

const GraphPreview = ({ points, title, xRange, yRange, isLoading, showTitle = false, compact = false }) => {
  const canvasRef = useRef(null);
  const chartRef = useRef(null);
  const [zoomLevel, setZoomLevel] = useState(1);
  const [selectedPoint, setSelectedPoint] = useState(null);

  useEffect(() => {
    if (!canvasRef.current || !points || points.length === 0) {
      if (chartRef.current) {
        chartRef.current.destroy();
        chartRef.current = null;
      }
      return;
    }

    // Уничтожаем предыдущий график
    if (chartRef.current) {
      chartRef.current.destroy();
    }

    const ctx = canvasRef.current.getContext('2d');

    // Определяем цвета в зависимости от темы
    const isDarkTheme = document.body.classList.contains('light-theme') ? false : true;

    // Цвета для темной/светлой темы
    const lineColor = isDarkTheme ? '#800000' : '#FF97BB'; // Тёмный красный / Светлый розовый
    const gridColor = isDarkTheme ? 'rgba(128, 0, 0, 0.2)' : 'rgba(255, 151, 187, 0.2)';
    const pointColor = isDarkTheme ? '#FF4444' : '#FF97BB'; // Яркий красный / Розовый
    const backgroundColor = isDarkTheme ? 'rgba(128, 0, 0, 0.05)' : 'rgba(255, 151, 187, 0.05)';

    // Автоматически определяем диапазоны с учетом константных функций
    const xValues = points.map(p => p.x);
    const yValues = points.map(p => p.y);

    let adjustedYMin = yRange.min;
    let adjustedYMax = yRange.max;

    // Если все Y одинаковы (константная функция), расширяем диапазон для видимости
    const isConstantFunction = Math.abs(Math.max(...yValues) - Math.min(...yValues)) < 1e-10;
    if (isConstantFunction && points.length > 0) {
      const constantValue = points[0].y;
      // Добавляем небольшой отступ вокруг константного значения
      const padding = Math.max(Math.abs(constantValue) * 0.1, 0.1);
      adjustedYMin = constantValue - padding;
      adjustedYMax = constantValue + padding;
    }

    // Подготовка данных с обработчиком клика
    const data = {
      datasets: [{
        label: '',
        data: points.map((point, index) => ({
          x: point.x,
          y: point.y,
          index: index
        })),
        borderColor: lineColor,
        backgroundColor: backgroundColor,
        borderWidth: 1.5,
        pointRadius: 2, // ОЧЕНЬ маленькие точки
        pointBackgroundColor: pointColor,
        pointBorderColor: isDarkTheme ? '#fff' : '#fff',
        pointBorderWidth: 0.5,
        pointHoverRadius: 4, // Немного увеличивается при наведении
        pointHoverBackgroundColor: isDarkTheme ? '#FF0000' : '#FF3399',
        pointHoverBorderColor: '#fff',
        pointHoverBorderWidth: 1,
        fill: false,
        tension: 0.4, // Минимальное сглаживание
        showLine: true
      }]
    };

    // Настройки графика
    const config = {
      type: 'line',
      data: data,
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: false,
          },
          tooltip: {
            enabled: false, // Отключаем всплывающие подсказки при наведении
          },
          zoom: {
            pan: {
              enabled: true,
              mode: 'xy',
              modifierKey: 'ctrl'
            },
            zoom: {
              wheel: {
                enabled: true,
              },
              pinch: {
                enabled: true
              },
              mode: 'xy',
              onZoom: ({ chart }) => {
                const scaleX = chart.scales.x.max - chart.scales.x.min;
                const scaleY = chart.scales.y.max - chart.scales.y.min;
                const originalRangeX = xRange.max - xRange.min;
                const originalRangeY = adjustedYMax - adjustedYMin;
                setZoomLevel(Math.max(scaleX / originalRangeX, scaleY / originalRangeY));
              }
            }
          }
        },
        scales: {
          x: {
            type: 'linear',
            position: 'bottom',
            title: {
              display: true,
              text: 'X',
              color: isDarkTheme ? 'rgba(255, 255, 255, 0.8)' : 'rgba(0, 0, 0, 0.7)',
              font: {
                size: 12
              }
            },
            grid: {
              color: gridColor,
              lineWidth: 0.5
            },
            ticks: {
              color: isDarkTheme ? 'rgba(255, 255, 255, 0.6)' : 'rgba(0, 0, 0, 0.5)',
              font: {
                size: 10
              }
            },
            min: xRange.min,
            max: xRange.max,
            border: {
              color: isDarkTheme ? 'rgba(255, 255, 255, 0.3)' : 'rgba(0, 0, 0, 0.2)'
            }
          },
          y: {
            title: {
              display: true,
              text: 'Y = f(X)',
              color: isDarkTheme ? 'rgba(255, 255, 255, 0.8)' : 'rgba(0, 0, 0, 0.7)',
              font: {
                size: 12
              }
            },
            grid: {
              color: gridColor,
              lineWidth: 0.5
            },
            ticks: {
              color: isDarkTheme ? 'rgba(255, 255, 255, 0.6)' : 'rgba(0, 0, 0, 0.5)',
              font: {
                size: 10
              }
            },
            min: adjustedYMin,
            max: adjustedYMax,
            border: {
              color: isDarkTheme ? 'rgba(255, 255, 255, 0.3)' : 'rgba(0, 0, 0, 0.2)'
            }
          }
        },
        interaction: {
          intersect: false,
          mode: 'point'
        },
        onClick: (event, elements) => {
          if (elements.length > 0) {
            const element = elements[0];
            const datasetIndex = element.datasetIndex;
            const index = element.index;
            const point = chartRef.current.data.datasets[datasetIndex].data[index];
            setSelectedPoint({
              x: point.x.toFixed(3),
              y: point.y.toFixed(3),
              index: point.index
            });
          } else {
            setSelectedPoint(null);
          }
        }
      }
    };

    // Создаём новый график
    chartRef.current = new Chart(ctx, config);

    // Очистка при размонтировании
    return () => {
      if (chartRef.current) {
        chartRef.current.destroy();
      }
    };
  }, [points, xRange, yRange]);

  const handleResetZoom = () => {
    if (chartRef.current) {
      chartRef.current.resetZoom();
      setZoomLevel(1);
      setSelectedPoint(null);
    }
  };

  if (isLoading) {
    return (
      <div className="graph-container">
        <div className="graph-loading">
          <div className="loading-spinner"></div>
          <p>Загрузка графика...</p>
        </div>
      </div>
    );
  }

  if (!points || points.length === 0) {
    return (
      <div className="graph-container">
        <div className="graph-empty">
          <p>Нет данных для отображения графика</p>
          <small>Создайте функцию, чтобы увидеть её график</small>
        </div>
      </div>
    );
  }

  return (
    <div className={`graph-container ${compact ? 'compact' : ''}`}>
      {showTitle && (
        <div className="graph-header">
          <h3 className="graph-title">График функции</h3>
        </div>
      )}

      <div className="graph-canvas-container">
        <canvas
          ref={canvasRef}
          onClick={(e) => {
            // Обработка клика вне точки
            if (!e.target.getContext) return;
            const rect = e.target.getBoundingClientRect();
            const x = e.clientX - rect.left;
            const y = e.clientY - rect.top;

            // Если клик не на точке, сбрасываем выбор
            if (chartRef.current && chartRef.current.getElementsAtEventForMode) {
              const elements = chartRef.current.getElementsAtEventForMode(
                e.nativeEvent,
                'point',
                { intersect: true },
                false
              );
              if (elements.length === 0) {
                setSelectedPoint(null);
              }
            }
          }}
        ></canvas>
      </div>

      {/* Информация о выбранной точке */}
      {selectedPoint && (
        <div className="selected-point-info">
          <span>Выбрана точка #{selectedPoint.index + 1}: </span>
          <span>X = {selectedPoint.x}, Y = {selectedPoint.y}</span>
          <button
            className="btn-clear-point"
            onClick={() => setSelectedPoint(null)}
            title="Снять выделение"
          >
            ✕
          </button>
        </div>
      )}

      <div className="graph-controls">
        <button
          className="btn-secondary btn-small"
          onClick={handleResetZoom}
          title="Сбросить масштаб"
          disabled={zoomLevel === 1}
        >
          Сбросить масштаб {zoomLevel !== 1 && `(${zoomLevel.toFixed(1)}x)`}
        </button>
      </div>

      <div className="graph-hint">
        <small>Кликните на точку, чтобы увидеть её координаты | Масштаб: колесо мыши | Перемещение: Ctrl + перетаскивание</small>
      </div>

      <div className="graph-info">
        <div className="graph-stats">
          <span>Точек: {points.length}</span>
          <span>X: [{xRange.min.toFixed(2)}; {xRange.max.toFixed(2)}]</span>
          <span>Y: [{yRange.min.toFixed(2)}; {yRange.max.toFixed(2)}]</span>
        </div>
      </div>
    </div>
  );
};

export default GraphPreview;