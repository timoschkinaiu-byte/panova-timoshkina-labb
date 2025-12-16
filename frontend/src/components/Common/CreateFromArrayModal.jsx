import { useState } from 'react';
import { validatePointsCount, validateArrayPoints } from '../../utils/validation.js';
import functionService from '../../services/functionService.js';
import '../../App.css';

const CreateFromArrayModal = ({
  isOpen,
  onClose,
  onFunctionCreated,
  isLoading = false
}) => {
  const [functionName, setFunctionName] = useState('');
  const [pointsCount, setPointsCount] = useState('');
  const [points, setPoints] = useState([]);
  const [validationError, setValidationError] = useState('');
  const [tableErrors, setTableErrors] = useState([]);
  const [isPublic, setIsPublic] = useState(false);
  const [isCreatingLocal, setIsCreatingLocal] = useState(false);

  if (!isOpen) return null;

  const handleGenerateTable = () => {
    const error = validatePointsCount(pointsCount);
    if (error) {
      setValidationError(error);
      return;
    }

    setValidationError('');
    const count = parseInt(pointsCount);
    const newPoints = Array(count).fill().map((_, i) => ({
      id: i,
      x: '',
      y: ''
    }));
    setPoints(newPoints);
    setTableErrors([]);
  };

  const handlePointChange = (id, field, value) => {
    setPoints(prev => prev.map(p =>
      p.id === id ? { ...p, [field]: value } : p
    ));

    if (tableErrors.length > 0) {
      setTableErrors([]);
    }
  };

  const handleCreate = async () => {
    // Проверка заполнения всех точек
    const emptyFields = points.filter(p => p.x === '' || p.y === '');
    if (emptyFields.length > 0) {
      setTableErrors(['Заполните все значения X и Y']);
      return;
    }

    // Конвертация и валидация
    const numericPoints = points.map(p => ({
      x: parseFloat(p.x),
      y: parseFloat(p.y)
    }));

    const validationErrors = validateArrayPoints(numericPoints);
    if (validationErrors.length > 0) {
      setTableErrors(validationErrors);
      return;
    }

    if (!functionName.trim()) {
      setTableErrors(['Введите название функции']);
      return;
    }

    setIsCreatingLocal(true);

    try {
      const xValues = numericPoints.map(p => p.x);
      const yValues = numericPoints.map(p => p.y);

      const response = await functionService.createFromArrays(
        functionName,
        xValues,
        yValues,
        isPublic
      );

      if (response.status === 201) {
        // Передаем реальный массив точек вместе с response
        onFunctionCreated(response, numericPoints);

        // Очистка формы
        resetForm();
        onClose();
      } else {
        setTableErrors(['Ошибка создания функции']);
      }
    } catch (error) {
      console.error('Error creating function:', error);
      const message = error.response?.data?.message || 'Ошибка создания функции';
      setTableErrors([message]);
    } finally {
      setIsCreatingLocal(false);
    }
  };

  const handleCancel = () => {
    resetForm();
    onClose();
  };

  const resetForm = () => {
    setFunctionName('');
    setPointsCount('');
    setPoints([]);
    setValidationError('');
    setTableErrors([]);
    setIsPublic(false);
  };

  const canCreate = functionName.trim() &&
                   points.length > 0 &&
                   points.every(p => p.x !== '' && p.y !== '');

  return (
    <div className="modal-overlay">
      <div className="modal-content">
        <div className="modal-header">
          <h3 className="modal-title">Создать функцию из массивов</h3>
          <p className="modal-description">Введите значения X и Y для табулированной функции</p>
        </div>

        <div className="form-section">
          {/* Название функции и публичность */}
          <div className="form-row" style={{ display: 'flex', gap: '20px', marginBottom: '20px' }}>
            <div className="form-group" style={{ flex: 1 }}>
              <label className="form-label">Название функции</label>
              <input
                type="text"
                className="form-input"
                value={functionName}
                onChange={(e) => setFunctionName(e.target.value)}
                placeholder="Введите название функции"
                disabled={isCreatingLocal}
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
                    disabled={isCreatingLocal}
                  />
                  <span className="slider"></span>
                </label>
                <span className="switch-label">Сделать публичной</span>
              </div>
            </div>
          </div>

          {/* Количество точек */}
          <div className="form-row" style={{ display: 'flex', gap: '15px', alignItems: 'center', marginBottom: '20px' }}>
            <div className="form-group">
              <label className="form-label">Количество точек (2-1000)</label>
              <input
                type="number"
                className={`form-input ${validationError ? 'error' : ''}`}
                value={pointsCount}
                onChange={(e) => {
                  setPointsCount(e.target.value);
                  if (validationError) setValidationError('');
                }}
                placeholder="Введите число"
                min="2"
                max="1000"
                disabled={isCreatingLocal}
                style={{ width: '150px' }}
              />
              {validationError && (
                <span className="error-message">{validationError}</span>
              )}
            </div>

            <button
              className="btn-secondary"
              onClick={handleGenerateTable}
              disabled={isCreatingLocal}
              style={{
                height: '42px',
                marginTop: '24px',
                padding: '10px 20px'
              }}
            >
              Ввести точки
            </button>
          </div>

          {/* Сообщения об ошибках */}
          {tableErrors.length > 0 && (
            <div className="server-message error" style={{ marginBottom: '20px' }}>
              {tableErrors.map((error, i) => (
                <div key={i}>{error}</div>
              ))}
            </div>
          )}

          {/* Таблица точек */}
          {points.length > 0 && (
            <div className="table-container">
              <table className="points-table">
                <thead>
                  <tr>
                    <th>X</th>
                    <th>Y</th>
                  </tr>
                </thead>
                <tbody>
                  {points.map(point => (
                    <tr key={point.id}>
                      <td>
                        <input
                          type="number"
                          value={point.x}
                          onChange={(e) => handlePointChange(point.id, 'x', e.target.value)}
                          disabled={isCreatingLocal}
                          step="any"
                          placeholder="Значение X"
                        />
                      </td>
                      <td>
                        <input
                          type="number"
                          value={point.y}
                          onChange={(e) => handlePointChange(point.id, 'y', e.target.value)}
                          disabled={isCreatingLocal}
                          step="any"
                          placeholder="Значение Y"
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              <div style={{
                marginTop: '15px',
                fontSize: '12px',
                color: 'var(--text-secondary)'
              }}>
                Всего точек: {points.length}
              </div>
            </div>
          )}
        </div>

        {/* Кнопки действий */}
        <div className="modal-buttons" style={{ marginTop: '30px' }}>
          <button
            className="btn-secondary"
            onClick={handleCancel}
            disabled={isCreatingLocal}
            style={{ padding: '12px 24px' }}
          >
            Отмена
          </button>
          <button
            className="btn-primary"
            onClick={handleCreate}
            disabled={isCreatingLocal || !canCreate}
            style={{ padding: '12px 24px' }}
          >
            {isCreatingLocal ? 'Создание...' : 'Создать функцию'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default CreateFromArrayModal;
