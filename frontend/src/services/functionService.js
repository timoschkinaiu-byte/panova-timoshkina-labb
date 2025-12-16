import API from './api';

const functionService = {
  // Создать функцию из массивов
  createFromArrays(name, xValues, yValues, isPublic = false, factoryType = 'ARRAY') {
    return API.post(`/functions/from-arrays?factoryType=${factoryType}`, {
      name,
      xValues,
      yValues,
      isPublic
    });
  },

  // Создать функцию из MathFunction (универсальный метод)
  createFromMathFunction(data) {
    // data должен содержать:
    // - name: название новой функции
    // - sourceFunctionName: имя исходной функции
    // - leftX, rightX, pointsCount: параметры табуляции
    // - isPublic: публичность
    // - factoryType: тип фабрики
    // - дополнительные параметры в зависимости от типа функции:
    //   * Для пользовательской функции: functionId (ID исходной функции)
    //   * Для постоянной функции: constantValue
    //   * Для B-сплайна: nodePoints[], splineOrder, weights[]

    const { factoryType = 'ARRAY', ...requestData } = data;
    return API.post(`/functions/from-math-function?factoryType=${factoryType}`, requestData);
  },

  // Старая сигнатура для совместимости
  createFromMathFunctionOld(name, sourceFunctionName, leftX, rightX, pointsCount,
                           isPublic = false, factoryType = 'ARRAY', additionalParams = {}) {
    const data = {
      name,
      sourceFunctionName,
      leftX,
      rightX,
      pointsCount,
      isPublic,
      ...additionalParams
    };
    return this.createFromMathFunction(data);
  },

  // Создать сложную функцию
  createComposite(name, outerFunctionName, innerFunctionName, isPublic = false, factoryType = 'ARRAY') {
    return API.post(`/functions/composite?factoryType=${factoryType}`, {
      name,
      outerFunctionName,
      innerFunctionName,
      isPublic
    });
  },

  // Импорт функции из файла
  importFunction(file, format = 'binary') {
    const formData = new FormData();
    formData.append('file', file);

    return API.post(`/functions/import?format=${format}`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
  },

  // Получить функции с фильтрацией
  getFunctions(search = '', type = '', ownerId = null, isPublic = null) {
    const params = new URLSearchParams();
    if (search) params.append('search', search);
    if (type) params.append('type', type);
    if (ownerId) params.append('ownerId', ownerId);
    if (isPublic !== null) params.append('isPublic', isPublic);

    return API.get(`/functions?${params.toString()}`);
  },

  // Псевдоним для обратной совместимости
  getMyFunctions(search = '', type = '', ownerId = null, isPublic = null) {
    return this.getFunctions(search, type, ownerId, isPublic);
  },

  // Получить функцию по ID
  getFunctionById(id) {
    return API.get(`/functions/${id}`);
  },

  // Получить точки функции
  getFunctionPoints(functionId, xFrom = null, xTo = null) {
    if (xFrom !== null && xTo !== null) {
      const params = new URLSearchParams({ functionId });
      params.append('xFrom', xFrom);
      params.append('xTo', xTo);
      return API.get(`/points?${params.toString()}`);
    } else {
      return API.get(`/functions/${functionId}/points`);
    }
  },

  // Добавить точку
  addPoint(functionId, xValue, yValue) {
    return API.post('/points', {
      functionId,
      xValue,
      yValue
    });
  },

  // Удалить точку
  deletePoint(pointId) {
    return API.delete(`/points/${pointId}`);
  },

  // Обновить функцию
  updateFunction(functionId, updateData) {
    return API.put(`/functions/${functionId}`, updateData);
  },

  // Вычислить значение функции в точке
  computeValue(functionId, x) {
    return API.post(`/functions/${functionId}/compute`, { x });
  },

  // Получить данные графика функции
  getGraphData(functionId, pointsCount = null, xFrom = null, xTo = null) {
    const params = new URLSearchParams();

    if (pointsCount !== null && pointsCount !== undefined) {
      params.append('pointsCount', pointsCount);
    }

    if (xFrom !== null && xFrom !== undefined) {
      params.append('xFrom', xFrom);
    }

    if (xTo !== null && xTo !== undefined) {
      params.append('xTo', xTo);
    }

    const queryString = params.toString();
    return API.get(`/functions/${functionId}/graph-data${queryString ? `?${queryString}` : ''}`);
  },

  // Удалить функцию
  deleteFunction(functionId) {
    return API.delete(`/functions/${functionId}`);
  },

  // Экспорт функции
  exportFunction(functionId, format = 'binary') {
    return API.post(
      `/functions/${functionId}/export?format=${format}`,
      {},
      {
        responseType: 'blob',
        headers: {
          'Content-Type': 'application/json'
        }
      }
    );
  },

  // Получить всех пользователей (для админов)
  getAllUsers() {
    return API.get('/users');
  },

  // Получить доступные Math функции (базовые)
  getAvailableMathFunctions() {
    return [
      { name: 'Квадратичная функция', type: 'SQR' },
      { name: 'Тождественная функция', type: 'IDENTITY' },
      { name: 'Постоянная функция', type: 'CONSTANT', requiresValue: true },
      { name: 'Единичная функция', type: 'UNIT' },
      { name: 'Нулевая функция', type: 'ZERO' },
      { name: 'B-сплайн функция', type: 'BSPLINE', requiresParams: true }
    ];
  }
};

export default functionService;