// src/services/Graph.jsx
import React from 'react';

const Graph = ({ points, width = 400, height = 200, showControls = true, title = '' }) => {
  if (!points || points.length === 0) {
    return (
      <div className="graph-empty" style={{ width, height }}>
        <div className="empty-graph-message">
          <div className="empty-icon">📊</div>
          <p>Нет данных для графика</p>
        </div>
      </div>
    );
  }

  // Находим min и max значений
  const xValues = points.map(p => p.x);
  const yValues = points.map(p => p.y);
  const minX = Math.min(...xValues);
  const maxX = Math.max(...xValues);
  const minY = Math.min(...yValues);
  const maxY = Math.max(...yValues);
  const rangeX = maxX - minX;
  const rangeY = maxY - minY;

  // Простой SVG график
  return (
    <div className="graph-container" style={{ width }}>
      {title && <h4 className="graph-title">{title}</h4>}

      <div className="graph-svg-container" style={{ width, height }}>
        <svg width={width} height={height} className="graph-svg">
          {/* Оси */}
          <line
            x1={20} y1={height - 20}
            x2={width - 20} y2={height - 20}
            stroke="#333" strokeWidth="2"
          />
          <line
            x1={20} y1={20}
            x2={20} y2={height - 20}
            stroke="#333" strokeWidth="2"
          />

          {/* Точки */}
          {points.map((point, index) => {
            const x = 20 + ((point.x - minX) / rangeX) * (width - 40);
            const y = height - 20 - ((point.y - minY) / rangeY) * (height - 40);

            return (
              <circle
                key={index}
                cx={x}
                cy={y}
                r="3"
                fill="#2196f3"
                stroke="#fff"
                strokeWidth="1"
              />
            );
          })}

          {/* Линия графика */}
          <polyline
            fill="none"
            stroke="#2196f3"
            strokeWidth="2"
            points={points.map((point, index) => {
              const x = 20 + ((point.x - minX) / rangeX) * (width - 40);
              const y = height - 20 - ((point.y - minY) / rangeY) * (height - 40);
              return `${x},${y}`;
            }).join(' ')}
          />
        </svg>
      </div>

      {showControls && (
        <div className="graph-controls">
          <div className="graph-info">
            <div className="info-row">
              <span className="info-label">Точек:</span>
              <span className="info-value">{points.length}</span>
            </div>
            <div className="info-row">
              <span className="info-label">Диапазон X:</span>
              <span className="info-value">
                {minX.toFixed(2)} ... {maxX.toFixed(2)}
              </span>
            </div>
            <div className="info-row">
              <span className="info-label">Диапазон Y:</span>
              <span className="info-value">
                {minY.toFixed(2)} ... {maxY.toFixed(2)}
              </span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Graph;