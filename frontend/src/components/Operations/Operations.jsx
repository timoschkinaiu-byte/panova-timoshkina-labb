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

  // Выбранные функции
  const [function1, setFunction1] = useState(null);
  const [function2, useState2] = useState(null);
  const [resultFunction, setResultFunction] = useState(null);

  // Точки функций
  const [func1Points, setFunc1Points] = useState([]);
  const [func2Points, setFunc2Points] = useState([]);
  const [resultPoints, setResultPoints] = useState([]);

  // Настройки операций
  const [operation, setOperation] = useState('add');
  const [loadingResult, setLoadingResult] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [isLoadingFunction, setIsLoadingFunction] = useState(false);

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
    if (function1?.functionId) {
      loadFunctionPoints(function1.functionId, setFunc1Points);
    } else {
      setFunc1Points([]);
    }
  }, [function1]);

  useEffect(() => {
    if (function2?.functionId) {
      loadFunctionPoints(function2.functionId, setFunc2Points);
    } else {
      setFunc2Points([]);
    }
  }, [function2]);

  useEffect(() => {
    if (resultFunction?.functionId) {
      loadFunctionPoints(resultFunction.functionId, setResultPoints);
    } else {
      setResultPoints([]);
    }
  }, [resultFunction]);

  // ============ ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ============
  const loadFunctions = async () => {
    try {
      const response = await functionService.getMyFunctions();
      setFunctions(response.data || []);
    } catch (error) {
      console.error('Ошибка загрузки функций:', error);
      notificationService.error('Ошибка загрузки функций');
    }
  };

  const loadFunctionPoints = async (functionId, setter) => {
    try {
      const response = await functionService.getFunctionPoints(functionId);
      const points = response.data || [];

      const formattedPoints = points.map((point, index) => ({
        id: index,
        x: point.xvalue ?? point.xValue ?? point.x ?? 0,
        y: point.yvalue ?? point.yValue ?? point.y ?? 0
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

  // ============ СОХРАНЕНИЕ И ЗАГРУЗКА ============
  const saveFunction = (func, points, name) => {
    if (!func || !points || points.length === 0) {
      notificationService.error('Нет функции для сохранения');
      return;
    }

    setIsSaving(true);
    try {
      const dataStr = JSON.stringify({
        functionName: func.functionName,
        functionId: func.functionId,
        points: points,
        isPublic: func.isPublic || false
      }, null, 2);

      const dataBlob = new Blob([dataStr], { type: 'application/json' });
      const link = document.createElement('a');
      link.href = URL.createObjectURL(dataBlob);
      link.download = `${name || func.functionName || 'function'}.json`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      notificationService.success('Функция сохранена');
    } catch (error) {
      console.error('Error saving function:', error);
      notificationService.error('Ошибка сохранения функции');
    } finally {
      setIsSaving(false);
    }
  };

  const handleSaveFunction1 = () => saveFunction(function1, func1Points, function1?.functionName);
  const handleSaveFunction2 = () => saveFunction(function2, func2Points, function2?.functionName);
  const handleSaveResult = () => saveFunction(resultFunction, resultPoints, resultFunction?.functionName);

  const handleLoadFunction = async (position) => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.txt,.bin,.json,.xml';

    input.onchange = async (e) => {
      const file = e.target.files[0];
      if (!file) return;

      setIsLoadingFunction(true);
      try {
        let format = 'auto';
        const fileName = file.name.toLowerCase();
        if (fileName.endsWith('.txt')) format = 'text';
        else if (fileName.endsWith('.bin')) format = 'binary';
        else if (fileName.endsWith('.xml')) format = 'xml';
        else if (fileName.endsWith('.json')) format = 'json';

        const response = await functionService.importFunction(file, format);

        if (!response?.data?.functionId) {
          throw new Error('Функция не была создана на сервере');
        }

        const functionId = response.data.functionId;

        const pointsResponse = await functionService.getFunctionPoints(functionId);
        const points = (pointsResponse?.data || []).map(p => ({
          x: p.xValue ?? p.xvalue ?? p.x,
          y: p.yValue ?? p.yvalue ?? p.y
        }));

        const loadedFunc = {
          functionId,
          functionName: response.data.functionName,
          points,
          isPublic: response.data.isPublic || false
        };

        if (position === 1) {
          setFunction1(loadedFunc);
        } else {
          setFunction2(loadedFunc);
        }

        setFunctions(prev => [...prev, response.data]);
        notificationService.success(`Функция "${response.data.functionName}" загружена`);
      } catch (error) {
        console.error('Error loading function:', error);
        notificationService.error(
          error.response?.data?.message || 'Ошибка загрузки из файла'
        );
      } finally {
        setIsLoadingFunction(false);
      }
    };

    input.click();
  };

  // ============ ОБРАБОТЧИКИ ДЕЙСТВИЙ ============
  const handleSelectFunction = (position) => {
    if (functions.length === 0) {
      notificationService.error('Нет доступных функций');
      return;
    }

    const selectedId = parseInt(prompt(`Выберите ID функции:\n${functions.map(f => `ID: ${f.functionId} - "${f.functionName}"`).join('\n')}`, ''));

    if (!selectedId) return;

    const selectedFunc = functions.find(f => f.functionId === selectedId);

    if (selectedFunc) {
      const funcInfo = {
        functionId: selectedFunc.functionId,
        functionName: selectedFunc.functionName,
        isPublic: selectedFunc.isPublic || false
      };

      if (position === 1) {
        setFunction1(funcInfo);
      } else {
        setFunction2(funcInfo);
      }
    } else {
      notificationService.error('Функция не найдена');
    }
  };

  const handleArrayFunctionCreated = (position, serverResponse) => {
    if (!serverResponse?.data?.functionId) {
      notificationService.error('Ошибка: функция не была создана на сервере');
      return;
    }

    const funcInfo = {
      functionId: serverResponse.data.functionId,
      functionName: serverResponse.data.functionName || 'Новая функция',
      isPublic: serverResponse.data.isPublic || false
    };

    if (position === 1) {
      setFunction1(funcInfo);
      setShowCreate1ArrayModal(false);
    } else {
      setFunction2(funcInfo);
      setShowCreate2ArrayModal(false);
    }

    setFunctions(prev => [...prev, serverResponse.data]);
    notificationService.success(`Функция "${serverResponse.data.functionName}" создана`);
  };

  const handleFunctionCreated = (position, serverResponse) => {
    if (!serverResponse?.data?.functionId) {
      notificationService.error('Ошибка: функция не была создана на сервере');
      return;
    }

    const funcInfo = {
      functionId: serverResponse.data.functionId,
      functionName: serverResponse.data.functionName || 'Новая функция',
      isPublic: serverResponse.data.isPublic || false
    };

    if (position === 1) {
      setFunction1(funcInfo);
      setShowCreate1FunctionModal(false);
    } else {
      setFunction2(funcInfo);
      setShowCreate2FunctionModal(false);
    }

    setFunctions(prev => [...prev, serverResponse.data]);
    notificationService.success(`Функция "${serverResponse.data.functionName}" создана`);
  };

  const handleOperation = async () => {
    if (!function1?.functionId || !function2?.functionId) {
      notificationService.warning('Выберите обе функции');
      return;
    }

    if (function1.functionId === function2.functionId) {
      notificationService.warning('Выберите разные функции');
      return;
    }

    setLoadingResult(true);
    try {
      const resultName = `${function1.functionName} ${operations.find(op => op.id === operation)?.symbol || '?'} ${function2.functionName}`;

      const endpoint = `/operations/${operation}`;
      const requestData = {
        functionId1: function1.functionId,
        functionId2: function2.functionId,
        resultName: resultName,
        isPublic: false,
        factoryType: 'ARRAY'
      };

      const response = await functionService.api.post(endpoint, requestData);

      if (response?.data) {
        const resultData = response.data;
        const resultFuncData = {
          functionId: resultData.functionId,
          functionName: resultData.functionName || resultName,
          isPublic: resultData.isPublic || false
        };

        setResultFunction(resultFuncData);
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

  const handlePointChange = async (position, index, newY) => {
    const newYNum = parseFloat(newY);
    if (isNaN(newYNum)) {
      notificationService.error('Введите корректное число');
      return;
    }

    if (position === 1 && function1?.functionId && func1Points[index]) {
      const point = func1Points[index];

      try {
        await functionService.updatePointByReplacement(
          function1.functionId,
          point.x,
          newYNum
        );

        const updatedPoints = [...func1Points];
        updatedPoints[index].y = newYNum;
        setFunc1Points(updatedPoints);

        notificationService.success('Значение Y обновлено');
      } catch (error) {
        console.error('Error updating point:', error);
        notificationService.error(`Ошибка: ${error.response?.data?.message || error.message}`);
      }
    } else if (position === 2 && function2?.functionId && func2Points[index]) {
      const point = func2Points[index];

      try {
        await functionService.updatePointByReplacement(
          function2.functionId,
          point.x,
          newYNum
        );

        const updatedPoints = [...func2Points];
        updatedPoints[index].y = newYNum;
        setFunc2Points(updatedPoints);

        notificationService.success('Значение Y обновлено');
      } catch (error) {
        console.error('Error updating point:', error);
        notificationService.error(`Ошибка: ${error.response?.data?.message || error.message}`);
      }
    }
  };

  // ============ КОМПОНЕНТ ПАНЕЛИ ФУНКЦИИ ============
  const FunctionPanel = ({
    title,
    func,
    points,
    position,
    isResult = false
  }) => {
    return (
      <div className="function-panel">
        <div className="panel-header">
          <h3 className="panel-title">{title}</h3>
          <div className="panel-actions">
            {!isResult ? (
              <>
                <button
                  className="btn-secondary btn-small"
                  onClick={() => handleClear(position)}
                >
                  Очистить
                </button>
              </>
            ) : (
              <button
                className="btn-secondary btn-small"
                onClick={() => handleClear('result')}
              >
                Очистить
              </button>
            )}
          </div>
        </div>

        <div className="function-info">
          {func ? (
            <div className="function-details">
              <div className="detail-row">
                <span className="detail-label">Название:</span>
                <span className="detail-value">{func.functionName}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">ID:</span>
                <span className="detail-value">{func.functionId}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Точек:</span>
                <span className="detail-value">{points.length}</span>
              </div>
              {func.isPublic && (
                <div className="detail-row">
                  <span className="detail-label">Статус:</span>
                  <span className="detail-value">🌐 Публичная</span>
                </div>
              )}
            </div>
          ) : (
            <div className="function-empty">
              <p>Функция не выбрана</p>
              <small>Создайте новую или загрузите существующую</small>
            </div>
          )}
        </div>

        <div className="function-actions">
          {!isResult && (
            <div className="action-buttons">
              <button
                className="btn-secondary"
                onClick={() => position === 1 ? setShowCreate1ArrayModal(true) : setShowCreate2ArrayModal(true)}
              >
                Создать из массивов
              </button>
              <button
                className="btn-secondary"
                onClick={() => position === 1 ? setShowCreate1FunctionModal(true) : setShowCreate2FunctionModal(true)}
              >
                Создать из функции
              </button>
              <button
                className="btn-secondary"
                onClick={() => handleLoadFunction(position)}
              >
                {isLoadingFunction ? 'Загрузка...' : 'Загрузить из файла'}
              </button>
              <button
                className="btn-secondary"
                onClick={() => handleSelectFunction(position)}
              >
                Выбрать существующую
              </button>
            </div>
          )}

          {func && (
            <button
              className="btn-primary"
              onClick={isResult ? handleSaveResult : (position === 1 ? handleSaveFunction1 : handleSaveFunction2)}
              style={{ marginTop: '15px' }}
            >
              {isSaving ? 'Сохранение...' : 'Сохранить функцию'}
            </button>
          )}

          {!isResult && func && (
            <small style={{ color: 'var(--text-secondary)', marginTop: '8px', display: 'block' }}>
              Поддерживаемые форматы: .txt, .bin, .json, .xml
            </small>
          )}
        </div>

        {points.length > 0 && (
          <div className="points-table-container">
            <h4>Таблица значений функции</h4>
            <div className="table-scroll">
              <table className="editable-table">
                <thead>
                  <tr>
                    <th>X</th>
                    <th>Y</th>
                    {!isResult && <th>Редактировать</th>}
                  </tr>
                </thead>
                <tbody>
                  {points.map((point, index) => (
                    <tr key={index}>
                      <td className="x-cell">{point.x.toFixed(4)}</td>
                      <td className="y-cell">{point.y.toFixed(4)}</td>
                      {!isResult && (
                        <td className="edit-cell">
                          <input
                            type="number"
                            value={point.y}
                            onChange={e => handlePointChange(position, index, e.target.value)}
                            step="any"
                            className="y-input"
                          />
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {!isResult && (
              <div className="table-info">
                <small>Измените значение Y и нажмите Enter для обновления</small>
              </div>
            )}
          </div>
        )}
      </div>
    );
  };

  // ============ РЕНДЕР ============
  return (
    <div className="operations-container">
      <div className="content-header">
        <h1 className="page-title">Операции с функциями</h1>
        <p className="page-description">
          Поэлементные операции над двумя табулированными функциями: сложение, вычитание, умножение и деление.
          Редактируйте значения Y в таблицах операндов. X - нередактируемое поле.
        </p>
      </div>

      <div className="differentiation-grid">
        <FunctionPanel
          title="Первая функция"
          func={function1}
          points={func1Points}
          position={1}
        />

        <div className="operation-panel">
          <div className="operation-center">
            <div className="operation-info">
              <h3>Выберите операцию</h3>
              <div className="operation-buttons">
                {operations.map(op => (
                  <button
                    key={op.id}
                    className={`btn-secondary ${operation === op.id ? 'active' : ''}`}
                    onClick={() => setOperation(op.id)}
                    style={{ margin: '5px' }}
                  >
                    {op.symbol} {op.name}
                  </button>
                ))}
              </div>
              <button
                className="btn-primary operation-button"
                onClick={handleOperation}
                disabled={!function1 || !function2 || loadingResult}
              >
                {loadingResult ? (
                  <div className="computing-status">
                    Вычисление...
                    <div className="computing-animation">
                      <div className="thread-dot"></div>
                      <div className="thread-dot"></div>
                      <div className="thread-dot"></div>
                    </div>
                  </div>
                ) : (
                  `Выполнить ${operations.find(op => op.id === operation)?.name?.toLowerCase()}`
                )}
              </button>
              <div className="operation-hint">
                {function1 && function2
                  ? `${function1.functionName} ${operations.find(op => op.id === operation)?.symbol} ${function2.functionName}`
                  : 'Выберите две функции для операции'}
              </div>
            </div>
          </div>
        </div>

        <FunctionPanel
          title="Вторая функция"
          func={function2}
          points={func2Points}
          position={2}
        />
      </div>

      <div className="function-panel result-panel" style={{ marginTop: '30px' }}>
        <div className="panel-header">
          <h3 className="panel-title">Результат операции</h3>
          <div className="panel-actions">
            {resultFunction && (
              <button className="btn-secondary btn-small" onClick={() => handleClear('result')}>
                Очистить
              </button>
            )}
          </div>
        </div>

        <div className="function-info">
          {resultFunction ? (
            <div className="function-details">
              <div className="detail-row">
                <span className="detail-label">Название:</span>
                <span className="detail-value">{resultFunction.functionName}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">ID:</span>
                <span className="detail-value">{resultFunction.functionId}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Точек:</span>
                <span className="detail-value">{resultPoints.length}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Операция:</span>
                <span className="detail-value">
                  {function1?.functionName} {operations.find(op => op.id === operation)?.symbol} {function2?.functionName}
                </span>
              </div>
            </div>
          ) : (
            <div className="function-empty">
              <p>Результат не вычислен</p>
              <small>Выполните операцию над функциями, чтобы увидеть результат</small>
            </div>
          )}
        </div>

        {resultFunction && (
          <div className="function-actions">
            <button
              className="btn-primary"
              onClick={handleSaveResult}
              style={{ marginTop: '15px' }}
            >
              {isSaving ? 'Сохранение...' : 'Сохранить результат'}
            </button>
          </div>
        )}

        {resultPoints.length > 0 && (
          <div className="points-table-container">
            <h4>Таблица значений результата</h4>
            <div className="table-scroll">
              <table className="result-table">
                <thead>
                  <tr>
                    <th>X</th>
                    <th>Y (результат)</th>
                  </tr>
                </thead>
                <tbody>
                  {resultPoints.map((point, index) => (
                    <tr key={index}>
                      <td className="x-cell">{point.x.toFixed(4)}</td>
                      <td className="y-cell">{point.y.toFixed(4)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="table-info">
              <small>Результат операции (только для чтения)</small>
            </div>
          </div>
        )}
      </div>

      {/* Модальные окна */}
      <CreateFromArrayModal
        isOpen={showCreate1ArrayModal}
        onClose={() => setShowCreate1ArrayModal(false)}
        onFunctionCreated={(response) => handleArrayFunctionCreated(1, response)}
      />
      <CreateFromArrayModal
        isOpen={showCreate2ArrayModal}
        onClose={() => setShowCreate2ArrayModal(false)}
        onFunctionCreated={(response) => handleArrayFunctionCreated(2, response)}
      />
      <CreateFromFunctionModal
        isOpen={showCreate1FunctionModal}
        onClose={() => setShowCreate1FunctionModal(false)}
        onFunctionCreated={(response) => handleFunctionCreated(1, response)}
      />
      <CreateFromFunctionModal
        isOpen={showCreate2FunctionModal}
        onClose={() => setShowCreate2FunctionModal(false)}
        onFunctionCreated={(response) => handleFunctionCreated(2, response)}
      />
    </div>
  );
};

export default Operations;