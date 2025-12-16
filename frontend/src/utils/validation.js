// Валидация параметров функций
export const validateFunctionParams = (functionName, params) => {
  const errors = {};

  switch (functionName) {
    case 'Постоянная функция':
      if (!params.constantValue) {
        errors.constantValue = 'Введите значение константы';
      } else if (isNaN(parseFloat(params.constantValue))) {
        errors.constantValue = 'Значение должно быть числом';
      }
      break;

    case 'B-сплайн функция':
      if (!params.nodePoints) {
        errors.nodePoints = 'Введите точки узлов';
      } else {
        const points = params.nodePoints.split(',').map(p => p.trim());
        if (points.length < 2) {
          errors.nodePoints = 'Нужно минимум 2 точки узла';
        }
        for (const point of points) {
          if (isNaN(parseFloat(point))) {
            errors.nodePoints = 'Все точки должны быть числами';
            break;
          }
        }
      }

      if (!params.splineOrder) {
        errors.splineOrder = 'Введите порядок сплайна';
      } else if (isNaN(parseInt(params.splineOrder)) || parseInt(params.splineOrder) < 1) {
        errors.splineOrder = 'Порядок должен быть положительным числом';
      }

      if (!params.weights) {
        errors.weights = 'Введите весовые коэффициенты';
      } else {
        const weights = params.weights.split(',').map(w => w.trim());
        if (weights.length < 2) {
          errors.weights = 'Нужно минимум 2 весовых коэффициента';
        }
        for (const weight of weights) {
          if (isNaN(parseFloat(weight))) {
            errors.weights = 'Все коэффициенты должны быть числами';
            break;
          }
        }
      }
      break;

    default:
      break;
  }

  return errors;
};

// Валидация количества точек
export const validatePointsCount = (pointsCount) => {
  if (!pointsCount) {
    return 'Введите количество точек';
  }

  const count = parseInt(pointsCount);
  if (isNaN(count)) {
    return 'Количество точек должно быть числом';
  }

  if (count < 2) {
    return 'Минимум 2 точки';
  }

  if (count > 1000) {
    return 'Максимум 1000 точек';
  }

  return null;
};

// Валидация точек массива
export const validateArrayPoints = (points) => {
  const errors = [];

  if (!points || points.length === 0) {
    return ['Нет точек для проверки'];
  }

  // Проверяем, что все X уникальны
  const xValues = points.map(p => p.x);
  const uniqueX = new Set(xValues);
  if (uniqueX.size !== xValues.length) {
    errors.push('Значения X должны быть уникальными');
  }

  // Проверяем, что X отсортированы
  for (let i = 1; i < points.length; i++) {
    if (points[i].x <= points[i-1].x) {
      errors.push('Значения X должны быть строго возрастающими');
      break;
    }
  }

  return errors;
};