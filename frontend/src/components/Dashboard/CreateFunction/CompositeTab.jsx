import { useState, useEffect } from 'react';
import FunctionParamsModal from '../../Common/FunctionParamsModal';
import GraphModal from '../../Common/GraphModal';
import functionService from '../../../services/functionService';
import authService from '../../../services/auth';
import '../../../App.css';

const CompositeTab = ({
  functionName,
  setFunctionName,
  isPublic,
  isCreating,
  setIsCreating,
  onSuccess,
  onError
}) => {
  const [outerFunctionKey, setOuterFunctionKey] = useState('');
  const [innerFunctionKey, setInnerFunctionKey] = useState('');
  const [availableFunctions, setAvailableFunctions] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isParamsModalOpen, setIsParamsModalOpen] = useState(false);
  const [currentFunctionType, setCurrentFunctionType] = useState(null); // 'outer' или 'inner'
  const [outerFunctionParams, setOuterFunctionParams] = useState(null);
  const [innerFunctionParams, setInnerFunctionParams] = useState(null);

  // Состояния для графика
  const [isGraphModalOpen, setIsGraphModalOpen] = useState(false);
  const [graphData, setGraphData] = useState([]);
  const [isGeneratingGraph, setIsGeneratingGraph] = useState(false);
  const [leftX, setLeftX] = useState('-10');
  const [rightX, setRightX] = useState('10');
  const [pointsCount, setPointsCount] = useState('100');

  // Загрузка доступных функций
  useEffect(() => {
    loadAvailableFunctions();
  }, []);

  const loadAvailableFunctions = async () => {
    setIsLoading(true);
    try {
      const allFunctions = await functionService.getAvailableFunctions();
      setAvailableFunctions(allFunctions);
    } catch (error) {
      console.error('Error loading functions:', error);
      onError('Ошибка загрузки функций');
    } finally {
      setIsLoading(false);
    }
  };

  // Проверяем, требуется ли функции параметры
  const needsParams = (functionKey) => {
    const func = availableFunctions.find(f => f.key === functionKey);
    return func?.requiresParams || func?.requiresValue;
  };

  const handleFunctionSelect = (funcType, functionKey) => {
    if (funcType === 'outer') {
      setOuterFunctionKey(functionKey);
      if (needsParams(functionKey)) {
        setCurrentFunctionType('outer');
        setIsParamsModalOpen(true);
      }
    } else {
      setInnerFunctionKey(functionKey);
      if (needsParams(functionKey)) {
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

  // Получаем полный ключ функции с параметрами
  const getFullFunctionKey = (functionKey, params) => {
    if (!functionKey) return '';

    // Для CONSTANT функции
    if (functionKey === 'CONSTANT' && params?.params?.constantValue) {
      return `CONSTANT_${parseFloat(params.params.constantValue)}`;
    }

    // Для BSPLINE - ключ остается 'BSPLINE', но бэкенд должен принимать параметры
    if (functionKey === 'BSPLINE') {
      return 'BSPLINE';
    }

    // Для других функций - просто ключ
    return functionKey;
  };

  // Функция для построения графика composite функции
  const generatePreviewGraph = async () => {
    if (!outerFunctionKey || !innerFunctionKey) {
      onError('Выберите обе функции для построения графика');
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

    // Проверяем параметры
    const outerFunc = availableFunctions.find(f => f.key === outerFunctionKey);
    const innerFunc = availableFunctions.find(f => f.key === innerFunctionKey);

    if (outerFunc?.requiresParams && !outerFunctionParams) {
      onError('Заполните параметры внешней функции');
      return;
    }

    if (innerFunc?.requiresParams && !innerFunctionParams) {
      onError('Заполните параметры внутренней функции');
      return;
    }

    setIsGeneratingGraph(true);

    try {
      // Формируем ключи функций с параметрами
      const outerKeyWithParams = getFullFunctionKey(outerFunctionKey, outerFunctionParams);
      const innerKeyWithParams = getFullFunctionKey(innerFunctionKey, innerFunctionParams);

      console.log('Построение графика composite функции:', {
        outer: outerKeyWithParams,
        inner: innerKeyWithParams,
        leftX: left,
        rightX: right,
        pointsCount: count
      });

      // 1. Сначала создаем временную composite функцию
      const tempName = `Preview Composite: ${outerKeyWithParams}∘${innerKeyWithParams}`;
      const createResponse = await functionService.createComposite(
        tempName,
        outerKeyWithParams,
        innerKeyWithParams,
        false
      );

      if (createResponse.status === 201 && createResponse.data?.functionId) {
        const functionId = createResponse.data.functionId;

        // 2. Получаем данные графика
        const graphResponse = await functionService.getGraphData(
          functionId,
          count,
          left,
          right
        );

        if (graphResponse.data?.points) {
          setGraphData(graphResponse.data.points);
          setIsGraphModalOpen(true);

          // 3. Удаляем временную функцию через 5 секунд
          setTimeout(() => {
            functionService.deleteFunction(functionId).catch(console.error);
          }, 5000);
        } else {
          onError('Не удалось получить данные графика');
          // Удаляем временную функцию
          functionService.deleteFunction(functionId).catch(console.error);
        }
      }
    } catch (error) {
      console.error('Error generating composite graph:', error);

      let errorMessage = 'Ошибка при построении графика';
      if (error.response?.status === 400) {
        errorMessage = 'Некорректные параметры функций';
      } else if (error.response?.status === 404) {
        errorMessage = 'Функции не найдены';
      } else if (error.response?.data?.message) {
        errorMessage = error.response.data.message;
      }

      onError(errorMessage);
    } finally {
      setIsGeneratingGraph(false);
    }
  };

  // Создание composite функции
  const handleCreate = async () => {
    // Валидация
    if (!functionName.trim()) {
      onError('Введите название сложной функции');
      return;
    }

    if (!outerFunctionKey || !innerFunctionKey) {
      onError('Выберите обе функции');
      return;
    }

    // Проверяем параметры
    const outerFunc = availableFunctions.find(f => f.key === outerFunctionKey);
    const innerFunc = availableFunctions.find(f => f.key === innerFunctionKey);

    if (outerFunc?.requiresParams && !outerFunctionParams) {
      onError('Заполните параметры внешней функции');
      return;
    }

    if (innerFunc?.requiresParams && !innerFunctionParams) {
      onError('Заполните параметры внутренней функции');
      return;
    }

    setIsCreating(true);

    try {
      // Формируем ключи функций с параметрами
      const outerKeyWithParams = getFullFunctionKey(outerFunctionKey, outerFunctionParams);
      const innerKeyWithParams = getFullFunctionKey(innerFunctionKey, innerFunctionParams);

      console.log('Создание сложной функции:', {
        name: functionName,
        outer: outerKeyWithParams,
        inner: innerKeyWithParams,
        isPublic
      });

      // Создаем сложную функцию
      const response = await functionService.createComposite(
        functionName + ' (composite)',
        outerKeyWithParams,
        innerKeyWithParams,
        isPublic
      );

      if (response.status === 201) {
        // Обновляем список доступных функций
        await loadAvailableFunctions();

        onSuccess();

        // Сбрасываем форму
        setOuterFunctionKey('');
        setInnerFunctionKey('');
        setOuterFunctionParams(null);
        setInnerFunctionParams(null);
        setFunctionName('');
      }
    } catch (error) {
      console.error('Error creating composite function:', error);

      let message = 'Ошибка создания сложной функции';
      if (error.response?.status === 400) {
        message = 'Некорректные параметры функций. Проверьте: ';

        if (error.response.data?.message) {
          message += error.response.data.message;
        } else if (outerFunctionKey === 'BSPLINE' || innerFunctionKey === 'BSPLINE') {
          message += 'Для B-сплайна нужно передать: nodePoints, splineOrder, weights';
        }
      } else if (error.response?.status === 404) {
        message = 'Одна из функций не найдена';
      } else if (error.response?.data?.message) {
        message = error.response.data.message;
      }

      onError(message);
    } finally {
      setIsCreating(false);
    }
  };

  const handleCancel = () => {
    setOuterFunctionKey('');
    setInnerFunctionKey('');
    setOuterFunctionParams(null);
    setInnerFunctionParams(null);
  };

  // Получаем отображаемое имя функции
  const getFunctionDisplayName = (functionKey) => {
    const func = availableFunctions.find(f => f.key === functionKey);
    if (!func) return functionKey;

    return func.name;
  };

  // Проверка, можно ли построить график
  const canShowGraph = outerFunctionKey && innerFunctionKey && leftX && rightX && pointsCount;

  // Получаем текущую выбранную функцию для модалки параметров
  const getCurrentFunctionForParams = () => {
    const functionKey = currentFunctionType === 'outer' ? outerFunctionKey : innerFunctionKey;
    const func = availableFunctions.find(f => f.key === functionKey);
    return func || null;
  };

  return (
    <div className="tab-content">
      <div className="form-section">
        <div className="form-row" style={{ display: 'flex', gap: '15px', marginBottom: '20px' }}>
          <div className="form-group" style={{ flex: 1 }}>
            <label className="form-label">
              Внешняя функция (f)
              {outerFunctionParams && (
                <span style={{ marginLeft: '10px', color: 'var(--accent-color)' }}>
                  ⚙️
                </span>
              )}
            </label>
            <select
              className="form-input"
              value={outerFunctionKey}
              onChange={(e) => handleFunctionSelect('outer', e.target.value)}
              disabled={isCreating || isLoading}
            >
              <option value="">Выберите внешнюю функцию...</option>

              <optgroup label="Базовые функции">
                {availableFunctions
                  .filter(f => f.type === 'BASIC')
                  .map(func => (
                    <option key={func.key} value={func.key}>
                      {func.name} {func.requiresParams && '⚙️'} {func.requiresValue && '🔢'}
                    </option>
                  ))}
              </optgroup>

              <optgroup label="Мои функции">
                {availableFunctions
                  .filter(f => f.type === 'USER' && !f.isComposite)
                  .map(func => (
                    <option key={func.key} value={func.key}>
                      {func.name} (ID: {func.functionId})
                    </option>
                  ))}
              </optgroup>

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
          </div>

          <div className="form-group" style={{ flex: 1 }}>
            <label className="form-label">
              Внутренняя функция (g)
              {innerFunctionParams && (
                <span style={{ marginLeft: '10px', color: 'var(--accent-color)' }}>
                  ⚙️
                </span>
              )}
            </label>
            <select
              className="form-input"
              value={innerFunctionKey}
              onChange={(e) => handleFunctionSelect('inner', e.target.value)}
              disabled={isCreating || isLoading}
            >
              <option value="">Выберите внутреннюю функцию...</option>

              <optgroup label="Базовые функции">
                {availableFunctions
                  .filter(f => f.type === 'BASIC')
                  .map(func => (
                    <option key={func.key} value={func.key}>
                      {func.name} {func.requiresParams && '⚙️'} {func.requiresValue && '🔢'}
                    </option>
                  ))}
              </optgroup>

              <optgroup label="Мои функции">
                {availableFunctions
                  .filter(f => f.type === 'USER' && !f.isComposite)
                  .map(func => (
                    <option key={func.key} value={func.key}>
                      {func.name} (ID: {func.functionId})
                    </option>
                  ))}
              </optgroup>

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
          </div>
        </div>

        {/* Поля для графика */}
        <div className="form-row" style={{ display: 'flex', gap: '15px', marginBottom: '20px', alignItems: 'center' }}>
          <div className="form-group" style={{ flex: 1 }}>
            <label className="form-label">Интервал для графика</label>
            <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
              <input
                type="number"
                className="form-input"
                value={leftX}
                onChange={(e) => setLeftX(e.target.value)}
                placeholder="От"
                disabled={isCreating || isLoading}
                step="any"
                style={{ width: '100px' }}
              />
              <span>до</span>
              <input
                type="number"
                className="form-input"
                value={rightX}
                onChange={(e) => setRightX(e.target.value)}
                placeholder="До"
                disabled={isCreating || isLoading}
                step="any"
                style={{ width: '100px' }}
              />
            </div>
          </div>

          <div className="form-group" style={{ flex: 1 }}>
            <label className="form-label">Точек на графике</label>
            <input
              type="number"
              className="form-input"
              value={pointsCount}
              onChange={(e) => setPointsCount(e.target.value)}
              placeholder="100"
              disabled={isCreating || isLoading}
              min="2"
              max="1000"
              style={{ width: '120px' }}
            />
          </div>

          <div className="form-group" style={{ marginTop: '24px' }}>
            <button
              className="btn-secondary"
              onClick={generatePreviewGraph}
              disabled={isGeneratingGraph || !canShowGraph || isCreating || isLoading}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
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
          </div>
        </div>

        {/* Показываем выбранную композицию */}
        {outerFunctionKey && innerFunctionKey && (
          <div className="form-group">
            <div className="server-message" style={{
              background: 'rgba(128, 0, 0, 0.1)',
              border: '1px solid rgba(128, 0, 0, 0.3)',
              color: 'var(--text-primary)',
              padding: '15px',
              borderRadius: '8px',
              marginTop: '10px'
            }}>
              <strong>Создается сложная функция:</strong><br />
              <code style={{ fontSize: '16px', display: 'block', marginTop: '10px' }}>
                h(x) = f(g(x))<br />
                где f(x) = {getFunctionDisplayName(outerFunctionKey)}<br />
                где g(x) = {getFunctionDisplayName(innerFunctionKey)}
              </code>
              <small style={{ display: 'block', marginTop: '10px', color: 'var(--text-secondary)' }}>
                После создания эта функция будет доступна для создания табулированных функций
              </small>
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
          disabled={isCreating || !outerFunctionKey || !innerFunctionKey}
        >
          {isCreating ? 'Создание...' : 'Создать сложную функцию'}
        </button>
      </div>

      {/* Модальное окно для параметров функций */}
      <FunctionParamsModal
        isOpen={isParamsModalOpen}
        functionName={getCurrentFunctionForParams()?.name || ''}
        functionKey={currentFunctionType === 'outer' ? outerFunctionKey : innerFunctionKey}
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
        title={`Composite: ${getFunctionDisplayName(outerFunctionKey)}∘${getFunctionDisplayName(innerFunctionKey)} в [${leftX}, ${rightX}]`}
      />
    </div>
  );
};

export default CompositeTab;