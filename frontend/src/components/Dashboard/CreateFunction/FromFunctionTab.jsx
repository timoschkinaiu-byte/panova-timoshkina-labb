import { useState } from 'react';
import FunctionParamsModal from '../../Common/FunctionParamsModal';
import GraphModal from '../../Common/GraphModal';
import functionService from '../../../services/functionService';
import "../../../App.css";

const FromFunctionTab = ({
  functionName,
  setFunctionName,
  isPublic,
  isCreating,
  setIsCreating,
  onSuccess,
  onError
}) => {
  const [selectedFunction, setSelectedFunction] = useState('');
  const [leftX, setLeftX] = useState('');
  const [rightX, setRightX] = useState('');
  const [pointsCount, setPointsCount] = useState('');
  const [isParamsModalOpen, setIsParamsModalOpen] = useState(false);
  const [functionParams, setFunctionParams] = useState(null);

  // Состояния для графика
  const [isGraphModalOpen, setIsGraphModalOpen] = useState(false);
  const [graphData, setGraphData] = useState([]);
  const [isGeneratingGraph, setIsGeneratingGraph] = useState(false);

  const availableFunctions = functionService.getAvailableMathFunctions();

  const handleFunctionSelect = (funcName) => {
    setSelectedFunction(funcName);

    // Проверяем, нужны ли параметры
    const func = availableFunctions.find(f => f.name === funcName);
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
    // Валидация
    if (!selectedFunction) {
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

    // Для функций с параметрами проверяем, что параметры введены
    const func = availableFunctions.find(f => f.name === selectedFunction);
    if ((func?.requiresParams || func?.requiresValue) && !functionParams) {
      onError('Заполните параметры функции');
      return;
    }

    setIsGeneratingGraph(true);

    try {
      // Создаем временную функцию для просмотра
      const tempName = `Предварительный просмотр: ${selectedFunction}`;
      let sourceFunctionName = selectedFunction;

      const response = await functionService.createFromMathFunction(
        tempName,
        sourceFunctionName,
        left,
        right,
        count,
        false // Не публичная, временная
      );

      if (response.status === 201 && response.data?.functionId) {
        const functionId = response.data.functionId;

        // Получаем данные графика
        const graphResponse = await functionService.getGraphData(
          functionId,
          count,
          left,
          right
        );

        if (graphResponse.data && graphResponse.data.points) {
          setGraphData(graphResponse.data.points);
          setIsGraphModalOpen(true);

          // Удаляем временную функцию
          setTimeout(() => {
            functionService.deleteFunction(functionId).catch(console.error);
          }, 1000);
        } else {
          onError('Не удалось получить данные графика');
        }
      }
    } catch (error) {
      console.error('Error generating preview graph:', error);

      let errorMessage = 'Ошибка при построении графика';
      if (error.response?.status === 401) {
        errorMessage = 'Недостаточно прав для создания функции';
      } else if (error.response?.status === 400) {
        errorMessage = 'Некорректные данные для создания функции';
      } else if (error.response?.data?.message) {
        errorMessage = error.response.data.message;
      } else if (!error.response) {
        errorMessage = 'Сервер не отвечает. Проверьте подключение к API';
      }

      onError(errorMessage);
    } finally {
      setIsGeneratingGraph(false);
    }
  };

  const handleCreate = async () => {
    // Валидация
    if (!functionName.trim()) {
      onError('Введите название функции');
      return;
    }

    if (!selectedFunction) {
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

    if (!pointsCount || parseInt(pointsCount) < 2) {
      onError('Количество точек должно быть не менее 2');
      return;
    }

    // Для функций с параметрами проверяем, что параметры введены
    const func = availableFunctions.find(f => f.name === selectedFunction);
    if ((func?.requiresParams || func?.requiresValue) && !functionParams) {
      onError('Заполните параметры функции');
      return;
    }

    let validatedParams = null;

    if (selectedFunction === 'Постоянная функция') {
      if (!functionParams?.constantValue) {
        onError('Введите значение константы');
        return;
      }

      const constant = parseFloat(functionParams.constantValue);
      if (isNaN(constant)) {
        onError('Значение константы должно быть числом');
        return;
      }

      validatedParams = { constantValue: constant };
    }

    if (selectedFunction === 'B-сплайн функция') {
      if (!functionParams?.nodePoints || !functionParams?.splineOrder || !functionParams?.weights) {
        onError('Заполните все параметры B-сплайна');
        return;
      }

      // Преобразуем и валидируем узловые точки
      const nodePointsArray = functionParams.nodePoints
        .split(',')
        .map(p => p.trim())
        .filter(p => p !== '')
        .map(p => parseFloat(p));

      if (nodePointsArray.some(p => isNaN(p))) {
        onError('Узловые точки должны быть числами');
        return;
      }

      if (nodePointsArray.length < 2) {
        onError('Нужно минимум 2 узловые точки');
        return;
      }

      // Проверяем порядок сплайна
      const splineOrder = parseInt(functionParams.splineOrder);
      if (isNaN(splineOrder) || splineOrder < 1) {
        onError('Порядок сплайна должен быть целым числом > 0');
        return;
      }

      if (splineOrder >= nodePointsArray.length) {
        onError('Порядок сплайна должен быть меньше количества узловых точек');
        return;
      }

      // Преобразуем и валидируем веса
      const weightsArray = functionParams.weights
        .split(',')
        .map(w => w.trim())
        .filter(w => w !== '')
        .map(w => parseFloat(w));

      if (weightsArray.some(w => isNaN(w))) {
        onError('Весовые коэффициенты должны быть числами');
        return;
      }

      if (weightsArray.length !== nodePointsArray.length) {
        onError('Количество весов должно совпадать с количеством узловых точек');
        return;
      }

      validatedParams = {
        nodePoints: nodePointsArray,
        splineOrder: splineOrder,
        weights: weightsArray
      };
    }

    setIsCreating(true);

    try {
      // Формируем название функции с параметрами если нужно

      const requestData = {
        name: functionName,
        sourceFunctionName: selectedFunction,
        leftX: left,
        rightX: right,
        pointsCount: count,
        isPublic: isPublic,
        ...validatedParams // Добавляем специфичные параметры
      };


      if (response.status === 201) {
        onSuccess();
        // Сбрасываем форму
        setSelectedFunction('');
        setLeftX('');
        setRightX('');
        setPointsCount('');
        setFunctionParams(null);
      }
    } catch (error) {
      console.error('Error creating function:', error);
      const message = error.response?.data?.message || 'Ошибка создания функции';
      onError(message);
    } finally {
      setIsCreating(false);
    }
  };

  const handleCancel = () => {
    setSelectedFunction('');
    setLeftX('');
    setRightX('');
    setPointsCount('');
    setFunctionParams(null);

  };

  // Проверка, можно ли построить график
  const canShowGraph = selectedFunction && leftX && rightX && pointsCount;

  return (
    <div className="tab-content">
      <div className="form-section">
        <div className="form-row" style={{ display: 'flex', gap: '15px', marginBottom: '20px', flexWrap: 'wrap' }}>
          <div className="form-group" style={{ flex: '0 0 300px' }}>
            <label className="form-label">Математическая функция</label>
            <select
              className="form-input"
              value={selectedFunction}
              onChange={(e) => handleFunctionSelect(e.target.value)}
              disabled={isCreating}
            >
              <option value="">Выберите функцию...</option>
              {availableFunctions.map(func => (
                <option key={func.name} value={func.name}>
                  {func.name}
                </option>
              ))}
            </select>
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

          <div className="form-group" >
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
          disabled={isCreating || !selectedFunction}
        >
          {isCreating ? 'Создание...' : 'Создать функцию'}
        </button>
      </div>

      <FunctionParamsModal
        isOpen={isParamsModalOpen}
        functionName={selectedFunction}
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
        title={`${selectedFunction} в интервале [${leftX}, ${rightX}]`}
      />
    </div>
  );
};

export default FromFunctionTab;