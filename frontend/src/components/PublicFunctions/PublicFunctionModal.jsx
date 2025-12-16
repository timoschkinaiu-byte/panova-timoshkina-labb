import { useState, useEffect } from 'react';
import GraphPreview from '../Common/GraphPreview';
import functionService from '../../services/functionService';
import notificationService from '../../services/notificationService';
import "../../App.css";

const PublicFunctionModal = ({ isOpen, onClose, function: func }) => {
  const [activeTab, setActiveTab] = useState('graph');

  // Состояния для вкладки "Точки"
  const [pointsView, setPointsView] = useState('all');
  const [points, setPoints] = useState([]);
  const [isLoadingPoints, setIsLoadingPoints] = useState(false);
  const [xFrom, setXFrom] = useState('');
  const [xTo, setXTo] = useState('');

  // Состояния для графика
  const [graphData, setGraphData] = useState([]);
  const [isLoadingGraph, setIsLoadingGraph] = useState(true);
  const [graphError, setGraphError] = useState(null);

  const [isExporting, setIsExporting] = useState(false);

  // Загрузка данных при открытии
  useEffect(() => {
    if (isOpen) {
      loadGraphData();
      if (activeTab === 'points') {
        loadPoints();
      }
    }
  }, [isOpen, activeTab]);

  // Загрузка данных графика
  const loadGraphData = async () => {
    setIsLoadingGraph(true);
    setGraphError(null);
    try {
      // 1. Получаем реальные точки функции
      const pointsResponse = await functionService.getFunctionPoints(func.functionId);

      if (!pointsResponse.data || pointsResponse.data.length === 0) {
        throw new Error('Функция не содержит точек');
      }

      const actualPoints = pointsResponse.data;
      const pointsCount = actualPoints.length;

      console.log('У функции точек:', pointsCount);

      // 2. Получаем границы X из реальных точек
      const xValues = actualPoints.map(p =>
        p.xvalue !== undefined ? p.xvalue :
        p.xValue !== undefined ? p.xValue :
        p.x !== undefined ? p.x : 0
      );

      const leftX = Math.min(...xValues);
      const rightX = Math.max(...xValues);

      console.log('Границы X:', leftX, 'до', rightX);

      // 3. Запрашиваем график с РЕАЛЬНЫМИ параметрами
      const response = await functionService.getGraphData(
        func.functionId,
        pointsCount,     // Реальное количество точек
        leftX,          // Реальная левая граница
        rightX          // Реальная правая граница
      );

      // 4. Проверяем ответ
      if (!response.data || !response.data.points) {
        throw new Error('Сервер не вернул данные графика');
      }

      if (response.data.points.length === 0) {
        throw new Error('График пустой');
      }

      // 5. Преобразуем данные
      const formattedPoints = response.data.points.map(p => {
        // Обрабатываем все возможные форматы
        const x = p.x !== undefined ? p.x :
                 p.xValue !== undefined ? p.xValue :
                 p.xvalue !== undefined ? p.xvalue : 0;

        const y = p.y !== undefined ? p.y :
                 p.yValue !== undefined ? p.yValue :
                 p.yvalue !== undefined ? p.yvalue : 0;

        return { x, y };
      });

      console.log('Получено точек графика:', formattedPoints.length);
      setGraphData(formattedPoints);

    } catch (error) {
      console.error('Ошибка загрузки графика:', error);

      // сообщение об ошибке
      let errorMessage = 'Не удалось построить график. ';

      if (error.message.includes('не содержит точек')) {
        errorMessage = 'Функция не содержит точек';
      } else if (error.response?.status === 500) {
        errorMessage += 'Ошибка сервера при построении графика';
      } else if (error.message) {
        errorMessage += error.message;
      }

      setGraphError(errorMessage);
      setGraphData([]);
    } finally {
      setIsLoadingGraph(false);
    }
  };

  const loadPoints = async (from = null, to = null) => {
    setIsLoadingPoints(true);

    try {
      // Проверяем что у нас есть functionId
      if (!func || !func.functionId) {
        throw new Error('Не указана функция');
      }

      let response;
      if (from !== null && to !== null) {
        response = await functionService.getFunctionPoints(
          func.functionId,
          from,
          to
        );
      } else {
        response = await functionService.getFunctionPoints(func.functionId);
      }

      if (response.data) {
        // Нормализуем данные
        const normalizedPoints = response.data.map(point => {
          const pointId = point.pointId || point.id;
          const xValue = point.xvalue !== undefined ? point.xvalue :
                        point.xValue !== undefined ? point.xValue :
                        point.x !== undefined ? point.x : null;
          const yValue = point.yvalue !== undefined ? point.yvalue :
                        point.yValue !== undefined ? point.yValue :
                        point.y !== undefined ? point.y : null;

          return { pointId, xValue, yValue };
        }).filter(point => point.pointId && point.xValue !== null && point.yValue !== null);

        setPoints(normalizedPoints);
      }
    } catch (error) {
      console.error('Error loading points:', error);
      notificationService.error('Ошибка загрузки точек');
      setPoints([]);
    } finally {
      setIsLoadingPoints(false);
    }
  };

  // Получение точек по диапазону
  const handleGetPointsByRange = () => {
    const from = xFrom ? parseFloat(xFrom) : null;
    const to = xTo ? parseFloat(xTo) : null;

    if (from !== null && to !== null && from >= to) {
      notificationService.error('Начало диапазона должно быть меньше конца');
      return;
    }

    loadPoints(from, to);
  };

  // Экспорт функции в файл
  const handleExportFunction = async () => {
    setIsExporting(true);
    try {
      const { functionData, blob, fileName } = await functionService.getExportData(func.functionId);

      // Создаем ссылку для скачивания
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();

      // Очистка
      setTimeout(() => {
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
      }, 100);

      notificationService.success(`Функция "${functionData.functionName}" сохранена как ${fileName}`);

    } catch (error) {
      console.error('Ошибка при сохранении файла:', error);

      let message = 'Ошибка при сохранении файла';
      if (error.response?.status === 404) {
        message = 'Функция не найдена';
      } else if (error.response?.status === 500) {
        message = 'Ошибка сервера при создании файла';
      } else if (error.message) {
        message = error.message;
      }

      notificationService.error(message);
    } finally {
      setIsExporting(false);
    }
  };

  // Форматирование даты и времени
  const formatDateTime = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleString('ru-RU', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  };

  if (!isOpen) return null;

  // Вычисляем диапазон для графика
  const xRange = graphData.length > 0
    ? { min: Math.min(...graphData.map(p => p.x)), max: Math.max(...graphData.map(p => p.x)) }
    : { min: -10, max: 10 };

  const yRange = graphData.length > 0
    ? { min: Math.min(...graphData.map(p => p.y)), max: Math.max(...graphData.map(p => p.y)) }
    : { min: -10, max: 10 };

  // Получаем логин владельца (если есть)
  const ownerLogin = func.ownerUsername || func.ownerName || `ID: ${func.ownerId}`;

  return (
    <div className="modal-overlay">
      <div className="modal-content function-modal">
        {/* Заголовок */}
        <div className="function-header">
          <div className="function-title-section">
            <h3 className="function-title">
              {func.functionName}
              <span className="public-badge">публичная</span>
            </h3>

            <div className="function-info-line">
              <span className="function-info-item">
                <strong>ID:</strong> #{func.functionId}
              </span>
              <span className="function-info-item">
                <strong>Владелец:</strong> {ownerLogin}
              </span>
              <span className="function-info-item">
                <strong>Создано:</strong> {formatDateTime(func.createdAt)}
              </span>
            </div>
          </div>

          <div className="function-header-right">
            <div className="public-toggle-container">
              <span className="public-label">Публичная:</span>
              <span className="public-status">✓</span>
            </div>

            <button className="modal-close" onClick={onClose} title="Закрыть">
              ×
            </button>
          </div>
        </div>

        {/* Вкладки */}
        <div className="function-tabs">
          <button
            className={`tab ${activeTab === 'graph' ? 'active' : ''}`}
            onClick={() => setActiveTab('graph')}
          >
            График
          </button>
          <button
            className={`tab ${activeTab === 'points' ? 'active' : ''}`}
            onClick={() => {
              setActiveTab('points');
              if (points.length === 0) loadPoints();
            }}
          >
            Точки функции
          </button>
        </div>

        {/* Контент вкладок */}
        <div className="function-content">
          {activeTab === 'graph' ? (
            <div className="graph-tab">
              {graphError ? (
                <div className="graph-error">
                  <p>{graphError}</p>
                </div>
              ) : (
                <GraphPreview
                  points={graphData}
                  title=""
                  xRange={xRange}
                  yRange={yRange}
                  isLoading={isLoadingGraph}
                  showTitle={false}
                  compact={true}
                />
              )}
            </div>
          ) : (
            <div className="points-tab">
              {/* Выбор вида отображения точек */}
              <div className="points-view-selector">
                <div className="radio-group horizontal">
                  <label className="radio-label">
                    <input
                      type="radio"
                      name="pointsView"
                      value="all"
                      checked={pointsView === 'all'}
                      onChange={(e) => {
                        setPointsView(e.target.value);
                        if (e.target.value === 'all') loadPoints();
                      }}
                    />
                    <span className="radio-text">Все точки</span>
                  </label>
                  <label className="radio-label">
                    <input
                      type="radio"
                      name="pointsView"
                      value="range"
                      checked={pointsView === 'range'}
                      onChange={(e) => setPointsView(e.target.value)}
                    />
                    <span className="radio-text">В диапазоне X</span>
                  </label>
                </div>
              </div>

              {/* Поля для диапазона */}
              {pointsView === 'range' && (
                <div className="range-inputs">
                  <div className="form-group">
                    <input
                      type="number"
                      className="form-input"
                      value={xFrom}
                      onChange={(e) => setXFrom(e.target.value)}
                      step="any"
                      placeholder="Начало диапазона X"
                    />
                  </div>
                  <div className="form-group">
                    <input
                      type="number"
                      className="form-input"
                      value={xTo}
                      onChange={(e) => setXTo(e.target.value)}
                      step="any"
                      placeholder="Конец диапазона X"
                    />
                  </div>
                  <button
                    className="btn-primary"
                    onClick={handleGetPointsByRange}
                    disabled={isLoadingPoints}
                  >
                    Получить точки
                  </button>
                </div>
              )}

              {/* Таблица точек */}
              <div className="points-table-container">
                {isLoadingPoints ? (
                  <div className="loading-points">
                    <div className="loading-spinner"></div>
                    <p>Загрузка точек...</p>
                  </div>
                ) : points.length === 0 ? (
                  <div className="empty-points">
                    <p>Точки не найдены</p>
                    <button
                      className="btn-primary"
                      onClick={() => loadPoints()}
                      style={{ marginTop: '10px' }}
                    >
                      Попробовать снова
                    </button>
                  </div>
                ) : (
                  <div className="points-table-wrapper">
                    <div className="points-count-info">
                      Найдено точек: {points.length}
                    </div>
                    <table className="points-table readonly-table">
                      <thead>
                        <tr>
                          <th>ID точки</th>
                          <th>X</th>
                          <th>Y</th>
                        </tr>
                      </thead>
                      <tbody>
                        {points.map(point => {
                          const pointId = point.pointId || point.id || 'N/A';
                          const xValue = point.xvalue !== undefined ? point.xvalue :
                                        point.xValue !== undefined ? point.xValue :
                                        point.x !== undefined ? point.x : 'N/A';
                          const yValue = point.yvalue !== undefined ? point.yvalue :
                                        point.yValue !== undefined ? point.yValue :
                                        point.y !== undefined ? point.y : 'N/A';

                          return (
                            <tr key={pointId}>
                              <td>#{pointId}</td>
                              <td>{typeof xValue === 'number' ? xValue.toFixed(4) : xValue}</td>
                              <td>{typeof yValue === 'number' ? yValue.toFixed(4) : yValue}</td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Футер с кнопками экспорта */}
        <div className="function-footer">
          <div className="export-buttons">
            <button
              className="btn-primary"
              onClick={handleExportFunction}
              disabled={isExporting}
              title="Сохранить функцию в формате JSON"
            >
              {isExporting ? 'Сохранение...' : 'Сохранить функцию как JSON'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default PublicFunctionModal;