import { useState, useEffect } from 'react';
import FunctionParamsModal from './FunctionParamsModal';
import GraphModal from './GraphModal';
import functionService from '../../services/functionService';
import '../../App.css';

const CreateFromFunctionModal = ({ isOpen, onClose, onFunctionCreated, isLoading = false }) => {
  const [functionName, setFunctionName] = useState('');
  const [selectedFunctionKey, setSelectedFunctionKey] = useState('');
  const [leftX, setLeftX] = useState('');
  const [rightX, setRightX] = useState('');
  const [pointsCount, setPointsCount] = useState('');
  const [isPublic, setIsPublic] = useState(false);
  const [functionParams, setFunctionParams] = useState(null);

  const [isParamsModalOpen, setIsParamsModalOpen] = useState(false);
  const [availableFunctions, setAvailableFunctions] = useState([]);
  const [isLoadingFunctions, setIsLoadingFunctions] = useState(false);
  const [error, setError] = useState('');
  const [isGraphModalOpen, setIsGraphModalOpen] = useState(false);
  const [graphData, setGraphData] = useState([]);
  const [isGeneratingGraph, setIsGeneratingGraph] = useState(false);
  const [isCreating, setIsCreating] = useState(false);

  // Загружаем функции при открытии
  useEffect(() => {
    if (isOpen) {
      loadAvailableFunctions();
      resetForm();
    }
  }, [isOpen]);

  const loadAvailableFunctions = async () => {
    setIsLoadingFunctions(true);
    try {
      // Пробуем получить функции с сервера
      const allFunctions = await functionService.getAvailableMathFunctions();

      // Если сервер вернул функции, используем их
      if (allFunctions && Array.isArray(allFunctions) && allFunctions.length > 0) {
        setAvailableFunctions(allFunctions);
      } else {
        // Иначе используем дефолтные
        setAvailableFunctions([
          { key: 'SQR_FUNCTION', name: 'Квадратичная функция', type: 'BASIC' },
          { key: 'IDENTITY_FUNCTION', name: 'Тождественная функция', type: 'BASIC' },
          { key: 'CONSTANT_FUNCTION', name: 'Постоянная функция', type: 'BASIC', requiresValue: true },
          { key: 'ZERO_FUNCTION', name: 'Нулевая функция', type: 'BASIC' },
          { key: 'SIN_FUNCTION', name: 'Синусоидальная функция', type: 'BASIC' },
          { key: 'COS_FUNCTION', name: 'Косинусоидальная функция', type: 'BASIC' },
          { key: 'EXP_FUNCTION', name: 'Экспоненциальная функция', type: 'BASIC' },
          { key: 'LOG_FUNCTION', name: 'Логарифмическая функция', type: 'BASIC' },
          { key: 'ABS_FUNCTION', name: 'Модуль', type: 'BASIC' },
          { key: 'BSPLINE_FUNCTION', name: 'B-сплайн функция', type: 'BASIC', requiresParams: true },
        ]);
      }
    } catch (error) {
      console.error('Ошибка загрузки функций:', error);
      // Используем дефолтные в случае ошибки
      setAvailableFunctions([
        { key: 'SQR_FUNCTION', name: 'Квадратичная функция', type: 'BASIC' },
        { key: 'IDENTITY_FUNCTION', name: 'Тождественная функция', type: 'BASIC' },
        { key: 'CONSTANT_FUNCTION', name: 'Постоянная функция', type: 'BASIC', requiresValue: true },
        { key: 'ZERO_FUNCTION', name: 'Нулевая функция', type: 'BASIC' },
      ]);
    } finally {
      setIsLoadingFunctions(false);
    }
  };

  const resetForm = () => {
    setFunctionName('');
    setSelectedFunctionKey('');
    setLeftX('');
    setRightX('');
    setPointsCount('');
    setIsPublic(false);
    setFunctionParams(null);
    setError('');
    setGraphData([]);
  };

  const handleFunctionSelect = (e) => {
    const value = e.target.value;
    console.log('Выбрана функция (handleFunctionSelect):', value);
    setSelectedFunctionKey(value);

    if (value) {
      // Проверяем, нужны ли параметры
      const func = availableFunctions.find(f => f.key === value || f.type === value);
      console.log('Найденная функция:', func);

      if (func?.requiresValue) {
        // Для постоянной функции сразу запросим значение
        const constantValue = prompt('Введите значение постоянной функции:', '1');
        if (constantValue !== null && constantValue !== '') {
          const numValue = parseFloat(constantValue);
          if (!isNaN(numValue)) {
            setFunctionParams({ constantValue: numValue });
          } else {
            setError('Введите числовое значение');
            setSelectedFunctionKey('');
          }
        } else {
          // Если пользователь отменил, сбрасываем выбор
          setSelectedFunctionKey('');
        }
      } else if (func?.requiresParams) {
        // Для функций со сложными параметрами откроем модалку
        setIsParamsModalOpen(true);
      } else {
        // Для остальных функций сбрасываем параметры
        setFunctionParams(null);
      }
    }
  };

  const handleParamsConfirm = (params) => {
    console.log('Параметры подтверждены:', params);
    setFunctionParams(params);
    setIsParamsModalOpen(false);
  };

  const handleCreate = async () => {
    setError('');

    // Базовая валидация
    if (!functionName.trim()) {
      setError('Введите название функции');
      return;
    }

    if (!selectedFunctionKey) {
      setError('Выберите математическую функцию');
      return;
    }

    if (!leftX || !rightX) {
      setError('Введите интервал');
      return;
    }

    const left = parseFloat(leftX);
    const right = parseFloat(rightX);
    const count = parseInt(pointsCount) || 100;

    if (isNaN(left) || isNaN(right)) {
      setError('Интервал должен содержать числа');
      return;
    }

    if (left >= right) {
      setError('Начало интервала должно быть меньше конца');
      return;
    }

    if (count < 2) {
      setError('Количество точек должно быть не менее 2');
      return;
    }

    // Для постоянной функции проверяем параметры
    const func = availableFunctions.find(f => f.key === selectedFunctionKey || f.type === selectedFunctionKey);
    if (func?.requiresValue && !functionParams?.constantValue) {
      setError('Введите значение постоянной функции');
      return;
    }

    if (func?.requiresParams && !functionParams) {
      setError('Заполните параметры функции');
      return;
    }

    setIsCreating(true);

    try {
      // Подготавливаем данные для API
      const requestData = {
        name: functionName.trim(),
        mathFunctionType: selectedFunctionKey,
        xFrom: left,
        xTo: right,
        pointsCount: count,
        isPublic: isPublic,
        factoryType: 'ARRAY'
      };

      // Добавляем специфичные параметры
      if (selectedFunctionKey === 'CONSTANT_FUNCTION' && functionParams?.constantValue) {
        requestData.constantValue = functionParams.constantValue;
      } else if (selectedFunctionKey === 'BSPLINE_FUNCTION' && functionParams) {
        // Для B-сплайна передаем все параметры
        Object.assign(requestData, functionParams);
      }

      console.log('Отправка данных на сервер:', requestData);

      const response = await functionService.createFromMathFunction(requestData);

      console.log('Ответ сервера:', response);

      if (response.data) {
        console.log('Функция успешно создана:', response.data);

        // Пытаемся получить точки функции
        let points = [];
        try {
          const functionId = response.data.functionId || response.data.id;
          if (functionId) {
            const pointsResponse = await functionService.getFunctionPoints(functionId);
            points = functionService.formatPointsForGraph(pointsResponse.data || []);
          }
        } catch (pointsError) {
          console.error('Ошибка получения точек:', pointsError);
          // Если не удалось получить точки, создаем фиктивные для графика
          const step = (right - left) / (count - 1);
          for (let i = 0; i < count; i++) {
            const x = left + step * i;
            points.push({ x, y: x }); // По умолчанию тождественная функция
          }
        }

        // Вызываем колбэк с данными
        onFunctionCreated(response, points);

        // Закрываем модалку
        resetForm();
        onClose();
      } else {
        throw new Error('Функция не была создана: нет данных в ответе');
      }
    } catch (error) {
      console.error('Ошибка создания функции:', error);

      let errorMessage = 'Ошибка создания функции';
      if (error.response?.status === 400) {
        errorMessage = 'Некорректные параметры функции';
      } else if (error.response?.status === 401) {
        errorMessage = 'Недостаточно прав для создания функции';
      } else if (error.response?.data?.message) {
        errorMessage = error.response.data.message;
      } else if (error.response?.data?.error) {
        errorMessage = error.response.data.error;
      } else if (error.message) {
        errorMessage = error.message;
      }

      setError(errorMessage);
    } finally {
      setIsCreating(false);
    }
  };

  const handleCancel = () => {
    resetForm();
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay">
      <div className="modal-content" style={{ maxWidth: '600px', maxHeight: '90vh', overflowY: 'auto' }}>
        <div className="modal-header">
          <h3 className="modal-title">Создать функцию из математической функции</h3>
          <button className="modal-close" onClick={onClose}>×</button>
        </div>

        <div className="form-section">
          {error && (
            <div className="server-message error" style={{ marginBottom: '15px' }}>
              <strong>Ошибка:</strong> {error}
            </div>
          )}

          {/* Название функции */}
          <div className="form-group">
            <label className="form-label">Название функции *</label>
            <input
              type="text"
              className="form-input"
              value={functionName}
              onChange={(e) => {
                setFunctionName(e.target.value);
                if (error) setError('');
              }}
              placeholder="Например: Моя функция на [-10, 10]"
              disabled={isCreating || isLoading}
            />
          </div>

          {/* Выбор функции */}
          <div className="form-group">
            <label className="form-label">
              Математическая функция *
              {isLoadingFunctions && (
                <span style={{ color: '#666', marginLeft: '10px' }}>(загрузка...)</span>
              )}
            </label>
            <select
              className="form-input"
              value={selectedFunctionKey}
              onChange={handleFunctionSelect}
              disabled={isCreating || isLoading || isLoadingFunctions}
              style={{
                padding: '12px',
                fontSize: '14px',
                cursor: 'pointer'
              }}
            >
              <option value="">-- Выберите функцию --</option>
              {availableFunctions.map((func, index) => (
                <option
                  key={func.key || func.type || index}
                  value={func.key || func.type}
                >
                  {func.name}
                  {func.requiresValue && ' 🔢'}
                  {func.requiresParams && ' ⚙️'}
                </option>
              ))}
            </select>
            <div style={{ display: 'flex', gap: '15px', marginTop: '5px' }}>
              <small style={{ color: '#666' }}>
                🔢 - требует значение константы
              </small>
              <small style={{ color: '#666' }}>
                ⚙️ - требует параметры
              </small>
            </div>
          </div>

          {/* Отображение параметров */}
          {selectedFunctionKey && functionParams && (
            <div className="form-group">
              <div className="server-message" style={{
                background: 'rgba(76, 175, 80, 0.1)',
                border: '1px solid #4CAF50',
                padding: '12px',
                borderRadius: '6px'
              }}>
                <strong>Параметры функции:</strong>
                <div style={{ marginTop: '8px' }}>
                  {functionParams.constantValue !== undefined && (
                    <div>Константа: {functionParams.constantValue}</div>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* Интервал */}
          <div style={{ display: 'flex', gap: '15px', marginBottom: '20px' }}>
            <div className="form-group" style={{ flex: 1 }}>
              <label className="form-label">Начало интервала (X от) *</label>
              <input
                type="number"
                className="form-input"
                value={leftX}
                onChange={(e) => {
                  setLeftX(e.target.value);
                  if (error) setError('');
                }}
                placeholder="-10"
                disabled={isCreating || isLoading}
                step="any"
              />
            </div>
            <div className="form-group" style={{ flex: 1 }}>
              <label className="form-label">Конец интервала (X до) *</label>
              <input
                type="number"
                className="form-input"
                value={rightX}
                onChange={(e) => {
                  setRightX(e.target.value);
                  if (error) setError('');
                }}
                placeholder="10"
                disabled={isCreating || isLoading}
                step="any"
              />
            </div>
          </div>

          {/* Количество точек */}
          <div className="form-group">
            <label className="form-label">Количество точек *</label>
            <input
              type="number"
              className="form-input"
              value={pointsCount}
              onChange={(e) => {
                setPointsCount(e.target.value);
                if (error) setError('');
              }}
              placeholder="100"
              disabled={isCreating || isLoading}
              min="2"
              max="10000"
            />
            <small style={{ display: 'block', marginTop: '5px', color: '#666' }}>
              Чем больше точек, тем точнее аппроксимация (рекомендуется 50-1000)
            </small>
          </div>

          {/* Публичность */}
          <div className="form-group">
            <div className="switch-container">
              <label className="switch">
                <input
                  type="checkbox"
                  checked={isPublic}
                  onChange={(e) => setIsPublic(e.target.checked)}
                  disabled={isCreating || isLoading}
                />
                <span className="slider"></span>
              </label>
              <span className="switch-label">Сделать публичной (видно всем пользователям)</span>
            </div>
          </div>
        </div>

        {/* Кнопки */}
        <div className="modal-buttons" style={{ marginTop: '25px' }}>
          <button
            className="btn-secondary"
            onClick={handleCancel}
            disabled={isCreating || isLoading}
            style={{ padding: '12px 24px' }}
          >
            Отмена
          </button>
          <button
            className="btn-primary"
            onClick={handleCreate}
            disabled={isCreating || isLoading || !functionName || !selectedFunctionKey || !leftX || !rightX}
            style={{ padding: '12px 24px' }}
          >
            {isCreating ? (
              <>
                <span className="loading-spinner small" style={{ marginRight: '8px' }}></span>
                Создание...
              </>
            ) : 'Создать функцию'}
          </button>
        </div>

        {/* Модальное окно параметров */}
        <FunctionParamsModal
          isOpen={isParamsModalOpen}
          functionName={availableFunctions.find(f => f.key === selectedFunctionKey || f.type === selectedFunctionKey)?.name || ''}
          functionKey={selectedFunctionKey}
          onClose={() => setIsParamsModalOpen(false)}
          onConfirm={handleParamsConfirm}
          isLoading={isCreating || isLoading}
        />

        {/* Модальное окно графика */}
        <GraphModal
          isOpen={isGraphModalOpen}
          onClose={() => setIsGraphModalOpen(false)}
          points={graphData}
          title="Предварительный просмотр"
        />
      </div>
    </div>
  );
};

export default CreateFromFunctionModal;