import API from './api';

const functionService = {
  // Получить ВСЕ доступные функции (базовые + пользовательские + composite)
  async getAvailableFunctions() {
    try {
      // 1. Получаем базовые функции (локально)
      const basicFunctions = this.getAvailableMathFunctions();

      // 2. Получаем пользовательские функции из API
      const userFunctionsResponse = await this.getMyFunctions();
      const userFunctions = userFunctionsResponse.data || [];

      // 3. Получаем сложные функции (помеченные как COMPOSITE)
      const compositeFunctions = userFunctions.filter(f =>
        f.functionType === 'COMPOSITE' ||
        f.functionName?.includes('(composite)')
      );

      // 4. Формируем единый список
      const allFunctions = [...basicFunctions];

      // Добавляем простые пользовательские функции
      userFunctions.forEach(func => {
        if (!func.functionName?.includes('(composite)') &&
            func.functionType !== 'COMPOSITE' &&
            !allFunctions.some(f => f.key === `USER_${func.functionId}`)) {
          allFunctions.push({
            key: `USER_${func.functionId}`,
            name: func.functionName,
            type: 'USER',
            functionId: func.functionId,
            ownerId: func.ownerId,
            isComposite: false,
            requiresParams: false
          });
        }
      });

      // Добавляем сложные функции
      compositeFunctions.forEach(func => {
        if (!allFunctions.some(f => f.key === `USER_${func.functionId}`)) {
          allFunctions.push({
            key: `USER_${func.functionId}`,
            name: func.functionName,
            type: 'COMPOSITE',
            functionId: func.functionId,
            ownerId: func.ownerId,
            isComposite: true,
            requiresParams: false
          });
        }
      });

      return allFunctions;
    } catch (error) {
      console.error('Error loading all functions:', error);
      return this.getAvailableMathFunctions(); // Возвращаем хотя бы базовые
    }
  },

  // Базовые математические функции
  getAvailableMathFunctions() {
    return [
      {
        key: 'SQR',
        name: 'Квадратичная функция',
        type: 'BASIC',
        requiresParams: false,
        requiresValue: false
      },
      {
        key: 'IDENTITY',
        name: 'Тождественная функция',
        type: 'BASIC',
        requiresParams: false,
        requiresValue: false
      },
      {
        key: 'CONSTANT',
        name: 'Постоянная функция',
        type: 'BASIC',
        requiresParams: false,
        requiresValue: true, // Требует значение константы
        paramName: 'constantValue',
        paramLabel: 'Значение константы'
      },
      {
        key: 'UNIT',
        name: 'Единичная функция',
        type: 'BASIC',
        requiresParams: false,
        requiresValue: false
      },
      {
        key: 'ZERO',
        name: 'Нулевая функция',
        type: 'BASIC',
        requiresParams: false,
        requiresValue: false
      },
      {
        key: 'BSPLINE',
        name: 'B-сплайн функция',
        type: 'BASIC',
        requiresParams: true, // Требует параметры
        params: [
          { name: 'nodePoints', label: 'Точки узлов (через запятую)', type: 'text' },
          { name: 'splineOrder', label: 'Порядок сплайна', type: 'number', min: 1 },
          { name: 'weights', label: 'Весовые коэффициенты (через запятую)', type: 'text' }
        ]
      }
    ];
  },

  // Создать функцию из MathFunction (универсальный метод)
  async createFromMathFunction(data) {
    // Подготавливаем данные в зависимости от типа функции
    const requestData = {
      name: data.name,
      sourceFunctionName: data.sourceFunctionKey,
      leftX: parseFloat(data.leftX),
      rightX: parseFloat(data.rightX),
      pointsCount: parseInt(data.pointsCount),
      isPublic: data.isPublic || false,
      factoryType: data.factoryType || 'ARRAY'
    };

    // Добавляем параметры для специальных функций
    if (data.sourceFunctionKey === 'CONSTANT' && data.constantValue) {
      requestData.sourceFunctionName = `CONSTANT_${parseFloat(data.constantValue)}`;
    } else if (data.sourceFunctionKey === 'BSPLINE' && data.nodePoints && data.splineOrder && data.weights) {
      requestData.sourceFunctionName = `BSPLINE`;
      requestData.nodePoints = data.nodePoints.split(',').map(p => parseFloat(p.trim()));
      requestData.splineOrder = parseInt(data.splineOrder);
      requestData.weights = data.weights.split(',').map(w => parseFloat(w.trim()));
    }

    // Для пользовательских функций используем ключ USER_ID
    if (data.sourceFunctionKey.startsWith('USER_')) {
      // Ключ уже в правильном формате
      requestData.sourceFunctionName = data.sourceFunctionKey;
    }

    console.log('Sending to createFromMathFunction:', requestData);
    return API.post(`/functions/from-math-function?factoryType=${requestData.factoryType}`, requestData);
  },

  // Создать сложную функцию (composite)
  async createComposite(name, outerFunctionKey, innerFunctionKey, isPublic = false, factoryType = 'ARRAY') {
    const requestData = {
      name: name,
      outerFunctionName: outerFunctionKey,
      innerFunctionName: innerFunctionKey,
      isPublic: isPublic
    };

    console.log('Creating composite function:', requestData);
    return API.post(`/functions/composite?factoryType=${factoryType}`, requestData);
  },

  // Создать функцию из массивов
  async createFromArrays(name, xValues, yValues, isPublic = false, factoryType = 'ARRAY') {
    return API.post(`/functions/from-arrays?factoryType=${factoryType}`, {
      name,
      xValues,
      yValues,
      isPublic
    });
  },

  // Получить мои функции
  async getMyFunctions(search = '', type = '', ownerId = null, isPublic = null) {
    const params = new URLSearchParams();
    if (search) params.append('search', search);
    if (type) params.append('type', type);
    if (ownerId) params.append('ownerId', ownerId);
    if (isPublic !== null) params.append('isPublic', isPublic);

    return API.get(`/functions?${params.toString()}`);
  },

  // Импорт/экспорт и другие методы остаются как есть
  async importFunction(file, format = 'binary') {
    const formData = new FormData();
    formData.append('file', file);

    return API.post(`/functions/import?format=${format}`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
  },


  async getFunctionById(id) {
    return API.get(`/functions/${id}`);
  },

  async getFunctionPoints(functionId, xFrom = null, xTo = null) {
    if (xFrom !== null && xTo !== null) {
      const params = new URLSearchParams({ functionId });
      params.append('xFrom', xFrom);
      params.append('xTo', xTo);
      return API.get(`/points?${params.toString()}`);
    } else {
      return API.get(`/functions/${functionId}/points`);
    }
  },

  async updateFunction(functionId, updateData) {
    return API.put(`/functions/${functionId}`, updateData);
  },

  async deleteFunction(functionId) {
    return API.delete(`/functions/${functionId}`);
  },

  async getGraphData(functionId, pointsCount = null, xFrom = null, xTo = null) {
    const params = new URLSearchParams();
    if (pointsCount !== null) params.append('pointsCount', pointsCount);
    if (xFrom !== null) params.append('xFrom', xFrom);
    if (xTo !== null) params.append('xTo', xTo);

    return API.get(`/functions/${functionId}/graph-data?${params.toString()}`);
  },

  // Добавить точку функции
    async addPoint(functionId, xValue, yValue) {
      return API.post('/points', {
        functionId,
        xValue,
        yValue
      });
    },

    // Удалить точку
    async deletePoint(pointId) {
      return API.delete(`/points/${pointId}`);
    },

    // Экспорт функции в файл (JSON формат)
    async exportFunction(functionId) {
      return API.get(`/functions/${functionId}/export?format=json`, {
        responseType: 'blob'
      });
    },

    // Экспорт функции в XML (если поддерживается)
    async exportFunctionXML(functionId) {
      return API.get(`/functions/${functionId}/export?format=xml`, {
        responseType: 'blob'
      });
    },

    // Получить данные для экспорта (чтобы создать файл с правильным именем)
    async getExportData(functionId) {
      try {
        // Сначала получаем информацию о функции
        const funcResponse = await this.getFunctionById(functionId);
        const functionData = funcResponse.data;

        // Затем получаем файл
        const fileResponse = await this.exportFunction(functionId);

        return {
          functionData,
          blob: fileResponse.data,
          fileName: `${functionData.functionName.replace(/[^a-zA-Z0-9а-яА-ЯёЁ\s\-_]/g, '_')}.json`
        };
      } catch (error) {
        console.error('Error getting export data:', error);
        throw error;
      }
    },

    async differentiateFunction(functionId) {
      return API.post('/operations/differentiate', {
        functionId
      });
    },

    async integrateFunction(functionId, threadsCount = 1) {
      return API.post('/operations/integrate', {
        functionId,
        threadsCount: Math.min(threadsCount, 8)
      });
    },

    // Обновление точки через удаление и создание новой
    async updatePointByReplacement(functionId, xValue, newYValue) {
      try {
        // 1. Находим точку по functionId и xValue
        const searchResponse = await API.get(`/points/search?functionId=${functionId}&x=${xValue}`);

        if (!searchResponse.data || searchResponse.data.length === 0) {
          throw new Error('Точка не найдена');
        }

        const oldPoint = searchResponse.data[0];
        const pointId = oldPoint.pointId;

        // 2. Удаляем старую точку
        await API.delete(`/points/${pointId}`);

        // 3. Создаем новую точку с тем же X и новым Y
        const newPointResponse = await API.post('/points', {
          functionId: functionId,
          xValue: xValue,
          yValue: newYValue
        });

        return newPointResponse.data;

      } catch (error) {
        console.error('Error updating point by replacement:', error);
        throw error;
      }
    }

};

export default functionService;