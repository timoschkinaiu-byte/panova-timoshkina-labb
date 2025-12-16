import { useState } from 'react';
import { validateFunctionParams } from '../../utils/validation';
import '../../App.css';

const FunctionParamsModal = ({
  isOpen,
  functionName,
  functionKey,
  onClose,
  onConfirm,
  isLoading
}) => {
  const [params, setParams] = useState({
    constantValue: '',
    nodePoints: '',
    splineOrder: '',
    weights: ''
  });
  const [errors, setErrors] = useState({});

  if (!isOpen) return null;

  // Получаем информацию о функции
  const getFunctionInfo = () => {
    // Базовые функции с параметрами
    const functionMap = {
      'Постоянная функция': {
        title: 'Параметр постоянной функции',
        description: 'Введите значение константы',
        requiresValue: true
      },
      'B-сплайн функция': {
        title: 'Параметры B-сплайн функции',
        description: 'Введите точки узлов, порядок сплайна и весовые коэффициенты',
        requiresParams: true
      }
    };

    return functionMap[functionName] || {
      title: 'Параметры функции',
      description: 'Заполните необходимые параметры'
    };
  };

  const functionInfo = getFunctionInfo();

  const handleSubmit = (e) => {
    e.preventDefault();

    const validationErrors = validateFunctionParams(functionName, params);
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    // Формируем ключ функции с параметрами
    let functionKeyWithParams = functionKey;

    if (functionName === 'Постоянная функция' && params.constantValue) {
      functionKeyWithParams = `CONSTANT_${params.constantValue}`;
    } else if (functionName === 'B-сплайн функция' && params.nodePoints && params.splineOrder && params.weights) {
      functionKeyWithParams = 'BSPLINE';
    }

    onConfirm({
      functionKey: functionKeyWithParams,
      params: params
    });

    setParams({ constantValue: '', nodePoints: '', splineOrder: '', weights: '' });
    setErrors({});
  };

  const renderFields = () => {
    switch (functionName) {
      case 'Постоянная функция':
        return (
          <div className="form-group">
            <label className="form-label">Значение константы</label>
            <input
              type="number"
              className={`form-input ${errors.constantValue ? 'error' : ''}`}
              value={params.constantValue}
              onChange={(e) => {
                setParams(prev => ({ ...prev, constantValue: e.target.value }));
                if (errors.constantValue) setErrors(prev => ({ ...prev, constantValue: '' }));
              }}
              placeholder="Например: 3.14"
              disabled={isLoading}
              step="any"
            />
            {errors.constantValue && (
              <span className="error-message">{errors.constantValue}</span>
            )}
          </div>
        );

      case 'B-сплайн функция':
        return (
          <>
            <div className="form-group">
              <label className="form-label">Точки узлов (через запятую)</label>
              <input
                type="text"
                className={`form-input ${errors.nodePoints ? 'error' : ''}`}
                value={params.nodePoints}
                onChange={(e) => {
                  setParams(prev => ({ ...prev, nodePoints: e.target.value }));
                  if (errors.nodePoints) setErrors(prev => ({ ...prev, nodePoints: '' }));
                }}
                placeholder="Например: 0, 1, 2, 3, 4"
                disabled={isLoading}
              />
              {errors.nodePoints && (
                <span className="error-message">{errors.nodePoints}</span>
              )}
            </div>

            <div className="form-group">
              <label className="form-label">Порядок сплайна</label>
              <input
                type="number"
                className={`form-input ${errors.splineOrder ? 'error' : ''}`}
                value={params.splineOrder}
                onChange={(e) => {
                  setParams(prev => ({ ...prev, splineOrder: e.target.value }));
                  if (errors.splineOrder) setErrors(prev => ({ ...prev, splineOrder: '' }));
                }}
                placeholder="Например: 3"
                disabled={isLoading}
                min="1"
              />
              {errors.splineOrder && (
                <span className="error-message">{errors.splineOrder}</span>
              )}
            </div>

            <div className="form-group">
              <label className="form-label">Весовые коэффициенты (через запятую)</label>
              <input
                type="text"
                className={`form-input ${errors.weights ? 'error' : ''}`}
                value={params.weights}
                onChange={(e) => {
                  setParams(prev => ({ ...prev, weights: e.target.value }));
                  if (errors.weights) setErrors(prev => ({ ...prev, weights: '' }));
                }}
                placeholder="Например: 1, 0.5, 1, 0.5, 1"
                disabled={isLoading}
              />
              {errors.weights && (
                <span className="error-message">{errors.weights}</span>
              )}
            </div>
          </>
        );

      default:
        return (
          <div className="form-group">
            <p className="info-message">Эта функция не требует дополнительных параметров</p>
          </div>
        );
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal-content">
        <div className="modal-header">
          <h3 className="modal-title">{functionInfo.title}</h3>
          <p className="modal-description">{functionInfo.description}</p>
        </div>

        <form onSubmit={handleSubmit}>
          {renderFields()}

          <div className="modal-buttons">
            <button
              type="button"
              className="btn-secondary"
              onClick={() => {
                onClose();
                setParams({ constantValue: '', nodePoints: '', splineOrder: '', weights: '' });
                setErrors({});
              }}
              disabled={isLoading}
            >
              Отмена
            </button>
            <button
              type="submit"
              className="btn-primary"
              disabled={isLoading}
            >
              {isLoading ? 'Сохранение...' : 'Сохранить'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default FunctionParamsModal;