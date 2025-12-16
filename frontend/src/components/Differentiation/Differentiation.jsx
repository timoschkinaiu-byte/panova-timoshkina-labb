import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import functionService from '../../services/functionService';
import notificationService from '../../services/notificationService';
import CreateFromArrayModal from '../Common/CreateFromArrayModal';
import CreateFromFunctionModal from '../Common/CreateFromFunctionModal';
import GraphModal from '../Common/GraphModal';
import '../../App.css';

const Differentiation = () => {
  const [sourceFunction, setSourceFunction] = useState(null);
  const [resultFunction, setResultFunction] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [isComputing, setIsComputing] = useState(false);

  const [showGraph, setShowGraph] = useState(false);
  const [graphPoints, setGraphPoints] = useState([]);
  const [graphTitle, setGraphTitle] = useState('');

  const [showArrayModal, setShowArrayModal] = useState(false);
  const [showFunctionModal, setShowFunctionModal] = useState(false);
  const [isLoadingFunction, setIsLoadingFunction] = useState(false);

  const navigate = useNavigate();

  // Загрузка точек функции с сервера (если они есть)
  const loadFunctionPoints = async (functionId, type) => {
    if (!functionId) return;
    try {
      const response = await functionService.getFunctionPoints(functionId);
      const points = response?.data?.map(p => ({ x: p.xValue, y: p.yValue })) || [];
      if (type === 'source') {
        setSourceFunction(prev => ({ ...prev, points }));
      } else if (type === 'result') {
        setResultFunction(prev => ({ ...prev, points }));
      }
    } catch (error) {
      console.error('Error loading function points:', error);
      notificationService.error('Ошибка загрузки точек функции');
    }
  };

  // Обработчики создания функций
  const handleArrayFunctionCreated = (serverResponse, numericPoints) => {
    if (!serverResponse?.data?.functionId) {
      notificationService.error('Ошибка: функция не была создана на сервере');
      return;
    }

    const newFunction = {
      functionId: serverResponse.data.functionId,
      functionName: serverResponse.data.functionName || 'Новая функция',
      points: numericPoints || [], // сразу передаем точки
      isPublic: serverResponse.data.isPublic || false
    };

    setSourceFunction(newFunction);
    notificationService.success(`Функция "${newFunction.functionName}" создана`);
  };

  const handleFunctionCreated = (serverResponse, numericPoints) => {
    if (!serverResponse?.data?.functionId) {
      notificationService.error('Ошибка: функция не была создана на сервере');
      return;
    }

    const newFunction = {
      functionId: serverResponse.data.functionId,
      functionName: serverResponse.data.functionName || 'Новая функция',
      points: numericPoints || [],
      isPublic: serverResponse.data.isPublic || false
    };

    setSourceFunction(newFunction);
    notificationService.success(`Функция "${newFunction.functionName}" создана`);
  };

  // Загрузка функции из файла
  const handleLoadFunction = async () => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.txt,.bin,.xml,.ser,.json';

    input.onchange = async (e) => {
      const file = e.target.files[0];
      if (!file) return;
      setIsLoadingFunction(true);
      try {
        const response = await functionService.importFunction(file, 'auto');
        if (response.status === 201 && response.data) {
          setSourceFunction({
            functionId: response.data.functionId,
            functionName: response.data.functionName,
            points: response.data.points?.map(p => ({ x: p.xValue, y: p.yValue })) || []
          });
          notificationService.success(`Функция "${response.data.functionName}" загружена`);
        }
      } catch (error) {
        console.error('Error loading function:', error);
        notificationService.error('Ошибка загрузки функции из файла');
      } finally {
        setIsLoadingFunction(false);
      }
    };

    input.click();
  };

  // Сохранение функции
  const saveFunction = (fn, name) => {
    if (!fn) {
      notificationService.error('Нет функции для сохранения');
      return;
    }
    setIsSaving(true);
    try {
      const dataStr = JSON.stringify(fn, null, 2);
      const dataBlob = new Blob([dataStr], { type: 'application/json' });
      const link = document.createElement('a');
      link.href = URL.createObjectURL(dataBlob);
      link.download = `${name || fn.functionName || 'function'}.json`;
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

  const handleSaveSource = () => saveFunction(sourceFunction, sourceFunction?.functionName);
  const handleSaveResult = () => saveFunction(resultFunction, resultFunction?.functionName);

  // Вычисление производной
  const handleDifferentiate = async () => {
    if (!sourceFunction?.functionId) {
      notificationService.error('Выберите функцию для дифференцирования');
      return;
    }
    setIsComputing(true);
    try {
      const response = await functionService.differentiateFunction(sourceFunction.functionId);
      if (response.status === 200 && response.data) {
        setResultFunction({
          functionId: response.data.functionId,
          functionName: `Производная: ${sourceFunction.functionName}`,
          points: response.data.points?.map(p => ({ x: p.xValue, y: p.yValue })) || []
        });
        notificationService.success('Дифференцирование выполнено успешно');
      } else {
        notificationService.error('Ошибка при вычислении производной');
      }
    } catch (error) {
      console.error('Error differentiating function:', error);
      notificationService.error('Ошибка при вычислении производной');
    } finally {
      setIsComputing(false);
    }
  };

  const handleShowGraph = (fn, title) => {
    if (!fn?.points?.length) {
      notificationService.error('Нет данных для графика');
      return;
    }
    setGraphPoints(fn.points);
    setGraphTitle(title);
    setShowGraph(true);
  };

  const handleClearSource = () => setSourceFunction(null);
  const handleClearResult = () => setResultFunction(null);

  const handleSourceYChange = async (index, newValue) => {
    if (!sourceFunction?.points?.length || !sourceFunction.functionId) return;
    const parsedValue = parseFloat(newValue);
    if (isNaN(parsedValue)) return;
    const originalPoint = sourceFunction.points[index];
    if (Math.abs(parsedValue - originalPoint.y) < 1e-10) return;

    try {
      await functionService.updatePointByReplacement(
        sourceFunction.functionId,
        originalPoint.x,
        parsedValue
      );
      const updatedPoints = [...sourceFunction.points];
      updatedPoints[index].y = parsedValue;
      setSourceFunction(prev => ({ ...prev, points: updatedPoints }));
    } catch (error) {
      console.error('Error updating point:', error);
      notificationService.error(`Ошибка: ${error.response?.data?.message || error.message}`);
    }
  };

  return (
    <div className="differentiation-container">
      <div className="content-header">
        <h1 className="page-title">Дифференцирование функций</h1>
        <p className="page-description">
          Вычислите производную табулированной функции. Выберите исходную функцию и нажмите "Вычислить производную".
        </p>
      </div>

      <div className="differentiation-grid">
        {/* Исходная функция */}
        <div className="function-panel">
          <div className="panel-header">
            <h3 className="panel-title">Исходная функция</h3>
            <div className="panel-actions">
              {sourceFunction && (
                <button className="btn-secondary btn-small" onClick={handleClearSource}>
                  Очистить
                </button>
              )}
              <button
                className="btn-secondary btn-small"
                onClick={() => handleShowGraph(sourceFunction, sourceFunction?.functionName)}
                disabled={!sourceFunction?.points?.length}
              >
                График
              </button>
            </div>
          </div>

          <div className="function-info">
            {sourceFunction ? (
              <div className="function-details">
                <div><strong>Название:</strong> {sourceFunction.functionName}</div>
                <div><strong>ID:</strong> {sourceFunction.functionId}</div>
                <div><strong>Точек:</strong> {sourceFunction.points?.length || 0}</div>
              </div>
            ) : (
              <div className="function-empty">
                <p>Функция не выбрана</p>
                <small>Создайте новую или загрузите существующую</small>
              </div>
            )}
          </div>

          <div className="function-actions">
            <button className="btn-secondary" onClick={() => setShowArrayModal(true)}>Создать из массивов</button>
            <button className="btn-secondary" onClick={() => setShowFunctionModal(true)}>Создать из функции</button>
            <button className="btn-secondary" onClick={handleLoadFunction}>
              {isLoadingFunction ? 'Загрузка...' : 'Загрузить из файла'}
            </button>
            <button className="btn-secondary" onClick={handleSaveSource} disabled={!sourceFunction}>
              {isSaving ? 'Сохранение...' : 'Сохранить'}
            </button>
          </div>

          {sourceFunction?.points?.length > 0 && (
            <div className="points-table-container">
              <h4>Таблица значений</h4>
              <div className="table-scroll">
                <table className="editable-table">
                  <thead>
                    <tr><th>X</th><th>Y</th></tr>
                  </thead>
                  <tbody>
                    {sourceFunction.points.map((p, i) => (
                      <tr key={i}>
                        <td>{p.x.toFixed(4)}</td>
                        <td>
                          <input
                            type="number"
                            value={p.y}
                            onChange={e => handleSourceYChange(i, e.target.value)}
                          />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>

        {/* Центральная панель */}
        <div className="operation-panel">
          <button className="btn-primary operation-button" onClick={handleDifferentiate} disabled={!sourceFunction}>
            {isComputing ? 'Вычисление...' : 'Вычислить производную →'}
          </button>
        </div>

        {/* Результат */}
        <div className="function-panel">
          <div className="panel-header">
            <h3 className="panel-title">Результат дифференцирования</h3>
            <div className="panel-actions">
              {resultFunction && (
                <button className="btn-secondary btn-small" onClick={handleClearResult}>Очистить</button>
              )}
              <button
                className="btn-secondary btn-small"
                onClick={() => handleShowGraph(resultFunction, resultFunction?.functionName)}
                disabled={!resultFunction?.points?.length}
              >
                График
              </button>
            </div>
          </div>

          <div className="function-info">
            {resultFunction ? (
              <div className="function-details">
                <div><strong>Название:</strong> {resultFunction.functionName}</div>
                <div><strong>ID:</strong> {resultFunction.functionId}</div>
                <div><strong>Точек:</strong> {resultFunction.points?.length || 0}</div>
              </div>
            ) : (
              <div className="function-empty">
                <p>Результат не вычислен</p>
                <small>Вычислите производную, чтобы увидеть результат</small>
              </div>
            )}
          </div>

          <button className="btn-secondary" onClick={handleSaveResult} disabled={!resultFunction}>
            {isSaving ? 'Сохранение...' : 'Сохранить результат'}
          </button>

          {resultFunction?.points?.length > 0 && (
            <div className="points-table-container">
              <h4>Таблица значений производной</h4>
              <div className="table-scroll">
                <table className="result-table">
                  <thead>
                    <tr><th>X</th><th>Y'</th></tr>
                  </thead>
                  <tbody>
                    {resultFunction.points.map((p, i) => (
                      <tr key={i}>
                        <td>{p.x?.toFixed(4)}</td>
                        <td>{p.y?.toFixed(6)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      </div>

      <CreateFromArrayModal
        isOpen={showArrayModal}
        onClose={() => setShowArrayModal(false)}
        onFunctionCreated={handleArrayFunctionCreated}
      />

      <CreateFromFunctionModal
        isOpen={showFunctionModal}
        onClose={() => setShowFunctionModal(false)}
        onFunctionCreated={handleFunctionCreated}
      />

      <GraphModal
        isOpen={showGraph}
        onClose={() => setShowGraph(false)}
        points={graphPoints}
        title={graphTitle}
      />
    </div>
  );
};

export default Differentiation;
