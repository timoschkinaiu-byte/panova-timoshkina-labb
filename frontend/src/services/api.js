import axios from 'axios';

const API = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

// Перехватчик для добавления токена
API.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    const user = JSON.parse(localStorage.getItem('user') || '{}');

    // Если есть пользователь, используем Basic Auth
    if (user.username && user.password) {
      config.auth = {
        username: user.username,
        password: user.password
      };
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// Перехватчик ошибок
API.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error.response?.data || error.message);
    return Promise.reject(error);
  }
);

export default API;