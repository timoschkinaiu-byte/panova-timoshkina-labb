import { useState, useEffect } from 'react';
import FunctionParamsModal from '../../Common/FunctionParamsModal';
import GraphModal from '../../Common/GraphModal';
import functionService from '../../../services/functionService';
import "../../../App.css";

const CompositeTab = ({
  functionName,
  setFunctionName,
  isPublic,
  isCreating,
  setIsCreating,
  onSuccess,
  onError
}) => {
  const [outerFunction, setOuterFunction] = useState('');
  const [innerFunction, setInnerFunction] = useState('');
  const [availableFunctions, setAvailableFunctions] = useState([]);
  const [isParamsModalOpen, setIsParamsModalOpen] = useState(false);
  const [currentFunctionType, setCurrentFunctionType] = useState(null); // 'outer' или 'inner'
  const [outerFunctionParams, setOuterFunctionParams] = useState(null);
  const [innerFunctionParams, setInnerFunctionParams] = useState(null);

  // Состояния для окна графика
  const [isGraphModalOpen, setIsGraphModalOpen] = useState(false);
  const [graphData, setGraphData] = useState([]);
  const [graphTitle, setGraphTitle] = useState('');
  const [isGeneratingGraph, setIsGeneratingGraph] = useState(false);

  useEffect(() => {
    loadAvailableFunctions();
  }, []);

  const loadAvailableFunctions = async () => {
    try {
      const response = await functionService.getAvailableMathFunctions();
      setAvailableFunctions(response);
    } catch (error) {
      console.error('Error loading functions:', error);
    }
  };

  // Проверяем, требуется ли функции параметры
  const needsParams = (functionName) => {
    const func = availableFunctions.find(f => f.name === functionName);
    return func?.requiresParams || func?.requiresValue;
  };

  const handleFunctionSelect = (funcType, funcName) => {
    if (funcType === 'outer') {
      setOuterFunction(funcName);
      if (needsParams(funcName)) {
        setCurrentFunctionType('outer');
        setIsParamsModalOpen(true);
      }
    } else {
      setInnerFunction(funcName);
      if (needsParams(funcName)) {
        setCurrentFunctionType('inner');
        setIsParamsModalOpen(true);
      }
    }
  };

  const handleParamsConfirm = (params) => {
    if (currentFunctionType === 'outer') {
      setOuterFunctionParams(params);
    } else {
      setInnerFunctionParams(params);
    }
    setIsParamsModalOpen(false);
  };

  const getFunctionWithParams = (funcName) => {
    // Здесь должна быть логика формирования имени функции с параметрами
    // Пока возвращаем просто имя
    return funcName;
  };

  // Функция для предварительного просмотра композиции
  const generatePreviewGraph = async () => {
    if (!outerFunction || !innerFunction) {
      onError('Выберите обе функции для предварительного просмотра');
      return;
    }

    setIsGeneratingGraph(true);

    try {
      // Сначала создаем временную сложную функцию для просмотра
      const tempResponse = await functionService.createComposite(
        `Предварительный просмотр: ${outerFunction}∘${innerFunction}`,
        outerFunction,
        innerFunction,
        false // Не публичная, временная
      );

      if (tempResponse.status === 201 && tempResponse.data?.functionId) {
        const functionId = tempResponse.data.functionId;

        // Получаем данные графика для этой функции
        const graphResponse = await functionService.getGraphData(
          functionId,
          200,
          -10,
          10
        );

        if (graphResponse.data && graphResponse.data.points) {
          setGraphData(graphResponse.data.points);
          setGraphTitle(`${outerFunction}(${innerFunction}(x))`);
          setIsGraphModalOpen(true);

          // Удаляем временную функцию после успешного получения данных
          setTimeout(() => {
            functionService.deleteFunction(functionId).catch(console.error);
          }, 1000);
        } else {
          onError('Не удалось получить данные графика от сервера');
        }
      } else {
        onError('Не удалось создать временную функцию для просмотра');
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
    if (!functionName.trim()) {
      onError('Введите название функции');
      return;
    }

    if (!outerFunction || !innerFunction) {
      onError('Выберите обе функции');
      return;
    }

    // Проверяем, что для функций с параметрами параметры введены
    if (needsParams(outerFunction) && !outerFunctionParams) {
      onError('Заполните параметры внешней функции');
      return;
    }

    if (needsParams(innerFunction) && !innerFunctionParams) {
      onError('Заполните параметры внутренней функции');
      return;
    }

    setIsCreating(true);

    try {
      // Формируем названия функций с параметрами
      const outerFuncWithParams = getFunctionWithParams(outerFunction, outerFunctionParams);
      const innerFuncWithParams = getFunctionWithParams(innerFunction, innerFunctionParams);

      const response = await functionService.createComposite(
        functionName,
        outerFuncWithParams,
        innerFuncWithParams,
        isPublic
      );

      if (response.status === 201 && response.data?.functionId) {
        onSuccess();
        // Сбрасываем форму
        setOuterFunction('');
        setInnerFunction('');
        setOuterFunctionParams(null);
        setInnerFunctionParams(null);
      } else {
        onError('Неизвестная ошибка при создании функции');
      }
    } catch (error) {
      console.error('Error creating composite function:', error);

      let message = 'Ошибка создания сложной функции';
      if (error.response?.status === 401) {
        message = 'Недостаточно прав для создания функции';
      } else if (error.response?.status === 400) {
        message = error.response.data?.message || 'Некорректные данные для создания функции';
      } else if (error.response?.data?.message) {
        message = error.response.data.message;
      } else if (!error.response) {
        message = 'Сервер не отвечает. Проверьте подключение к API';
      }

      onError(message);
    } finally {
      setIsCreating(false);
    }
  };

  const handleCancel = () => {
    setOuterFunction('');
    setInnerFunction('');
    setOuterFunctionParams(null);
    setInnerFunctionParams(null);
  };

  // Получаем текущее название функции для модального окна
  const getCurrentFunctionName = () => {
    return currentFunctionType === 'outer' ? outerFunction : innerFunction;
  };

  return (
    <div className="tab-content">
      <div className="form-section">
        <div className="form-row" style={{ display: 'flex', gap: '15px', marginBottom: '20px' }}>
          <div className="form-group" style={{ flex: 1 }}>
            <label className="form-label">Внешняя функция (f)</label>
            <select
              className="form-input"
              value={outerFunction}
              onChange={(e) => handleFunctionSelect('outer', e.target.value)}
              disabled={isCreating}
            >
              <option value="">Выберите внешнюю функцию...</option>
              {availableFunctions.map(func => (
                <option key={func.name} value={func.name}>
                  {func.name}
                </option>
              ))}
            </select>
            {outerFunctionParams && (
              <div style={{ marginTop: '5px', fontSize: '12px', color: 'var(--text-secondary)' }}>
                Параметры: {JSON.stringify(outerFunctionParams)}
              </div>
            )}
          </div>

          <div className="form-group" style={{ flex: 1 }}>
            <label className="form-label">Внутренняя функция (g)</label>
            <select
              className="form-input"
              value={innerFunction}
              onChange={(e) => handleFunctionSelect('inner', e.target.value)}
              disabled={isCreating}
            >
              <option value="">Выберите внутреннюю функцию...</option>
              {availableFunctions.map(func => (
                <option key={func.name} value={func.name}>
                  {func.name}
                </option>
              ))}
            </select>
            {innerFunctionParams && (
              <div style={{ marginTop: '5px', fontSize: '12px', color: 'var(--text-secondary)' }}>
                Параметры: {JSON.stringify(innerFunctionParams)}
              </div>
            )}
          </div>
        </div>

        {/* Показываем выбранную композицию */}
        {outerFunction && innerFunction && (
          <div className="form-group">
            <div className="server-message" style={{
              background: 'rgba(128, 0, 0, 0.1)',
              border: '1px solid rgba(128, 0, 0, 0.3)',
              color: 'var(--text-primary)'
            }}>
              <strong>Будет создана сложная функция:</strong><br />
              f(g(x)) = {outerFunction}({innerFunction}(x))
            </div>

            {/* Кнопка построения графика */}
            <div style={{ marginTop: '15px', textAlign: 'center' }}>
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
                Создаст временную функцию и покажет её график
              </p>
            </div>
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
          disabled={isCreating || !outerFunction || !innerFunction}
        >
          {isCreating ? 'Создание...' : 'Создать сложную функцию'}
        </button>
      </div>

      {/* Модальное окно для параметров функций */}
      <FunctionParamsModal
        isOpen={isParamsModalOpen}
        functionName={getCurrentFunctionName()}
        onClose={() => setIsParamsModalOpen(false)}
        onConfirm={handleParamsConfirm}
        isLoading={isCreating}
      />

      {/* Модальное окно для графика */}
      <GraphModal
        isOpen={isGraphModalOpen}
        onClose={() => {
          setIsGraphModalOpen(false);
          setGraphData([]);
          setGraphTitle('');
        }}
        points={graphData}
        title={graphTitle}
      />
    </div>
  );
};

export default CompositeTab;