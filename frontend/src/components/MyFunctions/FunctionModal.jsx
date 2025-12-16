import { useState, useEffect, useRef } from 'react';
import GraphPreview from '../Common/GraphPreview';
import functionService from '../../services/functionService';
import notificationService from '../../services/notificationService';
import "../../App.css";


const FunctionModal = ({ isOpen, onClose, function: func, onDelete, onUpdate }) => {
  const [activeTab, setActiveTab] = useState('graph');
  const [functionName, setFunctionName] = useState(func.functionName);
  const [isPublic, setIsPublic] = useState(func.isPublic || false);
  const [isEditingName, setIsEditingName] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

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

  // Состояния для добавления точки
  const [addPointModalOpen, setAddPointModalOpen] = useState(false);
  const [newPoint, setNewPoint] = useState({ x: '', y: '' });
  const [isAddingPoint, setIsAddingPoint] = useState(false);

  // Реф для автофокуса
  const nameInputRef = useRef(null);

  // Фокус на поле ввода при редактировании названия
  useEffect(() => {
    if (isEditingName && nameInputRef.current) {
      nameInputRef.current.focus();
      nameInputRef.current.select();
    }
  }, [isEditingName]);

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



  // Сохранение изменений
  const handleSaveChanges = async () => {
    if (!functionName.trim()) {
      notificationService.error('Введите название функции');
      return;
    }

    setIsSaving(true);
    try {
      const success = await onUpdate({
        functionName: functionName.trim(),
        isPublic
      });
      if (success) {
        setIsEditingName(false);
      }
    } catch (error) {
      // Ошибка обрабатывается в onUpdate
    } finally {
      setIsSaving(false);
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

  // Добавление точки
  // FunctionModal.jsx - найти функцию handleAddPointSubmit и исправить:

  const handleAddPointSubmit = async () => {
    if (!newPoint.x || !newPoint.y) {
      notificationService.error('Заполните оба значения');
      return;
    }

    const x = parseFloat(newPoint.x);
    const y = parseFloat(newPoint.y);

    if (isNaN(x) || isNaN(y)) {
      notificationService.error('Введите числовые значения');
      return;
    }

    setIsAddingPoint(true);
    try {
      // Отправляем данные с правильными названиями полей
      await functionService.addPoint(func.functionId, x, y);
      notificationService.success('Точка успешно добавлена');
      setAddPointModalOpen(false);
      setNewPoint({ x: '', y: '' });
      loadPoints(); // Обновляем список точек
    } catch (error) {
      console.error('Error adding point:', error);

      // Более информативное сообщение об ошибке
      let errorMessage = 'Ошибка при добавлении точки';
      if (error.response?.data?.message) {
        errorMessage += `: ${error.response.data.message}`;
      } else if (error.message) {
        errorMessage += `: ${error.message}`;
      }

      notificationService.error(errorMessage);
    } finally {
      setIsAddingPoint(false);
    }
  };


  // Удаление точки
  const handleDeletePoint = async (pointId) => {
    const confirmed = window.confirm('Удалить эту точку?');
    if (!confirmed) return;

    try {
      await functionService.deletePoint(pointId);
      notificationService.success('Точка удалена');
      loadPoints(); // Обновляем список точек
    } catch (error) {
      console.error('Error deleting point:', error);
      notificationService.error('Ошибка при удалении точки');
    }
  };

  // Экспорт функции
  const handleExportFunction = async () => {
    try {
      // Формируем имя файла
      const safeName = func.functionName
        .replace(/[^a-zA-Z0-9а-яА-ЯёЁ\s\-_]/g, '') // Убираем спецсимволы
        .replace(/\s+/g, '_') // Пробелы в подчеркивания
        .trim();

      const filename = `${safeName || 'function'}.bin`;

      console.log('Экспорт функции в .bin:', func.functionId);

      // Запрашиваем файл
      const response = await functionService.exportFunction(func.functionId, 'binary');

      // Проверяем что получили данные
      if (!response.data || response.data.size === 0) {
        throw new Error('Получен пустой файл');
      }

      console.log('Размер файла:', response.data.size, 'байт');

      // Создаем ссылку для скачивания
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();

      // Очистка
      setTimeout(() => {
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
      }, 100);

      notificationService.success(`Функция сохранена как ${filename}`);

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

  return (
    <div className="modal-overlay">
      <div className="modal-content function-modal">
        {/* Заголовок */}
        <div className="function-header">
          <div className="function-title-section">
            {isEditingName ? (
              <div className="name-editor">
                <input
                  ref={nameInputRef}
                  type="text"
                  className="name-input"
                  value={functionName}
                  onChange={(e) => setFunctionName(e.target.value)}
                  disabled={isSaving}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') handleSaveChanges();
                    if (e.key === 'Escape') {
                      setIsEditingName(false);
                      setFunctionName(func.functionName);
                    }
                  }}
                />
                <button
                  className="btn-save-name"
                  onClick={handleSaveChanges}
                  disabled={isSaving || !functionName.trim()}
                >
                  Сохранить
                </button>
                <button
                  className="btn-cancel-name"
                  onClick={() => {
                    setIsEditingName(false);
                    setFunctionName(func.functionName);
                  }}
                  disabled={isSaving}
                >
                  Отмена
                </button>
              </div>
            ) : (
              <h3
                className="function-title"
                onClick={() => setIsEditingName(true)}
                title="Нажмите для редактирования"
              >
                {func.functionName}
              </h3>
            )}

            <div className="function-info-line">
              <span className="function-info-item">
                <strong>ID:</strong> #{func.functionId}
              </span>
              <span className="function-info-item">
                <strong>Владелец:</strong> Вы
              </span>
              <span className="function-info-item">
                <strong>Создано:</strong> {formatDateTime(func.createdAt)}
              </span>
            </div>
          </div>

          <div className="function-header-right">
            <div className="public-toggle-container">
              <span className="public-label">Публичная:</span>
              <label className="switch">
                <input
                  type="checkbox"
                  checked={isPublic}
                  onChange={(e) => {
                    setIsPublic(e.target.checked);
                    onUpdate({ isPublic: e.target.checked });
                  }}
                />
                <span className="slider"></span>
              </label>
            </div>

            <div className="function-header-actions">
              <button
                className="btn-action delete-function"
                onClick={() => onDelete(func.functionId, func.functionName)}
                title="Удалить функцию"
              >
                Удалить функцию
              </button>
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
                <div className="points-table-header">
                  <button
                    className="btn-primary"
                    onClick={() => setAddPointModalOpen(true)}
                  >
                    Добавить точку
                  </button>
                </div>

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
                    <table className="points-table">
                      <thead>
                        <tr>
                          <th>ID точки</th>
                          <th>X</th>
                          <th>Y</th>
                          <th className="actions-column">Действия</th>
                        </tr>
                      </thead>
                      <tbody>
                        {points.map(point => {
                          // используем правильные названия полей из API
                          const pointId = point.pointId || point.id || 'N/A';
                          // Пробуем разные варианты названий полей
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
                              <td className="actions-column">
                                <button
                                  className="btn-action delete-point"
                                  onClick={() => handleDeletePoint(pointId)}
                                  title="Удалить точку"
                                >
                                  Удалить
                                </button>
                              </td>
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
              title="Сохранить функцию в файл .bin"
            >
              Сохранить как .bin
            </button>

          </div>
        </div>
      </div>

      {/* Модальное окно добавления точки */}
      {addPointModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content add-point-modal">
            <div className="modal-header">
              <h3 className="modal-title">Добавить точку</h3>
              <button
                className="modal-close"
                onClick={() => {
                  setAddPointModalOpen(false);
                  setNewPoint({ x: '', y: '' });
                }}
              >
                ×
              </button>
            </div>

            <div className="form-group">
              <label className="form-label">Значение X</label>
              <input
                type="number"
                className="form-input"
                value={newPoint.x}
                onChange={(e) => setNewPoint({ ...newPoint, x: e.target.value })}
                placeholder="Введите X"
                step="any"
                autoFocus
              />
            </div>

            <div className="form-group">
              <label className="form-label">Значение Y</label>
              <input
                type="number"
                className="form-input"
                value={newPoint.y}
                onChange={(e) => setNewPoint({ ...newPoint, y: e.target.value })}
                placeholder="Введите Y"
                step="any"
              />
            </div>

            <div className="modal-buttons">
              <button
                className="btn-secondary"
                onClick={() => {
                  setAddPointModalOpen(false);
                  setNewPoint({ x: '', y: '' });
                }}
                disabled={isAddingPoint}
              >
                Отмена
              </button>
              <button
                className="btn-primary"
                onClick={handleAddPointSubmit}
                disabled={isAddingPoint || !newPoint.x || !newPoint.y}
              >
                {isAddingPoint ? 'Добавление...' : 'Добавить'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default FunctionModal;