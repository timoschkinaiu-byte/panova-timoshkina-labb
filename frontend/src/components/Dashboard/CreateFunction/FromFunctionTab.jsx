import { useState, useEffect } from 'react';
import FunctionParamsModal from '../../Common/FunctionParamsModal';
import GraphModal from '../../Common/GraphModal';
import functionService from '../../../services/functionService';
import authService from '../../../services/auth';
import '../../../App.css';

const FromFunctionTab = ({
  functionName,
  setFunctionName,
  isPublic,
  isCreating,
  setIsCreating,
  onSuccess,
  onError
}) => {
  const [selectedFunctionKey, setSelectedFunctionKey] = useState('');
  const [leftX, setLeftX] = useState('');
  const [rightX, setRightX] = useState('');
  const [pointsCount, setPointsCount] = useState('');
  const [isParamsModalOpen, setIsParamsModalOpen] = useState(false);
  const [functionParams, setFunctionParams] = useState(null);

  // Состояния для графика
  const [isGraphModalOpen, setIsGraphModalOpen] = useState(false);
  const [graphData, setGraphData] = useState([]);
  const [isGeneratingGraph, setIsGeneratingGraph] = useState(false);

  // Состояния для доступных функций
  const [availableFunctions, setAvailableFunctions] = useState([]);
  const [isLoadingFunctions, setIsLoadingFunctions] = useState(false);

  // Загрузка доступных функций
  useEffect(() => {
    loadAvailableFunctions();
  }, []);

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
    if (!selectedFunctionKey) {
      onError('Выберите математическую функцию');
      return;
    }

    if (!leftX || !rightX) {
      onError('Введите интервал');
      return;
    }

    const left = parseFloat(leftX);
    const right = parseFloat(rightX);
    if (isNaN(left) || isNaN(right)) {
      onError('Интервал должен содержать числа');
      return;
    }

    if (left >= right) {
      onError('Левый край должен быть меньше правого');
      return;
    }

    const count = pointsCount ? parseInt(pointsCount) : 100;
    if (count < 2) {
      onError('Количество точек должно быть не менее 2');
      return;
    }

    // Проверка параметров
    const func = availableFunctions.find(f => f.key === selectedFunctionKey);
    if ((func?.requiresParams || func?.requiresValue) && !functionParams) {
      onError('Заполните параметры функции');
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

      if (response.status === 201 && response.data?.functionId) {
        const functionId = response.data.functionId;

        // Получаем график
        const graphResponse = await functionService.getGraphData(
          functionId,
          count,
          left,
          right
        );

        if (graphResponse.data?.points) {
          setGraphData(graphResponse.data.points);
          setIsGraphModalOpen(true);

          // Удаляем временную функцию через 5 секунд
          setTimeout(() => {
            functionService.deleteFunction(functionId).catch(console.error);
          }, 5000);
        } else {
          onError('Не удалось получить данные графика');
        }
      }
    } catch (error) {
      console.error('Error generating preview graph:', error);

      let errorMessage = 'Ошибка при построении графика';
      if (error.response?.status === 400) {
        errorMessage = 'Некорректные параметры функции';
      } else if (error.response?.data?.message) {
        errorMessage = error.response.data.message;
      }

      onError(errorMessage);
    } finally {
      setIsGeneratingGraph(false);
    }
  };

  const handleCreate = async () => {
    // Базовая валидация
    if (!functionName.trim()) {
      onError('Введите название функции');
      return;
    }

    if (!selectedFunctionKey) {
      onError('Выберите математическую функцию');
      return;
    }

    if (!leftX || !rightX) {
      onError('Введите интервал');
      return;
    }

    const left = parseFloat(leftX);
    const right = parseFloat(rightX);
    if (isNaN(left) || isNaN(right)) {
      onError('Интервал должен содержать числа');
      return;
    }

    if (left >= right) {
      onError('Левый край должен быть меньше правого');
      return;
    }

    const count = parseInt(pointsCount);
    if (!pointsCount || count < 2) {
      onError('Количество точек должно быть не менее 2');
      return;
    }

    // Проверка параметров
    const func = availableFunctions.find(f => f.key === selectedFunctionKey);
    if ((func?.requiresParams || func?.requiresValue) && !functionParams) {
      onError('Заполните параметры функции');
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

      if (response.status === 201) {
        onSuccess();
        // Сбрасываем форму
        setSelectedFunctionKey('');
        setLeftX('');
        setRightX('');
        setPointsCount('');
        setFunctionParams(null);
      }
    } catch (error) {
      console.error('Error creating function:', error);

      let errorMessage = 'Ошибка создания функции';
      if (error.response?.status === 400) {
        errorMessage = 'Некорректные параметры функции';
      } else if (error.response?.data?.message) {
        errorMessage = error.response.data.message;
      }

      onError(errorMessage);
    } finally {
      setIsCreating(false);
    }
  };

  const handleCancel = () => {
    setSelectedFunctionKey('');
    setLeftX('');
    setRightX('');
    setPointsCount('');
    setFunctionParams(null);
  };

  // Проверка, можно ли построить график
  const canShowGraph = selectedFunctionKey && leftX && rightX && pointsCount;

  return (
    <div className="tab-content">
      <div className="form-section">
        <div className="form-row" style={{ display: 'flex', gap: '15px', marginBottom: '20px', flexWrap: 'wrap' }}>
          <div className="form-group" style={{ flex: '0 0 300px' }}>
            <label className="form-label">
              Математическая функция
              {isLoadingFunctions && <span style={{ color: 'var(--text-secondary)', marginLeft: '10px' }}>
                (загрузка...)
              </span>}
            </label>
            <select
              className="form-input"
              value={selectedFunctionKey}
              onChange={(e) => handleFunctionSelect(e.target.value)}
              disabled={isCreating || isLoadingFunctions}
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
            <small style={{ color: 'var(--text-secondary)', display: 'block', marginTop: '5px' }}>
              ⚙️ - требует параметры, 🔢 - требует значение
            </small>
          </div>
        </div>

        <div className="form-row" style={{ display: 'flex', gap: '15px', marginBottom: '20px' }}>
          <div className="form-group">
            <label className="form-label">Начало интервала</label>
            <input
              type="number"
              className="form-input"
              value={leftX}
              onChange={(e) => setLeftX(e.target.value)}
              placeholder="Например: -10"
              disabled={isCreating}
              step="any"
            />
          </div>

          <div className="form-group">
            <label className="form-label">Конец интервала</label>
            <input
              type="number"
              className="form-input"
              value={rightX}
              onChange={(e) => setRightX(e.target.value)}
              placeholder="Например: 10"
              disabled={isCreating}
              step="any"
            />
          </div>

          <div className="form-group">
            <label className="form-label">Количество точек</label>
            <input
              type="number"
              className="form-input"
              value={pointsCount}
              onChange={(e) => setPointsCount(e.target.value)}
              placeholder="Например: 100"
              disabled={isCreating}
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
              disabled={isGeneratingGraph || isCreating}
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
      </div>

      <div className="action-buttons">
        <button
          className="btn-secondary"
          onClick={handleCancel}
          disabled={isCreating}
        >
          Отмена
        </button>
        <button
          className="btn-primary"
          onClick={handleCreate}
          disabled={isCreating || !selectedFunctionKey}
        >
          {isCreating ? 'Создание...' : 'Создать функцию'}
        </button>
      </div>

      <FunctionParamsModal
        isOpen={isParamsModalOpen}
        functionName={availableFunctions.find(f => f.key === selectedFunctionKey)?.name || ''}
        functionKey={selectedFunctionKey}
        onClose={() => setIsParamsModalOpen(false)}
        onConfirm={handleParamsConfirm}
        isLoading={isCreating}
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
  );
};

export default FromFunctionTab;