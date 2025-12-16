import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import functionService from '../../services/functionService';
import notificationService from '../../services/notificationService';
import CreateFromArrayModal from '../Common/CreateFromArrayModal';
import CreateFromFunctionModal from '../Common/CreateFromFunctionModal';
import GraphModal from '../Common/GraphModal';
import '../../App.css';

const Integration = () => {
  const [sourceFunction, setSourceFunction] = useState(null);
  const [resultFunction, setResultFunction] = useState(null);
  const [isSaving, setIsSaving] = useState(false);
  const [isComputing, setIsComputing] = useState(false);
  const [isLoadingFunction, setIsLoadingFunction] = useState(false);

  const [showGraph, setShowGraph] = useState(false);
  const [graphPoints, setGraphPoints] = useState([]);
  const [graphTitle, setGraphTitle] = useState('');

  const [showArrayModal, setShowArrayModal] = useState(false);
  const [showFunctionModal, setShowFunctionModal] = useState(false);

  const [threadsCount, setThreadsCount] = useState(1);
  const [maxThreads, setMaxThreads] = useState(8);

  const navigate = useNavigate();

  useEffect(() => {
    const userMaxThreads = 8;
    setMaxThreads(userMaxThreads);

    if (threadsCount > userMaxThreads) {
      setThreadsCount(userMaxThreads);
    }
  }, []);

  const handleThreadsChange = (value) => {
    const numValue = parseInt(value);
    if (!isNaN(numValue) && numValue > 0 && numValue <= maxThreads) {
      setThreadsCount(numValue);
    }
  };

  const handleLoadFunction = async () => {
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

        setSourceFunction({
          functionId,
          functionName: response.data.functionName,
          points,
          isPublic: response.data.isPublic || false
        });

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

  const handleArrayFunctionCreated = (serverResponse, numericPoints) => {
    if (!serverResponse?.data?.functionId) {
      notificationService.error('Ошибка: функция не была создана на сервере');
      return;
    }
    setSourceFunction({
      functionId: serverResponse.data.functionId,
      functionName: serverResponse.data.functionName || 'Новая функция',
      points: numericPoints || [],
      isPublic: serverResponse.data.isPublic || false
    });
    notificationService.success(`Функция "${serverResponse.data.functionName}" создана`);
  };

  const handleFunctionCreated = (serverResponse, numericPoints) => {
    if (!serverResponse?.data?.functionId) {
      notificationService.error('Ошибка: функция не была создана на сервере');
      return;
    }
    setSourceFunction({
      functionId: serverResponse.data.functionId,
      functionName: serverResponse.data.functionName || 'Новая функция',
      points: numericPoints || [],
      isPublic: serverResponse.data.isPublic || false
    });
    notificationService.success(`Функция "${serverResponse.data.functionName}" создана`);
  };

  const handleIntegrate = async () => {
    if (!sourceFunction?.functionId) {
      notificationService.error('Выберите функцию для интегрирования');
      return;
    }

    setIsComputing(true);
    try {
      const response = await functionService.integrateFunction(
        sourceFunction.functionId,
        threadsCount
      );

      if (response.status === 200 && response.data) {
        const integrationResult = response.data.result;
        const computationTime = response.data.computationTime;

        setResultFunction({
          value: integrationResult,
          computationTime: computationTime,
          functionName: `Интеграл от "${sourceFunction.functionName}"`,
          sourceFunctionId: sourceFunction.functionId,
          threadsUsed: threadsCount,
          isParallel: threadsCount > 1
        });

        notificationService.success(
          `Интегрирование выполнено (${threadsCount} потоков). Результат: ${integrationResult.toFixed(6)}`
        );
      } else {
        notificationService.error('Ошибка при вычислении интеграла');
      }
    } catch (error) {
      console.error('Error integrating function:', error);
      notificationService.error('Ошибка при вычислении интеграла');
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
    <div className="integration-container">
      <div className="content-header">
        <h1 className="page-title">Интегрирование функций</h1>
        <p className="page-description">
          Вычислите определенный интеграл табулированной функции по всей области определения.
          Вы можете выбрать количество потоков для параллельных вычислений.
        </p>
      </div>

      <div className="integration-grid">
        <div className="function-panel main-panel">
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
                <div className="detail-row">
                  <span className="detail-label">Название:</span>
                  <span className="detail-value">{sourceFunction.functionName}</span>
                </div>
                <div className="detail-row">
                  <span className="detail-label">ID:</span>
                  <span className="detail-value">{sourceFunction.functionId}</span>
                </div>
                <div className="detail-row">
                  <span className="detail-label">Точек:</span>
                  <span className="detail-value">{sourceFunction.points?.length || 0}</span>
                </div>
                <div className="detail-row">
                  <span className="detail-label">Область определения:</span>
                  <span className="detail-value">
                    [{sourceFunction.points?.[0]?.x?.toFixed(3) || 0},
                     {sourceFunction.points?.[sourceFunction.points?.length - 1]?.x?.toFixed(3) || 0}]
                  </span>
                </div>
              </div>
            ) : (
              <div className="function-empty">
                <p>Функция не выбрана</p>
                <small>Создайте новую или загрузите существующую</small>
              </div>
            )}
          </div>

          <div className="function-actions">
            <div className="action-buttons-row">
              <button className="btn-secondary" onClick={() => setShowArrayModal(true)}>
                Создать из массивов
              </button>
              <button className="btn-secondary" onClick={() => setShowFunctionModal(true)}>
                Создать из функции
              </button>
              <button className="btn-secondary" onClick={handleLoadFunction}>
                {isLoadingFunction ? 'Загрузка...' : 'Загрузить из файла'}
              </button>
            </div>

            <small style={{ color: 'var(--text-secondary)', marginTop: '8px', display: 'block' }}>
              Поддерживаемые форматы: .txt, .bin, .json, .xml
            </small>

            {sourceFunction && (
              <button
                className="btn-primary"
                onClick={handleSaveSource}
                style={{ marginTop: '15px' }}
              >
                {isSaving ? 'Сохранение...' : 'Сохранить функцию'}
              </button>
            )}
          </div>

          {sourceFunction?.points?.length > 0 && (
            <>
              <div className="integration-settings">
                <div className="settings-row">
                  <div className="form-group">
                    <label className="form-label">Количество потоков</label>
                    <div className="threads-input-container">
                      <input
                        type="number"
                        className="form-input"
                        value={threadsCount}
                        onChange={(e) => {
                          const value = parseInt(e.target.value);
                          if (value > 0 && value <= maxThreads) {
                            setThreadsCount(value);
                          } else if (value > maxThreads) {
                            setThreadsCount(maxThreads);
                            notificationService.warning(`Максимальное количество потоков: ${maxThreads}`);
                          }
                        }}
                        min="1"
                        max={maxThreads}
                        step="1"
                        disabled={isComputing || !sourceFunction}
                        style={{ width: '80px' }}
                      />
                      <div className="threads-hint">
                        <small>Максимум: {maxThreads} потоков</small>
                        <div className="threads-slider">
                          <input
                            type="range"
                            min="1"
                            max={maxThreads}
                            value={threadsCount}
                            onChange={(e) => setThreadsCount(parseInt(e.target.value))}
                            disabled={isComputing || !sourceFunction}
                            className= "range-slider"
                          />
                        </div>
                      </div>
                    </div>
                  </div>

                  <div className="form-group">
                    <label className="form-label">Тип вычислений</label>
                    <div className="computation-type">
                      <div className="computation-option">
                        <input
                          type="radio"
                          id="type-sequential"
                          name="computationType"
                          checked={threadsCount === 1}
                          onChange={() => setThreadsCount(1)}
                          disabled={isComputing || !sourceFunction}
                        />
                        <label
                          htmlFor="type-sequential"
                          className={isComputing || !sourceFunction ? 'disabled' : ''}
                        >
                          Последовательное
                        </label>
                      </div>
                      <div className="computation-option">
                        <input
                          type="radio"
                          id="type-parallel"
                          name="computationType"
                          checked={threadsCount > 1}
                          onChange={() => setThreadsCount(2)}
                          disabled={isComputing || !sourceFunction || maxThreads === 1}
                        />
                        <label
                          htmlFor="type-parallel"
                          className={isComputing || !sourceFunction || maxThreads === 1 ? 'disabled' : ''}
                        >
                          Параллельное
                        </label>
                      </div>
                    </div>
                    {threadsCount > 1 && (
                      <div className="parallel-info">
                        <small style={{ color: 'var(--accent-color)' }}>
                          Параллельное выполнение: {threadsCount} потоков
                        </small>
                      </div>
                    )}
                  </div>
                </div>

                <div className="performance-info">
                  <div className="info-item">
                    <span className="info-icon">P</span>
                    <span className="info-text">
                      {threadsCount === 1 ? 'Последовательное выполнение' : `Параллельное выполнение (${threadsCount} потоков)`}
                    </span>
                  </div>
                  <div className="info-item">
                    <span className="info-icon">S</span>
                    <span className="info-text">
                      Ожидаемое ускорение: {threadsCount === 1 ? '1x' : `до ${Math.min(threadsCount, 2.5).toFixed(1)}x`}
                    </span>
                  </div>
                  {threadsCount > maxThreads && (
                    <div className="info-item warning">
                      <span className="info-icon">!</span>
                      <span className="info-text">
                        Превышен лимит потоков. Установлено: {maxThreads}
                      </span>
                    </div>
                  )}
                </div>
              </div>

              <div className="integration-button-container">
                <button
                  className="btn-primary integrate-button"
                  onClick={handleIntegrate}
                  disabled={!sourceFunction || isComputing}
                >
                  {isComputing ? (
                    <div className="computing-status">
                      Вычисление...
                      <div className="computing-animation">
                        {[...Array(threadsCount)].map((_, i) => (
                          <div key={i} className="thread-dot"></div>
                        ))}
                      </div>
                    </div>
                  ) : (
                    `Вычислить интеграл (${threadsCount} поток${threadsCount === 1 ? '' : 'ов'})`
                  )}
                </button>
              </div>

              <div className="points-table-container">
                <h4>Таблица значений функции</h4>
                <div className="table-scroll">
                  <table className="editable-table">
                    <thead>
                      <tr>
                        <th>X</th>
                        <th>Y</th>
                        <th>Редактировать</th>
                      </tr>
                    </thead>
                    <tbody>
                      {sourceFunction.points.map((p, i) => (
                        <tr key={i}>
                          <td className="x-cell">{p.x.toFixed(4)}</td>
                          <td className="y-cell">{p.y.toFixed(4)}</td>
                          <td className="edit-cell">
                            <input
                              type="number"
                              value={p.y}
                              onChange={e => handleSourceYChange(i, e.target.value)}
                              step="any"
                              className="y-input"
                            />
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <div className="table-info">
                  <small>Измените значение Y и нажмите Enter для обновления</small>
                </div>
              </div>
            </>
          )}
        </div>

        <div className="result-panel">
          <div className="result-header">
            <h3 className="panel-title">Результат интегрирования</h3>
            <div className="panel-actions">
              {resultFunction && (
                <button className="btn-secondary btn-small" onClick={handleClearResult}>
                  Очистить
                </button>
              )}
            </div>
          </div>

          <div className="result-content">
            {resultFunction ? (
              <div className="result-card">
                <div className="result-icon">∫</div>
                <div className="result-main">
                  <div className="result-title">Значение интеграла</div>
                  <div className="result-value">
                    {resultFunction.value !== undefined ? resultFunction.value.toFixed(6) : 'Нет данных'}
                  </div>
                </div>
                <div className="result-details">
                  <div className="result-detail">
                    <span className="detail-label">Функция:</span>
                    <span className="detail-value">{resultFunction.functionName}</span>
                  </div>
                  <div className="result-detail">
                    <span className="detail-label">Потоков:</span>
                    <span className="detail-value">
                      {resultFunction.threadsUsed || 1}
                      {resultFunction.isParallel && ' (параллельно)'}
                    </span>
                  </div>
                  <div className="result-detail">
                    <span className="detail-label">Время:</span>
                    <span className="detail-value">{resultFunction.computationTime?.toFixed(3)} мс</span>
                  </div>
                  <div className="result-detail">
                    <span className="detail-label">Производительность:</span>
                    <span className="detail-value">
                      {resultFunction.threadsUsed > 1 ?
                        `~${(resultFunction.computationTime / resultFunction.threadsUsed).toFixed(1)} мс на поток` :
                        'Последовательно'}
                    </span>
                  </div>
                  <div className="result-detail">
                    <span className="detail-label">Область:</span>
                    <span className="detail-value">
                      [{sourceFunction?.points?.[0]?.x?.toFixed(3) || 0},
                       {sourceFunction?.points?.[sourceFunction?.points?.length - 1]?.x?.toFixed(3) || 0}]
                    </span>
                  </div>
                </div>

                <div className="result-actions">
                  <button
                    className="btn-secondary"
                    onClick={handleSaveResult}
                    style={{ width: '100%' }}
                  >
                    {isSaving ? 'Сохранение...' : 'Сохранить результат'}
                  </button>
                </div>
              </div>
            ) : (
              <div className="result-empty">
                <div className="empty-icon">∫</div>
                <p>Интеграл не вычислен</p>
                <small>Вычислите интеграл функции, чтобы увидеть результат</small>
              </div>
            )}
          </div>

          <div className="integration-info">
            <h4>Информация об интегрировании</h4>
            <ul className="info-list">
              <li>Интеграл вычисляется по всей области определения функции</li>
              <li>Используется численный метод интегрирования</li>
              <li>Результат - определенный интеграл ∫f(x)dx</li>
              <li>Интегрирование выполняется на сервере</li>
              <li>Параллельные вычисления ускоряют обработку больших функций</li>
              <li>Рекомендуется использовать 2-4 потока для оптимальной производительности</li>
            </ul>
          </div>
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

export default Integration;