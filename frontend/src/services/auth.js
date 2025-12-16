import axios from 'axios';

const API = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Перехватчик для добавления Basic Auth
API.interceptors.request.use(
  (config) => {
    const userStr = localStorage.getItem('user');
    if (userStr) {
      const user = JSON.parse(userStr);
      // Для Spring Basic Auth
      if (user.username && user.password) {
        config.auth = {
          username: user.username,
          password: user.password
        };
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Перехватчик ошибок
API.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error.response?.status, error.message);

    if (error.response?.status === 401) {
      localStorage.removeItem('user');
    }

    return Promise.reject(error);
  }
);

const authService = {
  // Регистрация
  async register(username, password) {
    try {
      const response = await API.post('/users/register', {
        username,
        password
      });

      // Сохраняем пользователя с паролем для Basic Auth
      const userData = {
        username,
        password,
        ...response.data
      };

      localStorage.setItem('user', JSON.stringify(userData));

      return {
        success: true,
        data: response.data
      };
    } catch (error) {
      let message = 'Ошибка регистрации';

      if (error.response) {
        switch (error.response.status) {
          case 400:
            message = 'Некорректные данные';
            break;
          case 409:
            message = 'Пользователь уже существует';
            break;
          default:
            message = error.response.data?.message || 'Ошибка сервера';
        }
      } else if (error.request) {
        message = 'Сервер не отвечает';
      }

      return {
        success: false,
        error: message
      };
    }
  },

  // Вход
  async login(username, password) {
    try {
      // Basic Auth для Spring Security
      const response = await API.get('/users/me', {
        auth: { username, password }
      });

      // Сохраняем данные с паролем для последующих запросов
      const userData = {
        username,
        password,
        ...response.data
      };

      localStorage.setItem('user', JSON.stringify(userData));

      return {
        success: true,
        data: response.data
      };
    } catch (error) {
      let message = 'Ошибка входа';

      if (error.response) {
        switch (error.response.status) {
          case 401:
            message = 'Неверное имя пользователя или пароль';
            break;
          case 403:
            message = 'Доступ запрещен';
            break;
          default:
            message = error.response.data?.message || 'Ошибка сервера';
        }
      } else if (error.request) {
        message = 'Сервер не отвечает';
      }

      return {
        success: false,
        error: message
      };
    }
  },

  // Выход
  logout() {
    localStorage.removeItem('user');
    return { success: true };
  },

  // Проверка авторизации
  getCurrentUser() {
    const userStr = localStorage.getItem('user');
    if (!userStr) return null;

    const user = JSON.parse(userStr);
    // Возвращаем без пароля для безопасности
    const { password, ...userWithoutPassword } = user;
    return userWithoutPassword;
  },

  // Проверка, авторизован ли пользователь
  isAuthenticated() {
    return !!this.getCurrentUser();
  },

  // Получение Basic Auth заголовков
  getAuthHeader() {
    const userStr = localStorage.getItem('user');
    if (!userStr) return null;

    const user = JSON.parse(userStr);
    return {
      username: user.username,
      password: user.password
    };
  },

  getCurrentUser() {
    const userStr = localStorage.getItem('user');
    if (!userStr) return null;

    try {
      const user = JSON.parse(userStr);
      const { password, ...userWithoutPassword } = user;
      return userWithoutPassword;
    } catch (error) {
      console.error('Error parsing user data:', error);
      return null;
    }
  }
};

export default authService;