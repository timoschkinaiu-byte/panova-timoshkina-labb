import { useRef, useEffect, useState } from 'react';
import Chart from 'chart.js/auto';
import ZoomPlugin from 'chartjs-plugin-zoom';
import '../../App.css';

Chart.register(ZoomPlugin);

const GraphModal = ({ isOpen, onClose, points, title }) => {
  const canvasRef = useRef(null);
  const [chart, setChart] = useState(null);

  useEffect(() => {
    if (!isOpen || !points || points.length === 0) {
      if (chart) {
        chart.destroy();
        setChart(null);
      }
      return;
    }

    const ctx = canvasRef.current.getContext('2d');

    // Определяем цвета в зависимости от темы
    const isDarkTheme = document.body.classList.contains('light-theme') ? false : true;


    const lineColor = isDarkTheme ? '#800000' : 'rgba(255, 0, 128, 0.8)'; // Красный для темной, розовый для светлой
    const gridColor = isDarkTheme ? 'rgba(255, 50, 50, 0.2)' : 'rgba(128, 0, 0, 0.15)'; // Красная сетка для темной
    const pointColor = isDarkTheme ? 'rgba(255, 100, 100, 0.9)' : 'rgba(255, 0, 128, 0.8)';
    const fillColor = isDarkTheme ? 'rgba(255, 50, 50, 0.1)' : 'rgba(255, 0, 128, 0.05)';

    // Находим min/max для осей
    const xValues = points.map(p => p.x);
    const yValues = points.map(p => p.y);
    const xMin = Math.min(...xValues);
    const xMax = Math.max(...xValues);
    const yMin = Math.min(...yValues);
    const yMax = Math.max(...yValues);

    // Добавляем немного отступа
    const xPadding = (xMax - xMin) * 0.1 || 1;
    const yPadding = (yMax - yMin) * 0.1 || 1;

    const newChart = new Chart(ctx, {
      type: 'line',
      data: {
        datasets: [{
          label: '', // Убираем надпись сверху
          data: points,
          borderColor: lineColor,
          backgroundColor: fillColor,
          borderWidth: 2,
          pointRadius: 4,
          pointBackgroundColor: pointColor,
          pointBorderColor: isDarkTheme ? '#fff' : '#fff',
          pointBorderWidth: 1,
          fill: false,
          tension: 0.3
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: false, // Скрываем легенду полностью
          },
          tooltip: {
              enabled: false
          },
          zoom: {
            pan: {
              enabled: true,
              mode: 'xy',
              modifierKey: 'ctrl'
            },
            zoom: {
              wheel: { enabled: true },
              pinch: { enabled: true },
              mode: 'xy'
            }
          }
        },
        scales: {
          x: {
            type: 'linear',
            title: {
              display: true,
              text: 'X',
              color: isDarkTheme ? 'rgba(255, 255, 255, 0.8)' : 'rgba(0, 0, 0, 0.7)'
            },
            min: xMin - xPadding,
            max: xMax + xPadding,
            grid: {
              color: gridColor,
              lineWidth: 1
            },
            ticks: {
              color: isDarkTheme ? 'rgba(255, 255, 255, 0.6)' : 'rgba(0, 0, 0, 0.5)'
            },
            border: {
              color: isDarkTheme ? 'rgba(255, 255, 255, 0.3)' : 'rgba(0, 0, 0, 0.2)'
            }
          },
          y: {
            title: {
              display: true,
              text: 'Y = f(X)',
              color: isDarkTheme ? 'rgba(255, 255, 255, 0.8)' : 'rgba(0, 0, 0, 0.7)'
            },
            min: yMin - yPadding,
            max: yMax + yPadding,
            grid: {
              color: gridColor,
              lineWidth: 0.5
            },
            ticks: {
              color: isDarkTheme ? 'rgba(255, 255, 255, 0.6)' : 'rgba(0, 0, 0, 0.5)'
            },
            border: {
              color: isDarkTheme ? 'rgba(255, 255, 255, 0.3)' : 'rgba(0, 0, 0, 0.2)'
            }
          }
        }
      }
    });

    setChart(newChart);

    return () => {
      if (newChart) {
        newChart.destroy();
      }
    };
  }, [isOpen, points, title]);

  if (!isOpen) return null;

  return (
    <div className="modal-overlay">
      <div className="modal-content" style={{ maxWidth: '800px', maxHeight: '90vh' }}>
        <div className="modal-header">
          <h3 className="modal-title">График функции: {title}</h3>
          <button
            className="modal-close"
            onClick={onClose}
            style={{
              background: 'none',
              border: 'none',
              fontSize: '24px',
              cursor: 'pointer',
              color: 'var(--text-secondary)'
            }}
          >
            ×
          </button>
        </div>

        <div style={{ height: '500px', margin: '20px 0' }}>
          <canvas ref={canvasRef}></canvas>
        </div>

        <div style={{
          fontSize: '12px',
          color: 'var(--text-secondary)',
          marginBottom: '15px',
          textAlign: 'center'
        }}>
          Масштабирование: колесо мыши • Перемещение: Ctrl + перетаскивание
        </div>

        <div className="modal-buttons">
          <button
            className="btn-secondary"
            onClick={() => {
              if (chart) chart.resetZoom();
            }}
          >
            Сбросить масштаб
          </button>
          <button className="btn-primary" onClick={onClose}>
            Закрыть
          </button>
        </div>
      </div>
    </div>
  );
};

export default GraphModal;