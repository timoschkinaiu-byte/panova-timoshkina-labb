export const validatePointsCount = (count) => {
  if (!count || count.trim() === '') {
    return 'Количество точек обязательно';
  }

  const num = parseInt(count);
  if (isNaN(num)) {
    return 'Введите число';
  }

  if (num < 2) {
    return 'Минимум 2 точки';
  }

  if (num > 1000) {
    return 'Максимум 1000 точек';
  }

  return '';
};

export const validateArrayPoints = (points) => {
  const errors = [];

  // Проверка уникальности X
  const xValues = points.map(p => p.x);
  const uniqueX = new Set(xValues);
  if (uniqueX.size !== xValues.length) {
    errors.push('Значения X должны быть уникальными');
  }

  // Проверка сортировки X
  for (let i = 1; i < points.length; i++) {
    if (points[i].x <= points[i - 1].x) {
      errors.push('Значения X должны быть строго возрастающими');
      break;
    }
  }

  // Проверка на NaN
  for (let i = 0; i < points.length; i++) {
    if (isNaN(points[i].x) || isNaN(points[i].y)) {
      errors.push('Все значения должны быть числами');
      break;
    }
  }

  return errors;
};

export const validateFunctionParams = (functionName, params) => {
  const errors = {};

  if (functionName === 'Постоянная функция') {
    if (!params.constantValue || params.constantValue.trim() === '') {
      errors.constantValue = 'Значение константы обязательно';
    } else if (isNaN(params.constantValue)) {
      errors.constantValue = 'Должно быть числом';
    }
  }

  if (functionName === 'B-сплайн функция') {
    if (!params.nodePoints || params.nodePoints.trim() === '') {
      errors.nodePoints = 'Точки узлов обязательны';
    }
    if (!params.splineOrder || params.splineOrder.trim() === '') {
      errors.splineOrder = 'Порядок сплайна обязателен';
    }
    if (!params.weights || params.weights.trim() === '') {
      errors.weights = 'Весовые коэффициенты обязательны';
    }
  }

  return errors;
};