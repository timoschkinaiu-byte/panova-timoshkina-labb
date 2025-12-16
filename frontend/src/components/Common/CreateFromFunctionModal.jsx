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

  const [isGraphModalOpen, setIsGraphModalOpen] = useState(false);
  const [graphData, setGraphData] = useState([]);
  const [isGeneratingGraph, setIsGeneratingGraph] = useState(false);

  const [error, setError] = useState('');
  const [isCreating, setIsCreating] = useState(false);

  useEffect(() => {
    if (isOpen) loadAvailableFunctions();
  }, [isOpen]);

  const loadAvailableFunctions = async () => {
    setIsLoadingFunctions(true);
    try {
      const allFunctions = await functionService.getAvailableFunctions();
      setAvailableFunctions(allFunctions);
    } catch (err) {
      console.error('Error loading available functions:', err);
      setAvailableFunctions(functionService.getAvailableMathFunctions());
    } finally {
      setIsLoadingFunctions(false);
    }
  };

  const handleFunctionSelect = (functionKey) => {
    setSelectedFunctionKey(functionKey);
    setFunctionParams(null);

    const func = availableFunctions.find(f => f.key === functionKey);
    if (func?.requiresParams || func?.requiresValue) {
      setIsParamsModalOpen(true);
    }
  };

  const handleParamsConfirm = (params) => {
    setFunctionParams(params);
    setIsParamsModalOpen(false);
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

  const generatePreviewGraph = async () => {
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

    if (left >= right) {
      setError('Левый край должен быть меньше правого');
      return;
    }
    if (count < 2) {
      setError('Количество точек должно быть не менее 2');
      return;
    }

    const func = availableFunctions.find(f => f.key === selectedFunctionKey);
    if ((func?.requiresParams || func?.requiresValue) && !functionParams) {
      setError('Заполните параметры функции');
      return;
    }

    setIsGeneratingGraph(true);
    setError('');

    try {
      const requestData = {
        name: `Preview: ${selectedFunctionKey}`,
        sourceFunctionKey: functionParams?.functionKey || selectedFunctionKey,
        leftX: left,
        rightX: right,
        pointsCount: count,
        isPublic: false
      };

      if (functionParams?.params) Object.assign(requestData, functionParams.params);

      const response = await functionService.createFromMathFunction(requestData);
      if (!response.data?.functionId) throw new Error('Не удалось получить ID функции');

      const functionId = response.data.functionId;

      const pointsResponse = await functionService.getFunctionPoints(functionId);
      const points = pointsResponse?.data?.points || [];

      if (!points.length) {
        for (let i = 0; i < count; i++) {
          const x = left + ((right - left) / (count - 1)) * i;
          points.push({ x, y: 0 });
        }
      }

      setGraphData(points);
      setIsGraphModalOpen(true);

      setTimeout(() => {
        functionService.deleteFunction(functionId).catch(console.error);
      }, 5000);
    } catch (err) {
      console.error('Error generating preview graph:', err);
      setError(err.response?.data?.message || 'Ошибка построения графика');
    } finally {
      setIsGeneratingGraph(false);
    }
  };

  const handleCreate = async () => {
    setError('');
    if (!functionName.trim()) { setError('Введите название функции'); return; }
    if (!selectedFunctionKey) { setError('Выберите математическую функцию'); return; }
    if (!leftX || !rightX) { setError('Введите интервал'); return; }

    const left = parseFloat(leftX);
    const right = parseFloat(rightX);
    const count = parseInt(pointsCount);

    if (isNaN(left) || isNaN(right)) { setError('Интервал должен содержать числа'); return; }
    if (left >= right) { setError('Левый край должен быть меньше правого'); return; }
    if (!pointsCount || count < 2) { setError('Количество точек должно быть не менее 2'); return; }

    const func = availableFunctions.find(f => f.key === selectedFunctionKey);
    if ((func?.requiresParams || func?.requiresValue) && !functionParams) {
      setError('Заполните параметры функции');
      return;
    }

    setIsCreating(true);

    try {
      const requestData = {
        name: functionName,
        sourceFunctionKey: functionParams?.functionKey || selectedFunctionKey,
        leftX: left,
        rightX: right,
        pointsCount: count,
        isPublic,
        factoryType: 'ARRAY'
      };

      if (functionParams?.params) Object.assign(requestData, functionParams.params);

      const response = await functionService.createFromMathFunction(requestData);

      if (!response.data?.functionId) {
        throw new Error('Функция не была создана на сервере');
      }

      const functionId = response.data?.functionId;

      if (!functionId) throw new Error('Не удалось получить ID функции');

      const pointsResponse = await functionService.getFunctionPoints(functionId);
      const points = Array.isArray(pointsResponse.data)
        ? pointsResponse.data
        : [];

      if (!points.length) {
        // fallback, только если реально пусто
        for (let i = 0; i < count; i++) {
          const x = left + ((right - left) / (count - 1)) * i;
          points.push({ x, y: 0 });
        }
      }


      onFunctionCreated({ ...response.data, points });
      resetForm();
      onClose();
    } catch (err) {
      console.error('Error creating function:', err);
      setError(err.response?.data?.message || 'Ошибка создания функции');
    } finally {
      setIsCreating(false);
    }
  };

  if (!isOpen) return null;

  const canShowGraph = selectedFunctionKey && leftX && rightX && pointsCount;

  return (
    <div className="modal-overlay">
      <div className="modal-content" style={{ maxWidth: '600px' }}>
        <div className="modal-header">
          <h3 className="modal-title">Создать функцию из другой функции</h3>
          <p className="modal-description">Выберите функцию и укажите интервал для табулирования</p>
        </div>

        <div className="form-section">
          {error && <div className="server-message error" style={{ marginBottom: '20px' }}>{error}</div>}

          <div className="form-row" style={{ display: 'flex', gap: '15px', marginBottom: '20px', flexWrap: 'wrap' }}>
            <div className="form-group" style={{ flex: 1 }}>
              <label className="form-label">Название функции</label>
              <input
                type="text"
                className="form-input"
                value={functionName}
                onChange={(e) => setFunctionName(e.target.value)}
                placeholder="Введите название функции"
                disabled={isCreating || isLoading}
              />
            </div>

            <div className="form-group">
              <label className="form-label">Публичность</label>
              <div className="switch-container">
                <label className="switch">
                  <input type="checkbox" checked={isPublic} onChange={(e) => setIsPublic(e.target.checked)} disabled={isCreating || isLoading} />
                  <span className="slider"></span>
                </label>
                <span className="switch-label">Сделать публичной</span>
              </div>
            </div>
          </div>

          <div className="form-group" style={{ marginBottom: '20px' }}>
            <label className="form-label">
              Математическая функция
              {isLoadingFunctions && <span style={{ color: 'var(--text-secondary)', marginLeft: '10px' }}>(загрузка...)</span>}
            </label>
            <select className="form-input" value={selectedFunctionKey} onChange={(e) => handleFunctionSelect(e.target.value)} disabled={isCreating || isLoadingFunctions}>
              <option value="">Выберите функцию...</option>
              <optgroup label="Базовые функции">
                {availableFunctions.filter(f => f.type === 'BASIC').map(func => (
                  <option key={func.key} value={func.key}>{func.name}{func.requiresParams && ' ⚙️'}{func.requiresValue && ' 🔢'}</option>
                ))}
              </optgroup>
              <optgroup label="Мои функции">
                {availableFunctions.filter(f => f.type === 'USER' && !f.isComposite).map(func => (
                  <option key={func.key} value={func.key}>{func.name} (ID: {func.functionId})</option>
                ))}
              </optgroup>
              <optgroup label="Сложные функции">
                {availableFunctions.filter(f => f.isComposite).map(func => (
                  <option key={func.key} value={func.key}>{func.name} (Composite)</option>
                ))}
              </optgroup>
            </select>
            <small style={{ color: 'var(--text-secondary)', display: 'block', marginTop: '5px' }}>⚙️ - требует параметры, 🔢 - требует значение</small>
          </div>

          <div className="form-row" style={{ display: 'flex', gap: '15px', marginBottom: '20px' }}>
            <div className="form-group" style={{ flex: 1 }}>
              <label className="form-label">Начало интервала</label>
              <input type="number" className="form-input" value={leftX} onChange={(e) => setLeftX(e.target.value)} placeholder="-10" disabled={isCreating} step="any" />
            </div>
            <div className="form-group" style={{ flex: 1 }}>
              <label className="form-label">Конец интервала</label>
              <input type="number" className="form-input" value={rightX} onChange={(e) => setRightX(e.target.value)} placeholder="10" disabled={isCreating} step="any" />
            </div>
            <div className="form-group" style={{ flex: 1 }}>
              <label className="form-label">Количество точек</label>
              <input type="number" className="form-input" value={pointsCount} onChange={(e) => setPointsCount(e.target.value)} placeholder="100" disabled={isCreating} min="2" />
            </div>
          </div>

          {canShowGraph && (
            <div className="form-group" style={{ textAlign: 'center', marginBottom: '20px' }}>
              <button className="btn-secondary" onClick={generatePreviewGraph} disabled={isGeneratingGraph || isCreating}>
                {isGeneratingGraph ? 'Построение графика...' : '📊 Предварительный просмотр графика'}
              </button>
            </div>
          )}
        </div>

        <div className="modal-buttons" style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
          <button className="btn-secondary" onClick={handleCancel} disabled={isCreating}>Отмена</button>
          <button className="btn-primary" onClick={handleCreate} disabled={isCreating || !selectedFunctionKey}>{isCreating ? 'Создание...' : 'Создать функцию'}</button>
        </div>

        <FunctionParamsModal
          isOpen={isParamsModalOpen}
          functionName={availableFunctions.find(f => f.key === selectedFunctionKey)?.name || ''}
          functionKey={selectedFunctionKey}
          onClose={() => setIsParamsModalOpen(false)}
          onConfirm={handleParamsConfirm}
          isLoading={isCreating}
        />

        <GraphModal
          isOpen={isGraphModalOpen}
          onClose={() => { setIsGraphModalOpen(false); setGraphData([]); }}
          points={graphData}
          title={`${selectedFunctionKey} в интервале [${leftX}, ${rightX}]`}
        />
      </div>
    </div>
  );
};

export default CreateFromFunctionModal;
