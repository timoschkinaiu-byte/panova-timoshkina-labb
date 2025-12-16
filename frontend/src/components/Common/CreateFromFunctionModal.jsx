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

  // Состояния для модальных окон
  const [isParamsModalOpen, setIsParamsModalOpen] = useState(false);

  // Состояния для доступных функций
  const [availableFunctions, setAvailableFunctions] = useState([]);
  const [isLoadingFunctions, setIsLoadingFunctions] = useState(false);
  const [error, setError] = useState('');

  // Состояния для графика
  const [isGraphModalOpen, setIsGraphModalOpen] = useState(false);
  const [graphData, setGraphData] = useState([]);
  const [isGeneratingGraph, setIsGeneratingGraph] = useState(false);

  const [isCreating, setIsCreating] = useState(false);

  // Загрузка доступных функций
  useEffect(() => {
    if (isOpen) {
      loadAvailableFunctions();
    }
  }, [isOpen]);

  const loadAvailableFunctions = async () => {
    setIsLoadingFunctions(true);
    try {
      // Получаем все доступные функции
      const allFunctions = await functionService.getAvailableFunctions();
      setAvailableFunctions(allFunctions);
    } catch (error) {
      console.error('Error loading available functions:', error);
      // Используем только базовые функции в случае ошибки
      setAvailableFunctions(functionService.getAvailableMathFunctions());
    } finally {
      setIsLoadingFunctions(false);
    }
  };

  const handleFunctionSelect = (functionKey) => {
    setSelectedFunctionKey(functionKey);
    setFunctionParams(null);

    // Проверяем, нужны ли параметры для этой функции
    const func = availableFunctions.find(f => f.key === functionKey);
    if (func?.requiresParams || func?.requiresValue) {
      setIsParamsModalOpen(true);
    }
  };

  const handleParamsConfirm = (params) => {
    setFunctionParams(params);
    setIsParamsModalOpen(false);
  };

  // Функция для предварительного просмотра графика
  const generatePreviewGraph = async () => {
    setError('');

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
    const count = pointsCount ? parseInt(pointsCount) : 100;

    if (isNaN(left) || isNaN(right)) {
      setError('Интервал должен содержать числа');
      return;
    }

    if (left >= right) {
      setError('Левый край должен быть меньше правого');
      return;
    }

    if (count < 2) {
      setError('Количество точек должно быть не менее 2');
      return;
    }

    // Проверка параметров
    const func = availableFunctions.find(f => f.key === selectedFunctionKey);
    if ((func?.requiresParams || func?.requiresValue) && !functionParams) {
      setError('Заполните параметры функции');
      return;
    }

    setIsGeneratingGraph(true);

    try {
      // Подготавливаем данные для временной функции
      const requestData = {
        name: `Preview: ${selectedFunctionKey}`,
        sourceFunctionKey: functionParams?.functionKey || selectedFunctionKey,
        leftX: left,
        rightX: right,
        pointsCount: count,
        isPublic: false
      };

      // Добавляем параметры если есть
      if (functionParams?.params) {
        Object.assign(requestData, functionParams.params);
      }

      // Создаем временную функцию
      const response = await functionService.createFromMathFunction(requestData);

      if (!response.data?.functionId) {
        throw new Error('Не удалось получить ID функции');
      }

      const functionId = response.data.functionId;

      // Получаем точки для графика
      const pointsResponse = await functionService.getFunctionPoints(functionId);
      const points = pointsResponse?.data || [];

      // Преобразуем точки в формат для графика
      const formattedPoints = points.map(point => {
        const x = point.xvalue !== undefined ? point.xvalue :
                  point.xValue !== undefined ? point.xValue :
                  point.x !== undefined ? point.x : 0;

        const y = point.yvalue !== undefined ? point.yvalue :
                  point.yValue !== undefined ? point.yValue :
                  point.y !== undefined ? point.y : 0;

        return { x, y };
      });

      // Если точек нет, создаем фиктивные
      if (!formattedPoints.length) {
        for (let i = 0; i < count; i++) {
          const x = left + ((right - left) / (count - 1)) * i;
          formattedPoints.push({ x, y: 0 });
        }
      }

      setGraphData(formattedPoints);
      setIsGraphModalOpen(true);

      // Удаляем временную функцию через 5 секунд
      setTimeout(() => {
        functionService.deleteFunction(functionId).catch(console.error);
      }, 5000);

    } catch (error) {
      console.error('Error generating preview graph:', error);

      let errorMessage = 'Ошибка при построении графика';
      if (error.response?.status === 400) {
        errorMessage = 'Некорректные параметры функции';
      } else if (error.response?.data?.message) {
        errorMessage = error.response.data.message;
      }

      setError(errorMessage);
    } finally {
      setIsGeneratingGraph(false);
    }
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
    const count = parseInt(pointsCount);

    if (isNaN(left) || isNaN(right)) {
      setError('Интервал должен содержать числа');
      return;
    }

    if (left >= right) {
      setError('Левый край должен быть меньше правого');
      return;
    }

    if (!pointsCount || count < 2) {
      setError('Количество точек должно быть не менее 2');
      return;
    }

    // Проверка параметров
    const func = availableFunctions.find(f => f.key === selectedFunctionKey);
    if ((func?.requiresParams || func?.requiresValue) && !functionParams) {
      setError('Заполните параметры функции');
      return;
    }

    setIsCreating(true);

    try {
      // Подготавливаем данные
      const requestData = {
        name: functionName,
        sourceFunctionKey: functionParams?.functionKey || selectedFunctionKey,
        leftX: left,
        rightX: right,
        pointsCount: count,
        isPublic: isPublic,
        factoryType: 'ARRAY'
      };

      // Добавляем параметры если есть
      if (functionParams?.params) {
        Object.assign(requestData, functionParams.params);
      }

      console.log('Отправляемые данные:', requestData);

      const response = await functionService.createFromMathFunction(requestData);

      if (!response.data?.functionId) {
        throw new Error('Функция не была создана на сервере');
      }

      const functionId = response.data.functionId;

      // Получаем точки созданной функции
      const pointsResponse = await functionService.getFunctionPoints(functionId);
      const points = pointsResponse?.data || [];

      // Формируем объект функции с точками
      const numericPoints = points.map(p => ({
        x: p.xvalue ?? p.xValue ?? p.x,
        y: p.yvalue ?? p.yValue ?? p.y
      }));

      onFunctionCreated(response, numericPoints);

      // Передаем созданную функцию


      // Очистка формы
      resetForm();
      onClose();

    } catch (error) {
      console.error('Error creating function:', error);

      let errorMessage = 'Ошибка создания функции';
      if (error.response?.status === 400) {
        errorMessage = 'Некорректные параметры функции';
      } else if (error.response?.data?.message) {
        errorMessage = error.response.data.message;
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

  if (!isOpen) return null;

  // Проверка, можно ли построить график
  const canShowGraph = selectedFunctionKey && leftX && rightX && pointsCount;

  return (
    <div className="modal-overlay">
      <div className="modal-content" style={{ maxWidth: '600px' }}>
        <div className="modal-header">
          <h3 className="modal-title">Создать функцию из другой функции</h3>
          <p className="modal-description">Выберите функцию и укажите интервал для табулирования</p>
        </div>

        <div className="form-section">
          {error && (
            <div className="server-message error" style={{ marginBottom: '20px' }}>
              {error}
            </div>
          )}

          {/* Название функции и публичность */}
          <div className="form-row" style={{ display: 'flex', gap: '20px', marginBottom: '20px' }}>
            <div className="form-group" style={{ flex: 1 }}>
              <label className="form-label">Название функции</label>
              <input
                type="text"
                className="form-input"
                value={functionName}
                onChange={(e) => {
                  setFunctionName(e.target.value);
                  if (error) setError('');
                }}
                placeholder="Введите название функции"
                disabled={isCreating || isLoading}
              />
            </div>

            <div className="form-group">
              <label className="form-label">Публичность</label>
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
                <span className="switch-label">Сделать публичной</span>
              </div>
            </div>
          </div>

          {/* Выбор функции */}
          <div className="form-group" style={{ marginBottom: '20px' }}>
            <label className="form-label">
              Математическая функция
              {isLoadingFunctions && (
                <span style={{ color: 'var(--text-secondary)', marginLeft: '10px' }}>
                  (загрузка...)
                </span>
              )}
            </label>
            <select
              className="form-input"
              value={selectedFunctionKey}
              onChange={(e) => handleFunctionSelect(e.target.value)}
              disabled={isCreating || isLoadingFunctions || isLoading}
            >
              <option value="">Выберите функцию...</option>

              {/* Базовые функции */}
              <optgroup label="Базовые функции">
                {availableFunctions
                  .filter(f => f.type === 'BASIC')
                  .map(func => (
                    <option key={func.key} value={func.key}>
                      {func.name}
                      {func.requiresParams && ' ⚙️'}
                      {func.requiresValue && ' 🔢'}
                    </option>
                  ))}
              </optgroup>

              {/* Мои простые функции */}
              <optgroup label="Мои функции">
                {availableFunctions
                  .filter(f => f.type === 'USER' && !f.isComposite)
                  .map(func => (
                    <option key={func.key} value={func.key}>
                      {func.name} (ID: {func.functionId})
                    </option>
                  ))}
              </optgroup>

              {/* Сложные функции */}
              <optgroup label="Сложные функции">
                {availableFunctions
                  .filter(f => f.isComposite)
                  .map(func => (
                    <option key={func.key} value={func.key}>
                      {func.name} (Composite)
                    </option>
                  ))}
              </optgroup>
            </select>
            <small style={{
              color: 'var(--text-secondary)',
              display: 'block',
              marginTop: '5px'
            }}>
              ⚙️ - требует параметры, 🔢 - требует значение
            </small>
          </div>

          {/* Параметры интервала */}
          <div className="form-row" style={{
            display: 'flex',
            gap: '15px',
            marginBottom: '20px',
            flexWrap: 'wrap'
          }}>
            <div className="form-group" style={{ flex: 1, minWidth: '150px' }}>
              <label className="form-label">Начало интервала</label>
              <input
                type="number"
                className="form-input"
                value={leftX}
                onChange={(e) => {
                  setLeftX(e.target.value);
                  if (error) setError('');
                }}
                placeholder="Например: -10"
                disabled={isCreating || isLoading}
                step="any"
              />
            </div>

            <div className="form-group" style={{ flex: 1, minWidth: '150px' }}>
              <label className="form-label">Конец интервала</label>
              <input
                type="number"
                className="form-input"
                value={rightX}
                onChange={(e) => {
                  setRightX(e.target.value);
                  if (error) setError('');
                }}
                placeholder="Например: 10"
                disabled={isCreating || isLoading}
                step="any"
              />
            </div>

            <div className="form-group" style={{ flex: 1, minWidth: '150px' }}>
              <label className="form-label">Количество точек</label>
              <input
                type="number"
                className="form-input"
                value={pointsCount}
                onChange={(e) => {
                  setPointsCount(e.target.value);
                  if (error) setError('');
                }}
                placeholder="Например: 100"
                disabled={isCreating || isLoading}
                min="2"
              />
            </div>
          </div>

          {/* Кнопка построения графика */}
          {canShowGraph && (
            <div className="form-group" style={{ marginTop: '20px', textAlign: 'center' }}>
              <button
                className="btn-secondary"
                onClick={generatePreviewGraph}
                disabled={isGeneratingGraph || isCreating || isLoading}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  margin: '0 auto',
                  padding: '10px 20px'
                }}
              >
                {isGeneratingGraph ? (
                  <>
                    <div className="loading-spinner" style={{ width: '16px', height: '16px' }}></div>
                    Построение графика...
                  </>
                ) : (
                  <>
                    📊 Предварительный просмотр графика
                  </>
                )}
              </button>
              <p style={{
                fontSize: '12px',
                color: 'var(--text-secondary)',
                marginTop: '5px'
              }}>
                Покажет график выбранной функции в заданном интервале
              </p>
            </div>
          )}

          {/* Информационное сообщение */}
          <div className="form-group">
            <div className="server-message" style={{
              background: 'rgba(128, 0, 0, 0.1)',
              border: '1px solid rgba(128, 0, 0, 0.3)',
              color: 'var(--text-primary)',
              padding: '15px',
              borderRadius: '8px',
              marginTop: '10px'
            }}>
              <strong>Будет создана табулированная функция:</strong><br />
              <code style={{ fontSize: '14px', display: 'block', marginTop: '10px' }}>
                • На основе выбранной функции<br />
                • В интервале [{leftX || '?'}, {rightX || '?'}]<br />
                • С {pointsCount || '?'} точками
              </code>
            </div>
          </div>
        </div>

        {/* Кнопки действий */}
        <div className="modal-buttons" style={{ marginTop: '30px' }}>
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
            disabled={isCreating || isLoading || !selectedFunctionKey}
            style={{ padding: '12px 24px' }}
          >
            {isCreating ? 'Создание...' : 'Создать функцию'}
          </button>
        </div>

        {/* Модальное окно параметров */}
        <FunctionParamsModal
          isOpen={isParamsModalOpen}
          functionName={availableFunctions.find(f => f.key === selectedFunctionKey)?.name || ''}
          functionKey={selectedFunctionKey}
          onClose={() => setIsParamsModalOpen(false)}
          onConfirm={handleParamsConfirm}
          isLoading={isCreating || isLoading}
        />

        {/* Модальное окно графика */}
        <GraphModal
          isOpen={isGraphModalOpen}
          onClose={() => {
            setIsGraphModalOpen(false);
            setGraphData([]);
          }}
          points={graphData}
          title={`${selectedFunctionKey} в интервале [${leftX}, ${rightX}]`}
        />
      </div>
    </div>
  );
};

export default CreateFromFunctionModal;