import React, { useState, useEffect } from 'react';
import GraphPreview from '../Common/GraphPreview';
import CreateFromArrayModal from '../Common/CreateFromArrayModal';
import CreateFromFunctionModal from '../Common/CreateFromFunctionModal';
import functionService from '../../services/functionService';
import notificationService from '../../services/notificationService';
import "../../App.css";

const Operations = () => {
  // Основные состояния
  const [functions, setFunctions] = useState([]);
  const [loading, setLoading] = useState(false);

  // Выбранные функции
  const [function1, setFunction1] = useState(null);
  const [function2, setFunction2] = useState(null);
  const [resultFunction, setResultFunction] = useState(null);

  // Точки функций в формате для таблицы
  const [func1Points, setFunc1Points] = useState([]);
  const [func2Points, setFunc2Points] = useState([]);
  const [resultPoints, setResultPoints] = useState([]);

  // Настройки операций
  const [operation, setOperation] = useState('add');
  const [loadingResult, setLoadingResult] = useState(false);

  // Модальные окна
  const [showCreate1ArrayModal, setShowCreate1ArrayModal] = useState(false);
  const [showCreate1FunctionModal, setShowCreate1FunctionModal] = useState(false);
  const [showCreate2ArrayModal, setShowCreate2ArrayModal] = useState(false);
  const [showCreate2FunctionModal, setShowCreate2FunctionModal] = useState(false);

  // Операции
  const operations = [
    { id: 'add', name: 'Сложение', symbol: '+' },
    { id: 'subtract', name: 'Вычитание', symbol: '−' },
    { id: 'multiply', name: 'Умножение', symbol: '×' },
    { id: 'divide', name: 'Деление', symbol: '÷' },
  ];

  // ============ ЭФФЕКТЫ ============
  useEffect(() => {
    loadFunctions();
  }, []);

  useEffect(() => {
    if (function1) loadFunctionPoints(function1.id, setFunc1Points, true);
    else setFunc1Points([]);
  }, [function1]);

  useEffect(() => {
    if (function2) loadFunctionPoints(function2.id, setFunc2Points, true);
    else setFunc2Points([]);
  }, [function2]);

  useEffect(() => {
    if (resultFunction) loadFunctionPoints(resultFunction.functionId, setResultPoints, false);
    else setResultPoints([]);
  }, [resultFunction]);

  // ============ ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ============
  const loadFunctions = async () => {
    try {
      setLoading(true);
      const response = await functionService.getMyFunctions();
      setFunctions(response.data || []);
    } catch (error) {
      console.error('Ошибка загрузки функций:', error);
      notificationService.error('Ошибка загрузки функций');
    } finally {
      setLoading(false);
    }
  };

  const loadFunctionPoints = async (functionId, setter, editable = true) => {
    try {
      const response = await functionService.getFunctionPoints(functionId);
      const points = response.data || [];

      // Форматируем точки для таблицы
      const formattedPoints = points.map((point, index) => ({
        id: index,
        x: point.x,
        y: point.y,
        editable: editable
      }));

      setter(formattedPoints);
      return formattedPoints;
    } catch (error) {
      console.error('Ошибка загрузки точек:', error);
      notificationService.error('Ошибка загрузки точек');
      setter([]);
      return [];
    }
  };

  const getGraphRange = (points) => {
    if (!points || points.length === 0) {
      return { x: { min: -10, max: 10 }, y: { min: -10, max: 10 } };
    }

    const xValues = points.map(p => p.x);
    const yValues = points.map(p => p.y);

    const xMin = Math.min(...xValues);
    const xMax = Math.max(...xValues);
    const yMin = Math.min(...yValues);
    const yMax = Math.max(...yValues);

    const xPadding = (xMax - xMin) * 0.1 || 1;
    const yPadding = (yMax - yMin) * 0.1 || 1;

    return {
      x: { min: xMin - xPadding, max: xMax + xPadding },
      y: { min: yMin - yPadding, max: yMax + yPadding }
    };
  };

  // ============ СОХРАНЕНИЕ И ЗАГРУЗКА ============
  const handleSaveFunction = async (func, points, position) => {
    if (!func || !points || points.length === 0) {
      notificationService.warning('Нет функции для сохранения');
      return;
    }

    try {
      // Используем метод экспорта из functionService
      const response = await functionService.exportFunction(func.id || func.functionId, 'binary');

      // Создаем blob для скачивания
      const blob = new Blob([response.data], { type: 'application/octet-stream' });
      const url = URL.createObjectURL(blob);

      const a = document.createElement('a');
      a.href = url;
      a.download = `${func.name.replace(/[^a-z0-9]/gi, '_') || 'function'}.bin`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);

      notificationService.success(`Функция "${func.name}" экспортирована`);
    } catch (error) {
      console.error('Ошибка экспорта:', error);
      notificationService.error('Ошибка экспорта функции');
    }
  };

  const handleLoadFunction = async (position) => {
    try {
      // Создаем input для выбора файла
      const input = document.createElement('input');
      input.type = 'file';
      input.accept = '.bin';

      input.onchange = async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        try {
          // Используем метод импорта из functionService
          const response = await functionService.importFunction(file, 'binary');

          if (response.data) {
            const loadedFunc = {
              id: response.data.id,
              functionId: response.data.functionId || response.data.id,
              name: response.data.name,
              type: response.data.type || response.data.functionType || 'TABULATED',
              isPublic: response.data.isPublic || false,
              pointsCount: response.data.pointsCount || 0
            };

            if (position === 1) {
              setFunction1(loadedFunc);
            } else {
              setFunction2(loadedFunc);
            }

            setFunctions(prev => [...prev, response.data]);
            notificationService.success(`Функция "${loadedFunc.name}" загружена`);
          }
        } catch (error) {
          console.error('Ошибка загрузки файла:', error);
          notificationService.error('Ошибка загрузки файла функции');
        }
      };

      input.click();
    } catch (error) {
      console.error('Ошибка загрузки:', error);
      notificationService.error('Ошибка при выборе файла');
    }
  };

  // ============ ОБРАБОТЧИКИ ДЕЙСТВИЙ ============
  const handleSelectFunction = (position) => {
    if (functions.length === 0) {
      notificationService.error('Нет доступных функций');
      return;
    }

    const functionList = functions.map(f => `ID: ${f.id} - "${f.name}"`).join('\n');
    const selectedId = parseInt(prompt(`Выберите ID функции:\n${functionList}`, ''));

    if (!selectedId) return;

    const selectedFunc = functions.find(f => f.id === selectedId || f.functionId === selectedId);

    if (selectedFunc) {
      const funcInfo = {
        id: selectedFunc.id,
        functionId: selectedFunc.functionId || selectedFunc.id,
        name: selectedFunc.name,
        type: selectedFunc.type || selectedFunc.functionType || 'TABULATED',
        isPublic: selectedFunc.isPublic || false,
        pointsCount: selectedFunc.pointsCount || 0
      };

      if (position === 1) setFunction1(funcInfo);
      else setFunction2(funcInfo);
    } else {
      notificationService.error('Функция не найдена');
    }
  };

  const handleSaveResult = async () => {
    if (!resultFunction || resultPoints.length === 0) {
      notificationService.error('Нет результата для сохранения');
      return;
    }

    const functionName = prompt('Введите имя для функции:', `${resultFunction.name}_saved`);
    if (!functionName?.trim()) return;

    try {
      const xValues = resultPoints.map(p => p.x);
      const yValues = resultPoints.map(p => p.y);

      const response = await functionService.createFromArrays(
        functionName.trim(),
        xValues,
        yValues,
        false
      );

      if (response.data) {
        setFunctions(prev => [...prev, response.data]);
        notificationService.success(`Функция "${functionName}" сохранена!`);
      }
    } catch (error) {
      console.error('Ошибка сохранения:', error);
      notificationService.error('Ошибка сохранения функции');
    }
  };

  const handleOperation = async () => {
    if (!function1 || !function2) {
      notificationService.warning('Выберите обе функции');
      return;
    }

    if (function1.id === function2.id) {
      notificationService.warning('Выберите разные функции');
      return;
    }

    setLoadingResult(true);
    try {
      const resultName = `${function1.name} ${operations.find(op => op.id === operation)?.symbol || '?'} ${function2.name}`;

      let result;
      switch (operation) {
        case 'add':
          result = await functionService.addFunctions(function1.id, function2.id, resultName, false);
          break;
        case 'subtract':
          result = await functionService.subtractFunctions(function1.id, function2.id, resultName, false);
          break;
        case 'multiply':
          result = await functionService.multiplyFunctions(function1.id, function2.id, resultName, false);
          break;
        case 'divide':
          result = await functionService.divideFunctions(function1.id, function2.id, resultName, false);
          break;
        default:
          throw new Error('Неизвестная операция');
      }

      if (result.data) {
        const resultData = result.data;
        const resultFuncData = {
          id: resultData.functionId || resultData.id,
          functionId: resultData.functionId || resultData.id,
          name: resultData.functionName || resultData.name || resultName,
          type: resultData.functionType || resultData.type || 'TABULATED',
          ownerId: resultData.ownerId,
          ownerName: resultData.ownerName,
          isPublic: resultData.isPublic || false,
          pointsCount: resultData.pointsCount || 0
        };

        setResultFunction(resultFuncData);
        setFunctions(prev => [...prev, resultFuncData]);
        notificationService.success('Операция выполнена!');
      }
    } catch (error) {
      console.error('Ошибка операции:', error);
      notificationService.error('Ошибка выполнения операции: ' + (error.response?.data?.message || error.message));
    } finally {
      setLoadingResult(false);
    }
  };

  const handleClear = (position) => {
    if (position === 1) {
      setFunction1(null);
      setFunc1Points([]);
    } else if (position === 2) {
      setFunction2(null);
      setFunc2Points([]);
    } else {
      setResultFunction(null);
      setResultPoints([]);
    }
  };

  const handleExportResult = async (format = 'json') => {
    if (!resultFunction) {
      notificationService.warning('Нет результата для экспорта');
      return;
    }

    try {
      await functionService.exportFunction(resultFunction.functionId, format);
      notificationService.success(`Экспортировано в ${format.toUpperCase()}`);
    } catch (error) {
      notificationService.error('Ошибка экспорта');
    }
  };

  const operationSymbol = (opId) => {
    return operations.find(o => o.id === opId)?.symbol || '+';
  };

  const handleCreateArrayFunction = (position, response) => {
    if (!response?.data) {
      notificationService.error('Ошибка: нет данных в ответе');
      return;
    }

    const funcInfo = {
      id: response.data.id,
      functionId: response.data.functionId || response.data.id,
      name: response.data.name,
      type: response.data.type || response.data.functionType || 'TABULATED',
      isPublic: response.data.isPublic || false,
      pointsCount: response.data.pointsCount || 0
    };

    if (position === 1) {
      setFunction1(funcInfo);
      setShowCreate1ArrayModal(false);
    } else {
      setFunction2(funcInfo);
      setShowCreate2ArrayModal(false);
    }

    setFunctions(prev => [...prev, response.data]);
    notificationService.success(`Функция "${response.data.name}" создана`);
  };

  const handleCreateMathFunction = (position, response) => {
    if (!response?.data) {
      notificationService.error('Ошибка: нет данных в ответе');
      return;
    }

    const funcInfo = {
      id: response.data.id,
      functionId: response.data.functionId || response.data.id,
      name: response.data.name,
      type: response.data.type || response.data.functionType || 'TABULATED',
      isPublic: response.data.isPublic || false,
      pointsCount: response.data.pointsCount || 0
    };

    if (position === 1) {
      setFunction1(funcInfo);
      setShowCreate1FunctionModal(false);
    } else {
      setFunction2(funcInfo);
      setShowCreate2FunctionModal(false);
    }

    setFunctions(prev => [...prev, response.data]);
    notificationService.success(`Функция "${response.data.name}" создана`);
  };

  // ============ РЕДАКТИРОВАНИЕ ТОЧЕК ============
  const handlePointChange = async (position, pointId, newY) => {
    const newYNum = parseFloat(newY);
    if (isNaN(newYNum)) {
      notificationService.error('Введите корректное число');
      return;
    }

    if (position === 1 && function1) {
      // Обновляем локально
      setFunc1Points(prev => prev.map(p =>
        p.id === pointId ? { ...p, y: newYNum } : p
      ));

      // Можно добавить обновление на сервере позже
      notificationService.info('Значение Y обновлено локально');
    } else if (position === 2 && function2) {
      // Обновляем локально
      setFunc2Points(prev => prev.map(p =>
        p.id === pointId ? { ...p, y: newYNum } : p
      ));

      notificationService.info('Значение Y обновлено локально');
    }
  };

  // ============ КОМПОНЕНТ ТАБЛИЦЫ ============
  const FunctionTable = ({
    title,
    func,
    points,
    position,
    isResult = false
  }) => {
    const graphRange = getGraphRange(points);

    return (
      <div className={`function-section ${isResult ? 'result-section' : ''}`}>
        <div className="section-header">
          <h3 className="section-title">{title}</h3>
          <div className="section-actions">
            {!isResult ? (
              <>
                <button
                  className="btn-action create"
                  onClick={() => position === 1
                    ? setShowCreate1ArrayModal(true)
                    : setShowCreate2ArrayModal(true)}
                  title="Создать из массивов"
                >
                  📊 Создать (массивы)
                </button>

                <button
                  className="btn-action create"
                  onClick={() => position === 1
                    ? setShowCreate1FunctionModal(true)
                    : setShowCreate2FunctionModal(true)}
                  title="Создать из Math функции"
                  style={{ backgroundColor: '#9c27b0' }}
                >
                  🧮 Создать (Math)
                </button>

                <button
                  className="btn-action open"
                  onClick={() => handleLoadFunction(position)}
                  title="Загрузить из файла"
                >
                  📂 Загрузить
                </button>

                <button
                  className="btn-action open"
                  onClick={() => handleSelectFunction(position)}
                  title="Выбрать существующую функцию"
                >
                  👁️ Выбрать
                </button>

                {func && (
                  <button
                    className="btn-action save"
                    onClick={() => handleSaveFunction(func, points, position)}
                    title="Сохранить в файл"
                  >
                    💾 Сохранить
                  </button>
                )}

                {func && (
                  <button
                    className="btn-action delete"
                    onClick={() => handleClear(position)}
                    title="Очистить"
                  >
                    🗑️ Очистить
                  </button>
                )}
              </>
            ) : (
              // Кнопки для результата
              <>
                <button className="btn-action save" onClick={handleSaveResult} title="Сохранить результат">
                  💾 Сохранить
                </button>
                <button className="btn-action export" onClick={() => handleExportResult('json')}>
                  📄 JSON
                </button>
                <button className="btn-action export" onClick={() => handleExportResult('binary')}>
                  💾 Binary
                </button>
                <button className="btn-action delete" onClick={() => handleClear('result')}>
                  🗑️ Очистить
                </button>
              </>
            )}
          </div>
        </div>

        {func ? (
          <div className="function-content">
            <div className="function-card">
              <h4 className="function-name">{func.name}</h4>
              <div className="function-meta">
                <span className="function-id">ID: {func.id}</span>
                <span className="function-type">{func.type || 'Табулированная'}</span>
                <span className="function-points">Точек: {points.length}</span>
                {func.isPublic && <span className="public-badge">🌐 Публичная</span>}
              </div>
            </div>

            {/* График */}
            {points.length > 0 && (
              <div className="graph-container compact">
                <GraphPreview
                  points={points}
                  xRange={graphRange.x}
                  yRange={graphRange.y}
                  isLoading={false}
                  showTitle={false}
                  compact={true}
                />
              </div>
            )}

            {/* Таблица точек */}
            <div className="points-table-container">
              <h4 className="table-title">Таблица точек</h4>
              <div className="points-table-wrapper">
                <table className="points-table">
                  <thead>
                    <tr>
                      <th width="50">№</th>
                      <th>X (нередактируемое)</th>
                      <th>Y {isResult ? '(результат)' : '(редактируемое)'}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {points.map((point, index) => (
                      <tr key={point.id}>
                        <td className="index-cell">{index + 1}</td>
                        <td className="x-cell">
                          <span className="x-value">
                            {typeof point.x === 'number' ? point.x.toFixed(4) : point.x}
                          </span>
                        </td>
                        <td className="y-cell">
                          {isResult ? (
                            // Для результата - только чтение
                            <span className="result-y">
                              {typeof point.y === 'number' ? point.y.toFixed(4) : point.y}
                            </span>
                          ) : (
                            // Для операндов - редактируемое поле
                            <input
                              type="number"
                              value={point.y}
                              onChange={(e) => handlePointChange(position, point.id, e.target.value)}
                              onBlur={(e) => handlePointChange(position, point.id, e.target.value)}
                              step="any"
                              className="y-input"
                              disabled={!point.editable}
                            />
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        ) : (
          <div className="empty-section">
            <p>Функция не выбрана</p>
            <p className="hint">Используйте кнопки сверху для создания или загрузки функции</p>
          </div>
        )}
      </div>
    );
  };

  // ============ РЕНДЕР ============
  return (
    <div className="operations-container">
      <div className="content-header">
        <h1 className="page-title">Операции с табулированными функциями</h1>
        <p className="page-description">
          Поэлементные операции над функциями: сложение, вычитание, умножение и деление.
          Редактируйте значения Y в таблицах операндов. X - нередактируемое поле.
        </p>
      </div>

      <div className="operations-content">
        <div className="operation-selector">
          <h3 className="section-title">Выберите операцию</h3>
          <div className="operation-buttons">
            {operations.map(op => (
              <button
                key={op.id}
                className={`operation-btn ${operation === op.id ? 'active' : ''}`}
                onClick={() => setOperation(op.id)}
              >
                {op.name} ({op.symbol})
              </button>
            ))}
          </div>
        </div>

        <div className="operations-grid">
          <FunctionTable
            title="Первая функция"
            func={function1}
            points={func1Points}
            position={1}
          />

          <div className="operation-center">
            <div className="operation-symbol">{operationSymbol(operation)}</div>
            <button
              className="btn-primary perform-operation"
              onClick={handleOperation}
              disabled={!function1 || !function2 || loadingResult}
            >
              {loadingResult ? (
                <>
                  <div className="loading-spinner small"></div>
                  Вычисление...
                </>
              ) : `Выполнить ${operations.find(op => op.id === operation)?.name?.toLowerCase()}`}
            </button>
            <div className="operation-hint">
              {function1 && function2
                ? `${function1.name} ${operationSymbol(operation)} ${function2.name}`
                : 'Выберите две функции'}
            </div>
          </div>

          <FunctionTable
            title="Вторая функция"
            func={function2}
            points={func2Points}
            position={2}
          />
        </div>

        <FunctionTable
          title="Результат операции"
          func={resultFunction}
          points={resultPoints}
          position="result"
          isResult={true}
        />
      </div>

      {/* Модальные окна */}
      <CreateFromArrayModal
        isOpen={showCreate1ArrayModal}
        onClose={() => setShowCreate1ArrayModal(false)}
        onFunctionCreated={(response) => handleCreateArrayFunction(1, response)}
      />
      <CreateFromArrayModal
        isOpen={showCreate2ArrayModal}
        onClose={() => setShowCreate2ArrayModal(false)}
        onFunctionCreated={(response) => handleCreateArrayFunction(2, response)}
      />
      <CreateFromFunctionModal
        isOpen={showCreate1FunctionModal}
        onClose={() => setShowCreate1FunctionModal(false)}
        onFunctionCreated={(response) => handleCreateMathFunction(1, response)}
      />
      <CreateFromFunctionModal
        isOpen={showCreate2FunctionModal}
        onClose={() => setShowCreate2FunctionModal(false)}
        onFunctionCreated={(response) => handleCreateMathFunction(2, response)}
      />
    </div>
  );
};

export default Operations;